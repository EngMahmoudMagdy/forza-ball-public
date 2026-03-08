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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.forzaball.app.ui.theme.ForzaBallBackgroundDark
import com.forzaball.app.ui.theme.ForzaBallPrimary

private const val LOGO_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCkAL2u94GltH0gb0jToC4o5aOD9YxRPw9NwKb1flAI-bCwrMMoOkHeV26eLvKLGFYBmnedNJCHIH2iaP2-YM6Jks4PM85oZZ-psd7wrCudFSy8o1zkCok8ETtOnWEb-PVq-jeayk33qCePTvmz8sOm_5_UiZZgBAQnSUD25LexPnQJhFN4saDpwL8f_27y4z6jELXkmQ-K6gx5cMcrEYXCF1e9LAn6GQQulXGX272kE055COHIQYvXBVy1y_xDcXIl67bEce_ztKId"

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
                model = LOGO_URL,
                contentDescription = "ForzaBall Logo",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ForzaBall",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).sp,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(24.dp))
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
