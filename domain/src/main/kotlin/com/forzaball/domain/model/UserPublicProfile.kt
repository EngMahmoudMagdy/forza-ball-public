package com.forzaball.domain.model

/** Public user profile shown on profile screens and enriched feed rows. */
data class UserPublicProfile(
    val userId: String,
    val displayName: String,
    val handle: String,
    /** Full-quality avatar for profile header / fullscreen. */
    val avatarUrl: String?,
    /** Small avatar for feed and comment lists. */
    val avatarThumbUrl: String?,
)
