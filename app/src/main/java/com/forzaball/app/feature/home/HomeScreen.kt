package com.forzaball.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.forzaball.app.ui.theme.ForzaBallTheme
import com.forzaball.domain.model.Club
import com.forzaball.app.feature.personalization.catalogLeagues
import com.forzaball.app.feature.profile.EditFavoritesOverlay
import com.forzaball.app.feature.profile.EditFavoritesViewModel
import com.forzaball.app.feature.profile.ProfileViewModel
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.TeamNextMatch
import com.forzaball.domain.model.TeamStandingSnapshot
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.AuthState
import com.forzaball.app.feature.feeds.CreatePostOverlay
import com.forzaball.app.notifications.FeedOpenRequest
import com.forzaball.app.feature.feeds.FeedPostDetailRoute
import com.forzaball.app.feature.feeds.FeedViewModel
import com.forzaball.app.feature.feeds.FeedsRoute
import com.forzaball.app.core.shared_ui_components.SwipeRefreshSharedComponent
import com.forzaball.app.ui.theme.ForzaBallPrimary
import org.koin.androidx.compose.koinViewModel

private const val LOGO_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCkAL2u94GltH0gb0jToC4o5aOD9YxRPw9NwKb1flAI-bCwrMMoOkHeV26eLvKLGFYBmnedNJCHIH2iaP2-YM6Jks4PM85oZZ-psd7wrCudFSy8o1zkCok8ETtOnWEb-PVq-jeayk33qCePTvmz8sOm_5_UiZZgBAQnSUD25LexPnQJhFN4saDpwL8f_27y4z6jELXkmQ-K6gx5cMcrEYXCF1e9LAn6GQQulXGX272kE055COHIQYvXBVy1y_xDcXIl67bEce_ztKId"

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = koinViewModel(),
    feedViewModel: FeedViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    editFavoritesViewModel: EditFavoritesViewModel = koinViewModel(),
    /** Cold start / deep link: open Feeds tab and this post (optionally a comment). */
    initialFeedOpen: FeedOpenRequest? = null,
    /** Opens a post from the global in-app banner (root overlay). */
    bannerOpenRequest: FeedOpenRequest? = null,
    onBannerOpenConsumed: () -> Unit = {},
    onNavigateToSignIn: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToFixturesList: () -> Unit = {},
    onOpenNewsArticle: (String, String) -> Unit = { _, _ -> },
    onNavigateToSearch: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.processIntent(HomeIntent.Load)
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var feedOverlayKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditFavorites by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialFeedOpen) {
        val open = initialFeedOpen ?: return@LaunchedEffect
        selectedTab = 2
        feedOverlayKey = open.overlayKey
    }

    LaunchedEffect(bannerOpenRequest) {
        val open = bannerOpenRequest ?: return@LaunchedEffect
        selectedTab = 2
        feedOverlayKey = open.overlayKey
        onBannerOpenConsumed()
    }

    HomeScreen(
        state = state,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        feedViewModel = feedViewModel,
        feedOverlayKey = feedOverlayKey,
        onFeedOverlayKeyChange = { feedOverlayKey = it },
        onLogout = profileViewModel::signOut,
        onNavigateToSignIn = onNavigateToSignIn,
        onNavigateToSignUp = onNavigateToSignUp,
        showEditFavorites = showEditFavorites,
        onShowEditFavorites = { showEditFavorites = true },
        onDismissEditFavorites = { showEditFavorites = false },
        editFavoritesViewModel = editFavoritesViewModel,
        onNavigateToFixturesList = onNavigateToFixturesList,
        onViewAllNews = { selectedTab = 1 },
        onOpenNewsArticle = onOpenNewsArticle,
        onRefresh = { viewModel.processIntent(HomeIntent.Refresh()) },
        onNavigateToSearch = onNavigateToSearch,
    )
}

