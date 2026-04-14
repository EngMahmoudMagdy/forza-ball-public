package com.forzaball.app.notifications

object FeedPushConstants {
    const val EXTRA_OPEN_FEED_POST_ID = "com.forzaball.EXTRA_OPEN_FEED_POST_ID"
    const val EXTRA_OPEN_FEED_COMMENT_ID = "com.forzaball.EXTRA_OPEN_FEED_COMMENT_ID"
    /** High importance; new id so existing installs pick up heads-up / priority behavior (channels are immutable). */
    const val NOTIFICATION_CHANNEL_ID = "feed_social_high"
}
