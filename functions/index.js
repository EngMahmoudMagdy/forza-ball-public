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
