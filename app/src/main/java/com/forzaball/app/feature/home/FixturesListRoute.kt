package com.forzaball.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import coil.compose.AsyncImage
import com.forzaball.domain.model.Match
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixturesListRoute(
    onBack: () -> Unit,
    viewModel: FixturesListViewModel = koinViewModel(),
) {
    val lazyItems = viewModel.fixturesPaging.collectAsLazyPagingItems()
    val refresh = lazyItems.loadState.refresh
    val append = lazyItems.loadState.append

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Fixtures",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                                    text = state.error.localizedMessage ?: "Couldn’t load fixtures",
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
                val match = lazyItems[index] ?: return@items
                FixturePagedRow(match = match)
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
                    ) {
                        Text(
                            "No fixtures — add favorite leagues or teams in your profile.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private val timeFmt = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault())

@Composable
private fun FixturePagedRow(match: Match) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = match.leagueName ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = timeFmt.format(Date(match.startTimeMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = match.homeClub.crestUrl,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        match.homeClub.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                }
                Text(
                    text = scoreLinePaged(match),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        match.awayClub.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    AsyncImage(
                        model = match.awayClub.crestUrl,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            if (match.isLive || match.statusShort != null) {
                Text(
                    text = listOfNotNull(
                        if (match.isLive) "LIVE" else null,
                        match.statusShort,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun scoreLinePaged(match: Match): String {
    val hs = match.homeScore
    val a = match.awayScore
    return if (hs != null && a != null) "$hs – $a" else "vs"
}