@Composable
fun HomeScreen(
    state: HomeState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    feedViewModel: FeedViewModel,
    feedOverlayKey: String?,
    onFeedOverlayKeyChange: (String?) -> Unit,
    onLogout: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    showEditFavorites: Boolean,
    onShowEditFavorites: () -> Unit,
    onDismissEditFavorites: () -> Unit,
    editFavoritesViewModel: EditFavoritesViewModel,
    onNavigateToFixturesList: () -> Unit,
    onViewAllNews: () -> Unit,
    onOpenNewsArticle: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val feedUi by feedViewModel.ui.collectAsState()
    val authState by feedViewModel.authState.collectAsState()
    val scoresViewModel: ScoresViewModel = koinViewModel()

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(
                title = homeTitleForTab(selectedTab),
                onSearchClick = onNavigateToSearch,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (selectedTab) {
                    0 -> HomeDashboardContent(
                        state = state,
                        onViewAllFixtures = onNavigateToFixturesList,
                        onViewAllNews = onViewAllNews,
                        onOpenNewsArticle = onOpenNewsArticle,
                        onRefresh = onRefresh,
                    )
                    1 -> NewsListRoute(
                        embeddedInTab = true,
                        onBack = {},
                        onOpenArticle = onOpenNewsArticle,
                    )
                    2 -> FeedsRoute(
                        viewModel = feedViewModel,
                        onOpenCreatePost = { onFeedOverlayKeyChange("create") },
                        onOpenPost = { onFeedOverlayKeyChange("post:$it") },
                    )
                    3 -> ScoresTabContent(
                        scoresViewModel = scoresViewModel,
                        userPreferences = state.userPreferences,
                    )
                    4 -> ProfileTabContent(
                        authState = authState,
                        userPreferences = state.userPreferences,
                        onLogout = onLogout,
                        onSignIn = onNavigateToSignIn,
                        onSignUp = onNavigateToSignUp,
                        onEditFavorites = onShowEditFavorites,
                    )
                    else -> PlaceholderTabContent(
                        title = homeTitleForTab(selectedTab),
                    )
                }
            }

            HomeBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        EditFavoritesOverlay(
            visible = showEditFavorites && selectedTab == 4,
            viewModel = editFavoritesViewModel,
            onDismiss = onDismissEditFavorites,
        )

        when (feedOverlayKey) {
            null -> {}
            "create" -> {
                if (authState is AuthState.SignedIn) {
                    CreatePostOverlay(
                        isPosting = feedUi.isPosting,
                        onDismiss = { onFeedOverlayKeyChange(null) },
                        onSubmit = { text ->
                            feedViewModel.createPost(text) { result ->
                                if (result.isSuccess) onFeedOverlayKeyChange(null)
                            }
                        },
                    )
                }
            }
            else -> {
                val key = feedOverlayKey
                if (key != null && key.startsWith("post:")) {
                    val open = FeedOpenRequest.fromOverlayKey(key)
                    if (open != null) {
                        val uid = (authState as? AuthState.SignedIn)?.uid
                        FeedPostDetailRoute(
                            postId = open.postId,
                            highlightCommentId = open.highlightCommentId,
                            currentUserId = uid,
                            viewModel = feedViewModel,
                            onBack = { onFeedOverlayKeyChange(null) },
                        )
                    }
                }
            }
        }
    }
}

private fun homeTitleForTab(tab: Int): String = when (tab) {
    0 -> "ForzaBall"
    1 -> "News"
    2 -> "Feeds"
    3 -> "Scores"
    4 -> "Profile"
    else -> "ForzaBall"
}

@Composable
private fun HomeTopBar(
    title: String,
    onSearchClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            .border(1.dp, ForzaBallPrimary.copy(alpha = 0.1f), RoundedCornerShape(0.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ForzaBallPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = LOGO_URL,
                    contentDescription = "ForzaBall Logo",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(
                onClick = { },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun HomeDashboardContent(
    state: HomeState,
    onViewAllFixtures: () -> Unit,
    onViewAllNews: () -> Unit,
    onOpenNewsArticle: (String, String) -> Unit,
    onRefresh: () -> Unit,
) {
    SwipeRefreshSharedComponent(
        modifier = Modifier.fillMaxSize(),
        isRefresh = state.isLoading,
        onRefresh = onRefresh,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        ) {
        if (state.isLoading || state.favoriteClubMatch != null) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Your Favorite Club",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = ForzaBallPrimary,
                    )
                    Text(
                        text = "FOLLOWING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = ForzaBallPrimary,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
                            .background(
                                ForzaBallPrimary.copy(alpha = 0.1f),
                                RoundedCornerShape(9999.dp),
                            )
                            .border(
                                1.dp,
                                ForzaBallPrimary.copy(alpha = 0.2f),
                                RoundedCornerShape(9999.dp),
                            )
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (state.isLoading) {
                    ListLoadingHeaderShimmer(modifier = Modifier.fillMaxWidth())
                } else {
                    state.favoriteClubMatch?.let { match ->
                    FavoriteMatchCard(match = match)
                    }
                }
            }
        }
        }

        val hasFavoriteTeam = !state.userPreferences.favoriteTeamId.isNullOrBlank()
        if (state.isLoading || hasFavoriteTeam) {
            item {
                Text(
                    text = "Next match for your team",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    if (state.isLoading) {
                        ListLoadingFooterShimmer(modifier = Modifier.fillMaxWidth())
                    } else {
                        state.favoriteTeamNextMatch?.let { row ->
                            TeamNextMatchCard(row = row, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        if (state.isLoading || state.liveMatches.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Live Scores",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = ForzaBallPrimary,
                    modifier = Modifier.clickable(onClick = onViewAllFixtures),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isLoading) {
                    items(3) {
                        ListLoadingFooterShimmer(modifier = Modifier.width(176.dp))
                    }
                } else {
                    items(state.liveMatches) { match ->
                        LiveScoreCard(match = match)
                    }
                }
            }
        }
        }

        if (state.isLoading || state.news.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Latest News for You",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = ForzaBallPrimary,
                    modifier = Modifier.clickable(onClick = onViewAllNews),
                )
            }
        }
        if (state.isLoading) {
            item { ListLoadingHeaderShimmer() }
            items(3) {
                ListLoadingFooterShimmer()
            }
        } else {
            itemsIndexed(state.news, key = { _, a -> a.id }) { index, article ->
                val open = article.articleUrl?.let { u ->
                    { onOpenNewsArticle(u, article.title) }
                }
                if (index == 0) {
                    NewsCardLarge(article = article, onClick = open)
                } else {
                    NewsCardHorizontal(article = article, onClick = open)
                }
            }
        }
        }
    }
    }
}

