package com.forzaball.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp

@Composable
fun ClickableProfileAvatar(
    photoUrl: String?,
    thumbUrl: String?,
    cacheVersion: Long,
    fallbackUserId: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileAvatarImage(
        photoUrl = photoUrl,
        thumbUrl = thumbUrl,
        cacheVersion = cacheVersion,
        fallbackUserId = fallbackUserId,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    )
}
