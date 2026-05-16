package com.forzaball.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

private const val AVATAR_PLACEHOLDER = "https://i.pravatar.cc/150?u="

@Composable
fun ProfileAvatarImage(
    photoUrl: String?,
    thumbUrl: String?,
    cacheVersion: Long,
    fallbackUserId: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val displayUrl = thumbUrl?.takeIf { it.isNotBlank() } ?: photoUrl?.takeIf { it.isNotBlank() }
    val model = remember(displayUrl, cacheVersion, fallbackUserId) {
        if (displayUrl == null) {
            "$AVATAR_PLACEHOLDER$fallbackUserId"
        } else {
            ImageRequest.Builder(context)
                .data(displayUrl)
                .memoryCacheKey("profile_${displayUrl}_$cacheVersion")
                .diskCacheKey("profile_${displayUrl}_$cacheVersion")
                .crossfade(true)
                .build()
        }
    }
    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}
