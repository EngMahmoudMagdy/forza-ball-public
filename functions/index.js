const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const FEED_TOPIC = "feed_all";

/** Must match the app’s `FeedPushConstants.NOTIFICATION_CHANNEL_ID` (`feed_social_high`). */
const ANDROID_FEED_CHANNEL_ID = "feed_social_high";

/** Firestore getAll supports a limited batch size per request. */
const FIRESTORE_GET_ALL_CHUNK = 10;
/** FCM multicast limit per request. */
const FCM_MULTICAST_CHUNK = 500;

/** FCM data payloads must use string values only. */
function stringifyData(obj) {
  const out = {};
  for (const [k, v] of Object.entries(obj)) {
    out[k] = v == null ? "" : String(v);
  }
  return out;
}

/** @param {string} text */
function twoLinePreview(text) {
  const s = text == null ? "" : String(text);
  const lines = s.split(/\r?\n/).filter((l) => l.trim().length > 0).slice(0, 2);
  return lines.join("\n").substring(0, 280);
}

/** @param {FirebaseFirestore.DocumentReference[]} refs */
async function getAllSnapshots(refs) {
  if (refs.length === 0) return [];
  const snaps = [];
  for (let i = 0; i < refs.length; i += FIRESTORE_GET_ALL_CHUNK) {
    const chunk = refs.slice(i, i + FIRESTORE_GET_ALL_CHUNK);
    const part = await db.getAll(...chunk);
    snaps.push(...part);
  }
  return snaps;
}

/**
 * Tray title/body aligned with the Android client’s [notificationTitle] + preview text.
 * @param {string} type `new_post` | `comment` | `like` | `dislike` | `comment_like`
 */
function feedNotificationFromPayload(type, actorName, preview) {
  const name =
    actorName == null || String(actorName).trim() === "" ? "Someone" : String(actorName).trim();
  const body =
    preview == null || String(preview).trim() === "" ? "Tap to open" : String(preview).trim();
  let title;
  switch (type) {
    case "new_post":
      title = name;
      break;
    case "comment":
      title = `${name} commented`;
      break;
    case "like":
      title = `${name} liked your post`;
      break;
    case "dislike":
      title = `${name} disliked your post`;
      break;
    case "comment_like":
      title = `${name} liked your comment`;
      break;
    default:
      title = "ForzaBall";
  }
  return { title, body };
}

function androidFeedMessageOptions() {
  return {
    priority: "high",
    notification: {
      channelId: ANDROID_FEED_CHANNEL_ID,
    },
  };
}

/**
 * Sends both **data** (for in-app / parsing) and **notification** (system tray when backgrounded).
 * @param {string[]} tokens
 * @param {Record<string, unknown>} data
 */
async function sendMulticastDataAndNotification(tokens, data) {
  const unique = [...new Set((tokens || []).filter((t) => t && typeof t === "string"))];
  if (unique.length === 0) return 0;
  const dataStr = stringifyData(data);
  const { title, body } = feedNotificationFromPayload(
    String(data.type || ""),
    data.actorName,
    data.preview,
  );
  let success = 0;
  for (let i = 0; i < unique.length; i += FCM_MULTICAST_CHUNK) {
    const chunk = unique.slice(i, i + FCM_MULTICAST_CHUNK);
    const resp = await admin.messaging().sendEachForMulticast({
      tokens: chunk,
      notification: { title, body },
      data: dataStr,
      android: androidFeedMessageOptions(),
    });
    success += resp.successCount;
    if (resp.failureCount > 0) {
      resp.responses.forEach((r, idx) => {
        if (!r.success) {
          console.error("FCM multicast failure", chunk[idx], r.error?.message);
        }
      });
    }
  }
  return success;
}

/**
 * Single device: data + notification + Android channel (same shape as multicast).
 * @param {Record<string, unknown>} data
 */
