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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.forzaball.app.feature.profile.ProfileViewModel
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.repository.AuthState
import com.forzaball.app.feature.feeds.CreatePostOverlay
import com.forzaball.app.feature.feeds.FeedPostDetailRoute
import com.forzaball.app.feature.feeds.FeedViewModel
import com.forzaball.app.feature.feeds.FeedsRoute
import com.forzaball.app.ui.theme.ForzaBallPrimary
import org.koin.androidx.compose.koinViewModel

private const val LOGO_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCkAL2u94GltH0gb0jToC4o5aOD9YxRPw9NwKb1flAI-bCwrMMoOkHeV26eLvKLGFYBmnedNJCHIH2iaP2-YM6Jks4PM85oZZ-psd7wrCudFSy8o1zkCok8ETtOnWEb-PVq-jeayk33qCePTvmz8sOm_5_UiZZgBAQnSUD25LexPnQJhFN4saDpwL8f_27y4z6jELXkmQ-K6gx5cMcrEYXCF1e9LAn6GQQulXGX272kE055COHIQYvXBVy1y_xDcXIl67bEce_ztKId"

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = koinViewModel(),
    feedViewModel: FeedViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    initialFeedPostId: String? = null,
    /** Opens a post from the global in-app banner (root overlay). */
    bannerOpenPostId: String? = null,
    onBannerOpenConsumed: () -> Unit = {},
    onNavigateToSignIn: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.processIntent(HomeIntent.Load)
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var feedOverlayKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(initialFeedPostId) {
        if (initialFeedPostId != null) {
            selectedTab = 2
            feedOverlayKey = "post:$initialFeedPostId"
        }
    }

    LaunchedEffect(bannerOpenPostId) {
        val id = bannerOpenPostId ?: return@LaunchedEffect
        selectedTab = 2
        feedOverlayKey = "post:$id"
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
    modifier: Modifier = Modifier,
) {
    val feedUi by feedViewModel.ui.collectAsState()
    val authState by feedViewModel.authState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(
                title = homeTitleForTab(selectedTab),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (selectedTab) {
                    0 -> HomeDashboardContent(state = state)
                    2 -> FeedsRoute(
                        viewModel = feedViewModel,
                        onOpenCreatePost = { onFeedOverlayKeyChange("create") },
                        onOpenPost = { onFeedOverlayKeyChange("post:$it") },
                    )
                    4 -> ProfileTabContent(
                        authState = authState,
                        onLogout = onLogout,
                        onSignIn = onNavigateToSignIn,
                        onSignUp = onNavigateToSignUp,
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
                    val postId = key.removePrefix("post:")
                    FeedPostDetailRoute(
                        postId = postId,
                        viewModel = feedViewModel,
                        onBack = { onFeedOverlayKeyChange(null) },
                    )
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
private fun HomeTopBar(title: String) {
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
                onClick = { },
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
private fun HomeDashboardContent(state: HomeState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
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
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .background(
                                ForzaBallPrimary.copy(alpha = 0.1f),
                                RoundedCornerShape(9999.dp),
                            )
                            .border(
                                1.dp,
                                ForzaBallPrimary.copy(alpha = 0.2f),
                                RoundedCornerShape(9999.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                state.favoriteClubMatch?.let { match ->
                    FavoriteMatchCard(match = match)
                } ?: run {
                    PlaceholderFavoriteMatchCard()
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                items(state.liveMatches) { match ->
                    LiveScoreCard(match = match)
                }
            }
        }

        item {
            Text(
                text = "Latest News for You",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 24.dp,
                    bottom = 16.dp,
                ),
            )
        }
        items(state.news) { article ->
            if (state.news.indexOf(article) == 0) {
                NewsCardLarge(article = article)
            } else {
                NewsCardHorizontal(article = article)
            }
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
    onLogout: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
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
fun NewsCardLarge(article: NewsArticle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .background(
                                ForzaBallPrimary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
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
fun NewsCardHorizontal(article: NewsArticle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
