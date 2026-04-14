package com.forzaball.app.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.forzaball.app.ui.theme.ForzaBallPrimary

@Composable
fun ListLoadingHeaderShimmer(modifier: Modifier = Modifier) {
    ShimmerBlock(height = 72.dp, modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
fun ListLoadingFooterShimmer(modifier: Modifier = Modifier) {
    ShimmerBlock(height = 56.dp, modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun ShimmerBlock(height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerShift",
    )
    val brush = Brush.horizontalGradient(
        colors = listOf(
            ForzaBallPrimary.copy(alpha = 0.08f + shift * 0.12f),
            ForzaBallPrimary.copy(alpha = 0.22f),
            ForzaBallPrimary.copy(alpha = 0.08f + (1f - shift) * 0.12f),
        ),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(brush),
    )
}

@Composable
fun FullScreenShimmerPlaceholder() {
    Column(Modifier.padding(16.dp)) {
        repeat(6) {
            ShimmerBlock(height = 88.dp, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}
