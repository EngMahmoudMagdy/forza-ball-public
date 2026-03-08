package com.forzaball.app.feature.home

import android.R.color.white
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forzaball.domain.model.NewsArticle
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.processIntent(HomeIntent.Load)
    }

    HomeScreen(state = state)
}

@Composable
fun HomeScreen(
    state: HomeState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(Color.Cyan),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        state.favoriteClubMatch?.let { match ->
            Text(
                text = if (match.isLive) {
                    "Live: ${match.homeClub.name} vs ${match.awayClub.name}"
                } else {
                    "Next: ${match.homeClub.name} vs ${match.awayClub.name}"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.news) { article ->
                NewsCard(article = article)
            }
        }
    }
}

@Composable
private fun NewsCard(
    article: NewsArticle,
) {
    Card {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            article.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = article.title,
                )
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

