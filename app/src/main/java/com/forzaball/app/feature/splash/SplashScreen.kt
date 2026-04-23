package com.forzaball.app.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forzaball.app.R
import com.forzaball.app.ui.theme.ForzaBallBackgroundDark
import com.forzaball.app.ui.theme.ForzaBallPrimary

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ForzaBallBackgroundDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = R.drawable.forzaball_brand,
                contentDescription = "ForzaBall",
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = ForzaBallPrimary,
                strokeWidth = 3.dp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading…",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}