async function sendTokenDataAndNotification(token, data) {
  if (!token || typeof token !== "string") return;
  const dataStr = stringifyData(data);
  const { title, body } = feedNotificationFromPayload(
    String(data.type || ""),
    data.actorName,
    data.preview,
  );
  await admin.messaging().send({
    token,
    notification: { title, body },
    data: dataStr,
    android: androidFeedMessageOptions(),
  });
}

exports.onPostCreated = functions.firestore
  .document("posts/{postId}")
  .onCreate(async (snap, context) => {
    const postId = context.params.postId;
    const post = snap.data() || {};
    const actorId = post.userId;
    const preview = twoLinePreview(post.text);

    const userDoc = await db.collection("users").doc(String(actorId)).get();
    const u = userDoc.data() || {};
    const actorName = String(u.displayName || u.username || "Someone");
    const actorPhotoUrl = u.avatarUrl != null ? String(u.avatarUrl) : "";

    const postData = {
      type: "new_post",
      postId: String(postId),
      actorId: String(actorId || ""),
      actorName,
      actorPhotoUrl,
      preview,
    };
    const dataStr = stringifyData(postData);
    const { title, body } = feedNotificationFromPayload("new_post", actorName, preview);
    await admin.messaging().send({
      topic: FEED_TOPIC,
      notification: { title, body },
      data: dataStr,
      android: androidFeedMessageOptions(),
    });
    return null;
  });

/**
 * Notifies: post author (if they are not the commenter) + every distinct user who has commented
 * on this post (except the new commenter). Matches app writes to `posts/{postId}/comments/{commentId}`.
 */
exports.onCommentCreated = functions.firestore
  .document("posts/{postId}/comments/{commentId}")
  .onCreate(async (snap, context) => {
    const postId = context.params.postId;
    const comment = snap.data() || {};
    const commenterId = String(comment.userId || "").trim();
    if (!commenterId) {
      console.warn("onCommentCreated: missing userId on comment", postId);
      return null;
    }

    const postSnap = await db.collection("posts").doc(postId).get();
    if (!postSnap.exists) return null;
    const postAuthorId = String(postSnap.data().userId || "").trim();

    const commentsSnap = await db.collection("posts").doc(postId).collection("comments").get();
    const recipientIds = new Set();
    if (postAuthorId) recipientIds.add(postAuthorId);
    commentsSnap.forEach((doc) => {
      const uid = doc.data().userId;
      if (uid) recipientIds.add(String(uid).trim());
    });
    recipientIds.delete(commenterId);

    if (recipientIds.size === 0) return null;

    const commenterDoc = await db.collection("users").doc(commenterId).get();
    const cu = commenterDoc.data() || {};
    const actorName = String(cu.displayName || cu.username || "Someone");
    const actorPhotoUrl = cu.avatarUrl != null ? String(cu.avatarUrl) : "";
    const preview = twoLinePreview(comment.text);

    const refs = [...recipientIds]
      .filter((id) => id && id.length > 0)
      .map((id) => db.collection("users").doc(id));
    const userSnaps = await getAllSnapshots(refs);
    const tokens = [];
    userSnaps.forEach((s) => {
      if (s.exists) {
        const t = s.data().fcmToken;
        if (t && typeof t === "string" && t.length > 0) tokens.push(t);
      }
    });

    const uniqueTokens = [...new Set(tokens)];
    if (uniqueTokens.length === 0) {
      console.warn("onCommentCreated: no fcmToken for recipients", [...recipientIds]);
      return null;
    }

    // Document id for this comment (matches app + Firestore path); required in FCM data for deep link / scroll-to.
    const newCommentId = String(snap.id || context.params.commentId || "").trim();
    if (!newCommentId) {
      console.error("onCommentCreated: missing comment id", postId);
      return null;
    }

    const payload = {
      type: "comment",
      postId: String(postId),
      commentId: newCommentId,
      actorId: commenterId,
      actorName,
      actorPhotoUrl,
      preview,
    };
    const ok = await sendMulticastDataAndNotification(uniqueTokens, payload);
    console.log("onCommentCreated sent", ok, "/", uniqueTokens.length);
    return null;
  });

