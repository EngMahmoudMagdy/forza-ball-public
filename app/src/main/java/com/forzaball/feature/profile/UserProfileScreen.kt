package com.forzaball.feature.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.forzaball.R
import com.forzaball.feature.feeds.FeedPostCard
import com.forzaball.ui.components.ClickableProfileAvatar
import com.forzaball.ui.components.FullscreenImageDialog
import com.forzaball.ui.theme.ForzaBallOnSurface
import com.forzaball.ui.theme.ForzaBallOnSurfaceVariant
import com.forzaball.ui.theme.ForzaBallPrimary
import com.forzaball.ui.theme.ForzaBallSurfaceContainer
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileRoute(
    userId: String,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    viewModel: UserProfileViewModel = koinViewModel(),
) {
    LaunchedEffect(userId) { viewModel.load(userId) }
    val ui by viewModel.ui.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var fullscreenPhoto by remember { mutableStateOf<String?>(null) }
    val profile = ui.profile

    fullscreenPhoto?.let { url ->
        FullscreenImageDialog(imageUrl = url, onDismiss = { fullscreenPhoto = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.profile_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (ui.isLoading && profile == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ForzaBallPrimary)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ProfileHeaderSection(
                displayName = profile?.displayName.orEmpty(),
                handle = profile?.handle.orEmpty(),
                photoUrl = profile?.avatarUrl,
                thumbUrl = profile?.avatarThumbUrl,
                cacheVersion = if (ui.isOwnProfile) ui.localPhotoCacheVersion else 0L,
                fallbackUserId = userId,
                isOwnProfile = ui.isOwnProfile,
                onEditProfile = onEditProfile,
                onPhotoClick = { profile?.avatarUrl?.let { fullscreenPhoto = it } },
            )
            if (ui.isOwnProfile) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = ForzaBallPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ForzaBallPrimary,
                        )
                    },
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                stringResource(R.string.my_posts),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                stringResource(R.string.saved_posts),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                    )
                }
            }
            val posts = if (ui.isOwnProfile && selectedTab == 1) ui.savedPosts else ui.posts
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (ui.isOwnProfile && selectedTab == 1) R.string.no_saved_posts else R.string.no_posts_yet,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForzaBallOnSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(posts, key = { it.id }) { post ->
                        FeedPostCard(
                            post = post,
                            onLike = {},
                            onDislike = {},
                            onOpenPost = { onOpenPost(post.id) },
                            onShare = {},
                            onComment = { onOpenPost(post.id) },
                            onAuthorClick = { onOpenUserProfile(post.userId) },
                            onMoreClick = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    displayName: String,
    handle: String,
    photoUrl: String?,
    thumbUrl: String?,
    cacheVersion: Long,
    fallbackUserId: String,
    isOwnProfile: Boolean,
    onEditProfile: () -> Unit,
    onPhotoClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ForzaBallPrimary, ForzaBallSurfaceContainer),
                        ),
                    )
                    .padding(4.dp),
            ) {
                ClickableProfileAvatar(
                    photoUrl = photoUrl,
                    thumbUrl = thumbUrl,
                    cacheVersion = cacheVersion,
                    fallbackUserId = fallbackUserId,
                    size = 120.dp,
                    onClick = onPhotoClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                )
            }
            if (isOwnProfile) {
                IconButton(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ForzaBallPrimary)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_profile),
                        tint = ForzaBallOnSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            ),
            color = ForzaBallOnSurface,
            modifier = Modifier.clickable(onClick = onPhotoClick),
        )
        if (handle.isNotBlank()) {
            Text(
                text = "@$handle",
                style = MaterialTheme.typography.bodyMedium,
                color = ForzaBallOnSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (isOwnProfile) {
            Spacer(modifier = Modifier.size(20.dp))
            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(R.string.edit_profile),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black,
                    ),
                )
            }
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}
