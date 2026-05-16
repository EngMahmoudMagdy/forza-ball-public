package com.forzaball.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListRoute(
    onBack: () -> Unit,
    onOpenArticle: (String, String) -> Unit,
    /** When true, used as a main tab: no app bar or back affordance. */
    embeddedInTab: Boolean = false,
    viewModel: NewsListViewModel = koinViewModel(),
) {
    val lazyItems = viewModel.newsPaging.collectAsLazyPagingItems()
    val refresh = lazyItems.loadState.refresh
    val append = lazyItems.loadState.append

    val list: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(
            modifier = listModifier,
        ) {
            when (val state = refresh) {
                is LoadState.Loading -> {
                    if (lazyItems.itemCount == 0) {
                        item {
                            ListLoadingHeaderShimmer()
                            FullScreenShimmerPlaceholder()
                        }
                    }
                }
                is LoadState.Error -> {
                    if (lazyItems.itemCount == 0) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = state.error.localizedMessage ?: "Couldn’t load news",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Button(onClick = { lazyItems.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                is LoadState.NotLoading -> Unit
            }

            items(
                count = lazyItems.itemCount,
                key = { index -> lazyItems[index]?.id ?: "slot-$index" },
            ) { index ->
                val article = lazyItems[index] ?: return@items
                NewsCardHorizontal(
                    article = article,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = article.articleUrl?.let { url ->
                        { onOpenArticle(url, article.title) }
                    },
                )
            }

            if (append is LoadState.Loading && lazyItems.itemCount > 0) {
                item {
                    ListLoadingFooterShimmer()
                }
            }
            if (append is LoadState.Error) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = append.error.localizedMessage ?: "Couldn’t load more",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(onClick = { lazyItems.retry() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            if (refresh is LoadState.NotLoading && lazyItems.itemCount == 0) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "No news yet — choose your favorite team in your profile.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (embeddedInTab) {
        list(Modifier.fillMaxSize())
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "News",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            list(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}