exports.onLikeCreated = functions.firestore
  .document("posts/{postId}/likes/{likeUserId}")
  .onCreate(async (snap, context) => {
    const postId = context.params.postId;
    const likerId = context.params.likeUserId;

    const postSnap = await db.collection("posts").doc(postId).get();
    if (!postSnap.exists) return null;
    const authorId = postSnap.data().userId;
    if (authorId === likerId) return null;

    const likerDoc = await db.collection("users").doc(String(likerId)).get();
    const u = likerDoc.data() || {};
    const actorName = String(u.displayName || u.username || "Someone");
    const actorPhotoUrl = u.avatarUrl != null ? String(u.avatarUrl) : "";

    const authorDoc = await db.collection("users").doc(String(authorId)).get();
    const token = authorDoc.data()?.fcmToken;

    await sendTokenDataAndNotification(token, {
      type: "like",
      postId: String(postId),
      actorId: String(likerId || ""),
      actorName,
      actorPhotoUrl,
      preview: "Liked your post",
    });
    return null;
  });

/**
 * Notifies the **comment author** when someone likes their comment (not the post author).
 */
exports.onCommentLikeCreated = functions.firestore
  .document("posts/{postId}/comments/{commentId}/commentLikes/{likeUserId}")
  .onCreate(async (snap, context) => {
    const postId = context.params.postId;
    const commentId = context.params.commentId;
    const likerId = context.params.likeUserId;

    const commentSnap = await db
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc(commentId)
      .get();
    if (!commentSnap.exists) return null;
    const commentAuthorId = String(commentSnap.data().userId || "").trim();
    if (!commentAuthorId || commentAuthorId === likerId) return null;

    const likerDoc = await db.collection("users").doc(String(likerId)).get();
    const u = likerDoc.data() || {};
    const actorName = String(u.displayName || u.username || "Someone");
    const actorPhotoUrl = u.avatarUrl != null ? String(u.avatarUrl) : "";

    const authorDoc = await db.collection("users").doc(commentAuthorId).get();
    const token = authorDoc.data()?.fcmToken;

    await sendTokenDataAndNotification(token, {
      type: "comment_like",
      postId: String(postId),
      commentId: String(commentId),
      actorId: String(likerId || ""),
      actorName,
      actorPhotoUrl,
      preview: "Liked your comment",
    });
    return null;
  });

/**
 * Deletes the caller’s Firestore profile (`users/{uid}`), drains `users/{uid}/notifications`,
 * then deletes the Firebase Auth user. Uses Admin SDK so it is not blocked by client rules.
 * Call from the account-delete web page or the app after the user is signed in.
 */
exports.deleteOwnAccount = functions.https.onCall(async (data, context) => {
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError("unauthenticated", "Sign in is required.");
  }
  const uid = context.auth.uid;

  try {
    const notifRef = db.collection("users").doc(uid).collection("notifications");
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const snap = await notifRef.limit(500).get();
      if (snap.empty) break;
      const batch = db.batch();
      snap.docs.forEach((d) => batch.delete(d.ref));
      await batch.commit();
    }

    await db.collection("users").doc(uid).delete().catch(() => {});

    await admin.auth().deleteUser(uid);
    return { ok: true };
  } catch (e) {
    console.error("deleteOwnAccount failed", uid, e);
    const msg = e && e.message ? String(e.message) : "Deletion failed";
    throw new functions.https.HttpsError("internal", msg);
  }
});

