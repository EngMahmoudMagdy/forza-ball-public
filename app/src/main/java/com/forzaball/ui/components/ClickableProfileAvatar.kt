package com.forzaball.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

@Composable
fun ClickableProfileAvatar(
    imageUrl: String?,
    fallbackUserId: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = imageUrl?.takeIf { it.isNotBlank() }
        ?: "https://i.pravatar.cc/150?u=$fallbackUserId"
    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop,
    )
}
