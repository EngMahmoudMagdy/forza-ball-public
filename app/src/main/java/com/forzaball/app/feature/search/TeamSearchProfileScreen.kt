package com.forzaball.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forzaball.app.feature.home.FavoriteMatchCard
import com.forzaball.app.feature.home.NewsCardHorizontal
import com.forzaball.app.ui.theme.ForzaBallPrimary
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSearchProfileRoute(
    onBack: () -> Unit,
    onOpenNewsArticle: (String, String) -> Unit,
    viewModel: TeamSearchProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    TeamSearchProfileScreen(
        state = state,
        onBack = onBack,
        onOpenNewsArticle = onOpenNewsArticle,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSearchProfileScreen(
    state: TeamSearchProfileState,
    onBack: () -> Unit,
    onOpenNewsArticle: (String, String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Text(
                "Loading…",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.teamCrestUrl != null) {
                        AsyncImage(
                            model = state.teamCrestUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    Text(
                        state.teamDisplayName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(Modifier.height(20.dp))
                state.errorMessage?.let { err ->
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    "Match",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ForzaBallPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                if (state.match != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (state.isLive) {
                            Text(
                                "LIVE",
                                color = ForzaBallPrimary,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        } else {
                            Text(
                                "Up next",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FavoriteMatchCard(match = state.match)
                } else {
                    Text(
                        "No live or upcoming match found right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "News",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ForzaBallPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                if (state.news.isEmpty()) {
                    Text(
                        "No team-tagged news in this window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.news.forEach { article ->
                        val url = article.articleUrl.orEmpty()
                        NewsCardHorizontal(
                            article = article,
                            modifier = Modifier
                                .clip(RoundedCornerShape(0.dp))
                                .padding(horizontal = 0.dp),
                            onClick = { if (url.isNotEmpty()) onOpenNewsArticle(url, article.title) },
                        )
                    }
                }
            }
        }
    }
}