exports.onDislikeCreated = functions.firestore
  .document("posts/{postId}/dislikes/{dislikeUserId}")
  .onCreate(async (snap, context) => {
    const postId = context.params.postId;
    const dislikerId = context.params.dislikeUserId;

    const postSnap = await db.collection("posts").doc(postId).get();
    if (!postSnap.exists) return null;
    const authorId = postSnap.data().userId;
    if (authorId === dislikerId) return null;

    const dislikerDoc = await db.collection("users").doc(String(dislikerId)).get();
    const u = dislikerDoc.data() || {};
    const actorName = String(u.displayName || u.username || "Someone");
    const actorPhotoUrl = u.avatarUrl != null ? String(u.avatarUrl) : "";

    const authorDoc = await db.collection("users").doc(String(authorId)).get();
    const token = authorDoc.data()?.fcmToken;

    await sendTokenDataAndNotification(token, {
      type: "dislike",
      postId: String(postId),
      actorId: String(dislikerId || ""),
      actorName,
      actorPhotoUrl,
      preview: "Disliked your post",
    });
    return null;
  });

// ---------------------------------------------------------------------------
// Post detail web page — serves HTML for https://forzaball.app/post/<id>
// Provides Open Graph tags for social-media link previews and attempts to
// deep-link into the native app, falling back to a styled landing page.
// ---------------------------------------------------------------------------

