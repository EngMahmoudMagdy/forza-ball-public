const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const FEED_TOPIC = "feed_all";

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
 * Same data payload to many devices; Android high priority for timely wake/delivery (incl. Android 15).
 * @param {string[]} tokens
 * @param {Record<string, string>} data
 */
async function sendDataMulticastHighPriority(tokens, data) {
  const unique = [...new Set((tokens || []).filter((t) => t && typeof t === "string"))];
  if (unique.length === 0) return 0;
  const dataStr = stringifyData(data);
  let success = 0;
  for (let i = 0; i < unique.length; i += FCM_MULTICAST_CHUNK) {
    const chunk = unique.slice(i, i + FCM_MULTICAST_CHUNK);
    const resp = await admin.messaging().sendEachForMulticast({
      tokens: chunk,
      data: dataStr,
      android: { priority: "high" },
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

/** @param {Record<string, string>} data */
async function sendToToken(token, data) {
  if (!token || typeof token !== "string") return;
  const dataStr = stringifyData(data);
  await admin.messaging().send({
    token,
    data: dataStr,
    android: { priority: "high" },
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

    await admin.messaging().send({
      topic: FEED_TOPIC,
      data: stringifyData({
        type: "new_post",
        postId: String(postId),
        actorId: String(actorId || ""),
        actorName,
        actorPhotoUrl,
        preview,
      }),
      android: { priority: "high" },
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

    const payload = {
      type: "comment",
      postId: String(postId),
      actorId: commenterId,
      actorName,
      actorPhotoUrl,
      preview,
    };
    const ok = await sendDataMulticastHighPriority(uniqueTokens, payload);
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

    await sendToToken(token, {
      type: "like",
      postId: String(postId),
      actorId: String(likerId || ""),
      actorName,
      actorPhotoUrl,
      preview: "Liked your post",
    });
    return null;
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

    await sendToToken(token, {
      type: "dislike",
      postId: String(postId),
      actorId: String(dislikerId || ""),
      actorName,
      actorPhotoUrl,
      preview: "Disliked your post",
    });
    return null;
  });