@Composable
private fun TeamNextMatchCard(row: TeamNextMatch, modifier: Modifier = Modifier.width(260.dp)) {
    val m = row.nextMatch
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = row.teamCrestUrl,
                    contentDescription = row.teamDisplayName,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = row.teamDisplayName.ifBlank { "Team ${row.teamId}" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (m != null) {
                m.leagueName?.takeIf { it.isNotBlank() }?.let { ln ->
                    Text(
                        text = ln,
                        style = MaterialTheme.typography.labelSmall,
                        color = ForzaBallPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = "${m.homeClub.shortOrName()} vs ${m.awayClub.shortOrName()}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = teamNextTimeFmt.format(java.util.Date(m.startTimeMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    text = "No upcoming fixture",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val teamNextTimeFmt = java.text.SimpleDateFormat("EEE d MMM · HH:mm", java.util.Locale.getDefault())

private fun Club.shortOrName(): String = name.split(" ").takeLast(2).joinToString(" ").ifBlank { name }

private val scoresRowTimeFmt =
    java.text.SimpleDateFormat("EEE d MMM · HH:mm", java.util.Locale.getDefault())

@Composable
private fun ScoresTabContent(
    scoresViewModel: ScoresViewModel,
    userPreferences: UserPreferences,
) {
    val ui by scoresViewModel.state.collectAsState()
    val hasTeam = !userPreferences.favoriteTeamId.isNullOrBlank()

    SwipeRefreshSharedComponent(
        modifier = Modifier.fillMaxSize(),
        isRefresh = ui.isLoading,
        onRefresh = { scoresViewModel.refresh() },
    ) {
        if (!hasTeam) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Choose your favorite team in your profile to see league standings, European tables when applicable, and fixtures.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ui.errorMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                    item {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item {
                    Text(
                        text = "Standings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                item {
                    val domesticTitle = ui.domesticStanding?.leagueDisplayName
                        ?: catalogLeagues.find { it.id == userPreferences.favoriteTeamLeagueSlug }?.name
                        ?: userPreferences.favoriteTeamLeagueSlug.orEmpty()
                    StandingSnapshotCard(
                        title = domesticTitle.ifBlank { "Domestic league" },
                        snapshot = ui.domesticStanding,
                    )
                }
                if (ui.showUclSection) {
                    item {
                        StandingSnapshotCard(
                            title = "UEFA Champions League",
                            snapshot = ui.uclStanding,
                        )
                    }
                }
                item {
                    Text(
                        text = "Fixtures",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                val fixtures = ui.fixtures.sortedBy { it.startTimeMillis }
                if (fixtures.isEmpty() && !ui.isLoading) {
                    item {
                        Text(
                            text = "No fixtures loaded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(fixtures, key = { it.id }) { m ->
                    ScoresFixtureRow(match = m)
                }
            }
        }
    }
}

@Composable
private fun StandingSnapshotCard(
    title: String,
    snapshot: TeamStandingSnapshot?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ForzaBallPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (snapshot == null) {
                Text(
                    text = "Not listed in the current table.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "#${snapshot.position} · ${snapshot.teamName}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "${snapshot.played} played · ${snapshot.points} pts · " +
                        "W ${snapshot.wins} · D ${snapshot.draws} · L ${snapshot.losses}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                snapshot.recordSummary?.takeIf { it.isNotBlank() }?.let { rec ->
                    Text(
                        text = rec,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                snapshot.goalDifferenceDisplay?.takeIf { it.isNotBlank() }?.let { gd ->
                    Text(
                        text = "GD $gd",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoresFixtureRow(match: Match) {
    val statusText = when {
        match.isLive -> buildString {
            append(match.statusShort ?: "LIVE")
            match.minuteElapsed?.let { append(" · ${it}′") }
        }
        match.isCompleted || match.statusShort?.contains("final", ignoreCase = true) == true ||
            match.statusShort?.equals("FT", ignoreCase = true) == true -> run {
            val hs = match.homeScore?.toString() ?: "–"
            val away = match.awayScore?.toString() ?: "–"
            "$hs – $away · ${match.statusShort ?: "FT"}"
        }
        else -> "${match.homeClub.shortOrName()} vs ${match.awayClub.shortOrName()} · ${match.statusShort ?: "Scheduled"}"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = match.leagueName?.takeIf { it.isNotBlank() } ?: match.homeClub.leagueId,
                    style = MaterialTheme.typography.labelSmall,
                    color = ForzaBallPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = scoresRowTimeFmt.format(java.util.Date(match.startTimeMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${match.homeClub.shortOrName()} vs ${match.awayClub.shortOrName()}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PlaceholderTabContent(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$title — coming soon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileTabContent(
    authState: AuthState,
    userPreferences: UserPreferences,
    onLogout: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onEditFavorites: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (authState) {
            is AuthState.SignedIn -> {
                Text(
                    text = "Signed in",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                authState.email?.takeIf { it.isNotBlank() }?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Favorite league",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val leagueLabel = userPreferences.favoriteTeamLeagueSlug?.let { slug ->
                            catalogLeagues.find { it.id == slug }?.name ?: slug
                        }
                        Text(
                            text = leagueLabel ?: "None selected",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Favorite team",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = when {
                                userPreferences.favoriteTeamId.isNullOrBlank() -> "None selected"
                                else -> {
                                    userPreferences.favoriteTeamName?.takeIf { it.isNotBlank() }
                                        ?: "ESPN team ${userPreferences.favoriteTeamId}"
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onEditFavorites,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Change favorite team", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Log out", fontWeight = FontWeight.Bold)
                }
            }
            AuthState.Loading -> {
                Text(
                    text = "Loading…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AuthState.SignedOut -> {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Sign in to post on the feed and sync your preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Log in", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSignUp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Sign up", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HomeBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        val tabs = listOf(
            Triple(Icons.Default.Home, "Home", 0),
            Triple(Icons.AutoMirrored.Filled.Article, "News", 1),
            Triple(Icons.Default.RssFeed, "Feeds", 2),
            Triple(Icons.Default.SportsSoccer, "Scores", 3),
            Triple(Icons.Default.Person, "Profile", 4),
        )
        tabs.forEach { (icon, label, index) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ForzaBallPrimary,
                    selectedTextColor = ForzaBallPrimary,
                    indicatorColor = ForzaBallPrimary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
fun FavoriteMatchCard(match: Match) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            ForzaBallPrimary.copy(alpha = 0.4f),
                            androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(androidx.compose.ui.graphics.Color.Red),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = liveStatusLabel(match),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = match.leagueName ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = match.homeClub.crestUrl,
                                contentDescription = match.homeClub.name,
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = match.homeClub.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = scoreLine(match),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = match.statusShort ?: "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = match.awayClub.crestUrl,
                                contentDescription = match.awayClub.name,
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = match.awayClub.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ForzaBallPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("View Match Center", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlaceholderFavoriteMatchCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            ForzaBallPrimary.copy(alpha = 0.4f),
                            androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "No live match for your favorite club",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LiveScoreCard(match: Match) {
    Card(
        modifier = Modifier.width(176.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = match.leagueName?.uppercase() ?: "—",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = liveMinuteOrStatus(match),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (match.isLive) androidx.compose.ui.graphics.Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = match.homeClub.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = match.homeScore?.toString() ?: "—",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = match.awayClub.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = match.awayScore?.toString() ?: "—",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun liveMinuteOrStatus(match: Match): String {
    if (match.isLive && match.minuteElapsed != null) {
        return "${match.minuteElapsed}'"
    }
    return match.statusShort ?: ""
}

private fun liveStatusLabel(match: Match): String {
    if (match.isLive) {
        val m = match.minuteElapsed
        return if (m != null) "LIVE — ${m}'" else "LIVE"
    }
    return "Next"
}

private fun scoreLine(match: Match): String {
    val hs = match.homeScore
    val a = match.awayScore
    return if (hs != null && a != null) "$hs – $a" else "vs"
}

@Composable
fun NewsCardLarge(
    article: NewsArticle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                article.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = article.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = article.clubIds.firstOrNull()?.uppercase() ?: "NEWS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = ForzaBallPrimary,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
                            .background(
                                ForzaBallPrimary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp),
                            )
                            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    )
                    Text(
                        text = "Recently",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun NewsCardHorizontal(
    article: NewsArticle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                article.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = article.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = article.clubIds.firstOrNull()?.uppercase() ?: "NEWS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = ForzaBallPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recently",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun homePreviewSampleState(): HomeState {
    val arsenal = Club(
        id = "57",
        name = "Arsenal",
        leagueId = "pl",
        crestUrl = "https://crests.football-data.org/57.png",
    )
    val chelsea = Club(
        id = "61",
        name = "Chelsea",
        leagueId = "pl",
        crestUrl = "https://crests.football-data.org/61.png",
    )
    val liverpool = Club(
        id = "64",
        name = "Liverpool",
        leagueId = "pl",
        crestUrl = "https://crests.football-data.org/64.png",
    )
    val city = Club(
        id = "65",
        name = "Man City",
        leagueId = "pl",
        crestUrl = "https://crests.football-data.org/65.png",
    )
    val highlight = Match(
        id = "preview-1",
        homeClub = arsenal,
        awayClub = chelsea,
        startTimeMillis = 0L,
        isLive = true,
        homeScore = 2,
        awayScore = 1,
        statusShort = "2H",
        minuteElapsed = 67,
        leagueName = "Premier League",
    )
    val liveRow = Match(
        id = "preview-2",
        homeClub = liverpool,
        awayClub = city,
        startTimeMillis = 0L,
        isLive = true,
        homeScore = 1,
        awayScore = 1,
        statusShort = "1H",
        minuteElapsed = 34,
        leagueName = "PL",
    )
    val nextPreview = Match(
        id = "preview-next",
        homeClub = arsenal,
        awayClub = liverpool,
        startTimeMillis = System.currentTimeMillis() + 86_400_000L,
        isLive = false,
        leagueName = "Premier League",
    )
    return HomeState(
        favoriteClubMatch = highlight,
        liveMatches = listOf(highlight, liveRow),
        favoriteTeamNextMatch = TeamNextMatch(
            teamId = "359",
            teamDisplayName = "Arsenal",
            teamCrestUrl = arsenal.crestUrl,
            nextMatch = nextPreview,
        ),
        userPreferences = UserPreferences(
            countryCode = null,
            favoriteTeamLeagueSlug = "eng.1",
            favoriteTeamId = "359",
            favoriteTeamName = "Arsenal",
        ),
        news = listOf(
            NewsArticle(
                id = "n1",
                title = "Title race heats up after dramatic weekend",
                summary = "Key results reshape the table as the season enters its final stretch.",
                imageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800&q=80",
                publishedAtMillis = 0L,
                leagueId = "pl",
                clubIds = listOf("premier league"),
                articleUrl = null,
            ),
            NewsArticle(
                id = "n2",
                title = "Injury update ahead of European fixtures",
                summary = "Managers face selection headaches with several first-team players sidelined.",
                imageUrl = "https://images.unsplash.com/photo-1431324155629-1a6deb1dec8d?w=400&q=80",
                publishedAtMillis = 0L,
                leagueId = "pl",
                clubIds = listOf("news"),
                articleUrl = null,
            ),
        ),
    )
}

@Preview(name = "Home — Dashboard", showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenDashboardPreview() {
    ForzaBallTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeTopBar(title = homeTitleForTab(0))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    HomeDashboardContent(
                        state = homePreviewSampleState(),
                        onViewAllFixtures = {},
                        onViewAllNews = {},
                        onOpenNewsArticle = { _, _ -> },
                        onRefresh = {},
                    )
                }
                HomeBottomNavigation(
                    selectedTab = 0,
                    onTabSelected = {},
                )
            }
        }
    }
}