/** Escape HTML-sensitive characters to prevent XSS in interpolated strings. */
function esc(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function formatCount(n) {
  const num = Number(n) || 0;
  if (num >= 1_000_000) return (num / 1_000_000).toFixed(1).replace(/\.0$/, "") + "M";
  if (num >= 1_000) return (num / 1_000).toFixed(1).replace(/\.0$/, "") + "K";
  return String(num);
}

function timeAgo(millis) {
  const diff = Date.now() - Number(millis || 0);
  const s = Math.floor(diff / 1000);
  if (s < 60) return "just now";
  const m = Math.floor(s / 60);
  if (m < 60) return m + "m ago";
  const h = Math.floor(m / 60);
  if (h < 24) return h + "h ago";
  const d = Math.floor(h / 24);
  if (d < 7) return d + "d ago";
  return new Date(Number(millis)).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function postPageHtml(postId, post, author) {
  const authorName = esc(author.displayName || author.username || "ForzaBall User");
  const authorHandle = esc(author.username || "");
  const avatarUrl = esc(
    author.avatarUrl || `https://i.pravatar.cc/150?u=${esc(post.userId)}`,
  );
  const text = esc(post.text || "");
  const ogDescription = (post.text || "Check out this post on ForzaBall").substring(0, 200);
  const deepLink = `forzaball://post/${esc(postId)}`;
  const webUrl = `https://forzaball.app/post/${esc(postId)}`;
  const likes = formatCount(post.likeCount);
  const dislikes = formatCount(post.dislikeCount);
  const comments = formatCount(post.commentCount);
  const ago = timeAgo(post.createdAtMillis || post.createdAt?._seconds * 1000);

  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>${authorName} on ForzaBall</title>

<!-- Open Graph -->
<meta property="og:type" content="article"/>
<meta property="og:title" content="${authorName} on ForzaBall"/>
<meta property="og:description" content="${esc(ogDescription)}"/>
<meta property="og:url" content="${webUrl}"/>
<meta property="og:site_name" content="ForzaBall"/>
<meta property="og:image" content="${avatarUrl}"/>

<!-- Twitter Card -->
<meta name="twitter:card" content="summary"/>
<meta name="twitter:title" content="${authorName} on ForzaBall"/>
<meta name="twitter:description" content="${esc(ogDescription)}"/>
<meta name="twitter:image" content="${avatarUrl}"/>

<!-- Deep-link: Android intent, iOS universal link, custom scheme fallback -->
<meta property="al:android:url" content="${deepLink}"/>
<meta property="al:android:package" content="com.forzaball"/>
<meta property="al:android:app_name" content="ForzaBall"/>

<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;background:#0d1b1a;color:#e8e8e8;min-height:100vh;display:flex;flex-direction:column;align-items:center}
.container{max-width:560px;width:100%;padding:24px 16px}
.header{display:flex;align-items:center;gap:12px;padding:16px 0}
.logo-text{font-size:22px;font-weight:800;color:#29a847;letter-spacing:-0.5px}
.card{background:#142221;border-radius:16px;padding:20px;margin-top:8px;border:1px solid rgba(255,255,255,0.06)}
.author-row{display:flex;align-items:center;gap:12px}
.avatar{width:48px;height:48px;border-radius:50%;object-fit:cover;background:#1a3332}
.author-info{flex:1}
.author-name{font-size:16px;font-weight:700;color:#f0f0f0}
.author-handle{font-size:13px;color:#8b9e9c;margin-top:1px}
.time{font-size:12px;color:#8b9e9c;margin-top:2px}
.post-text{margin-top:16px;font-size:15px;line-height:1.55;color:#d4d4d4;white-space:pre-wrap;word-break:break-word}
.stats{display:flex;gap:24px;margin-top:18px;padding-top:14px;border-top:1px solid rgba(255,255,255,0.06)}
.stat{display:flex;align-items:center;gap:5px;font-size:13px;color:#8b9e9c}
.stat svg{width:18px;height:18px;fill:#8b9e9c}
.actions{margin-top:28px;display:flex;flex-direction:column;gap:12px}
.btn-open{display:block;text-align:center;padding:14px 20px;border-radius:12px;background:#29a847;color:#fff;font-size:16px;font-weight:700;text-decoration:none;transition:background .15s}
.btn-open:hover{background:#23903d}
.btn-store{display:block;text-align:center;padding:13px 20px;border-radius:12px;background:rgba(255,255,255,0.07);color:#e8e8e8;font-size:14px;font-weight:600;text-decoration:none;border:1px solid rgba(255,255,255,0.1);transition:background .15s}
.btn-store:hover{background:rgba(255,255,255,0.12)}
.store-row{display:flex;gap:12px}
.store-row a{flex:1}
.footer{margin-top:40px;padding:20px 0;text-align:center;font-size:12px;color:#5a706e}
</style>
</head>
<body>
<div class="container">
  <div class="header">
    <svg width="32" height="32" viewBox="0 0 32 32" fill="none"><circle cx="16" cy="16" r="16" fill="#29a847"/><text x="16" y="21" text-anchor="middle" font-size="16" font-weight="bold" fill="#fff">F</text></svg>
    <span class="logo-text">ForzaBall</span>
  </div>

  <div class="card">
    <div class="author-row">
      <img class="avatar" src="${avatarUrl}" alt="" onerror="this.style.display='none'"/>
      <div class="author-info">
        <div class="author-name">${authorName}</div>
        ${authorHandle ? `<div class="author-handle">@${authorHandle}</div>` : ""}
        <div class="time">${esc(ago)}</div>
      </div>
    </div>
    <div class="post-text">${text}</div>
    <div class="stats">
      <div class="stat">
        <svg viewBox="0 0 24 24"><path d="M2 20h2c.55 0 1-.45 1-1v-9c0-.55-.45-1-1-1H2v11zm19.83-7.12c.11-.25.17-.52.17-.8V11c0-1.1-.9-2-2-2h-5.5l.92-4.65c.05-.22.02-.46-.08-.66L14.17 2 7.59 8.59C7.22 8.95 7 9.45 7 10v8c0 1.1.9 2 2 2h9c.78 0 1.47-.46 1.79-1.11l2.91-6.81c.09-.21.09-.44.04-.2z"/></svg>
        ${likes}
      </div>
      <div class="stat">
        <svg viewBox="0 0 24 24"><path d="M22 4h-2c-.55 0-1 .45-1 1v9c0 .55.45 1 1 1h2V4zM2.17 11.12c-.11.25-.17.52-.17.8V13c0 1.1.9 2 2 2h5.5l-.92 4.65c-.05.22-.02.46.08.66L9.83 22l6.58-6.59c.36-.36.59-.86.59-1.41V6c0-1.1-.9-2-2-2H6c-.78 0-1.47.46-1.79 1.11l-2.91 6.81c-.09.21-.09.44-.04.2z"/></svg>
        ${dislikes}
      </div>
      <div class="stat">
        <svg viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/></svg>
        ${comments}
      </div>
    </div>
  </div>

  <div class="actions">
    <a class="btn-open" id="openApp" href="${deepLink}">Open in ForzaBall</a>
    <div class="store-row">
      <a class="btn-store" href="https://play.google.com/store/apps/details?id=com.forzaball" target="_blank" rel="noopener">Google Play</a>
      <a class="btn-store" href="https://apps.apple.com/app/forzaball/id6740080498" target="_blank" rel="noopener">App Store</a>
    </div>
  </div>

  <div class="footer">&copy; ${new Date().getFullYear()} ForzaBall. All rights reserved.</div>
</div>

<script>
(function(){
  var ua = navigator.userAgent || "";
  var isBot = /bot|crawl|spider|slurp|facebookexternalhit|Twitterbot|WhatsApp|TelegramBot|LinkedInBot|Discordbot/i.test(ua);
  if (isBot) return;

  var deepLink = "${deepLink}";
  var playStore = "https://play.google.com/store/apps/details?id=com.forzaball";
  var appStore = "https://apps.apple.com/app/forzaball/id6740080498";
  var isAndroid = /android/i.test(ua);
  var isIOS = /iPhone|iPad|iPod/i.test(ua);

  var intentUrl = "intent://post/${esc(postId)}#Intent;scheme=forzaball;package=com.forzaball;S.browser_fallback_url=" + encodeURIComponent(playStore) + ";end";

  if (isAndroid) {
    window.location.replace(intentUrl);
  } else if (isIOS) {
    window.location.replace(deepLink);
    setTimeout(function(){ window.location.replace(appStore); }, 1500);
  }
})();
</script>
</body>
</html>`;
}

function notFoundPageHtml() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>Post Not Found — ForzaBall</title>
<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;background:#0d1b1a;color:#e8e8e8;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:24px}
h1{font-size:24px;font-weight:800;color:#29a847;margin-bottom:12px}
p{color:#8b9e9c;font-size:15px;margin-bottom:24px}
a{display:inline-block;padding:12px 28px;border-radius:12px;background:#29a847;color:#fff;font-weight:700;text-decoration:none}
a:hover{background:#23903d}
</style>
</head>
<body>
<h1>Post Not Found</h1>
<p>This post may have been removed or the link is invalid.</p>
<a href="https://forzaball.app">Go to ForzaBall</a>
</body>
</html>`;
}

exports.postPage = functions.https.onRequest(async (req, res) => {
  const segments = req.path.replace(/^\/+|\/+$/g, "").split("/");
  // Expected path: /post/<postId> — "post" segment may be index 0 (function URL) or after prefix
  const postIdx = segments.indexOf("post");
  const postId = postIdx >= 0 && postIdx < segments.length - 1
    ? segments[postIdx + 1]
    : segments[segments.length - 1];

  if (!postId || postId === "post") {
    res.status(404).send(notFoundPageHtml());
    return;
  }

  try {
    const postSnap = await db.collection("posts").doc(postId).get();
    if (!postSnap.exists) {
      res.status(404).send(notFoundPageHtml());
      return;
    }
    const post = postSnap.data() || {};
    const userId = String(post.userId || "");
    let author = {};
    if (userId) {
      const userSnap = await db.collection("users").doc(userId).get();
      if (userSnap.exists) author = userSnap.data() || {};
    }

    res.set("Cache-Control", "public, max-age=300, s-maxage=600");
    res.status(200).send(postPageHtml(postId, post, author));
  } catch (err) {
    console.error("postPage error", postId, err);
    res.status(500).send(notFoundPageHtml());
  }
});
