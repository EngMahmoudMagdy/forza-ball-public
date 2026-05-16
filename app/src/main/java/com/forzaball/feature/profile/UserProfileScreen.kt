package com.forzaball.feature.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.forzaball.R
import com.forzaball.feature.feeds.FeedPostCard
import com.forzaball.ui.components.ClickableProfileAvatar
import com.forzaball.ui.components.FullscreenImageDialog
import com.forzaball.ui.theme.ForzaBallPrimary
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
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    ClickableProfileAvatar(
                        photoUrl = profile?.avatarUrl,
                        thumbUrl = profile?.avatarThumbUrl,
                        cacheVersion = if (ui.isOwnProfile) ui.localPhotoCacheVersion else 0L,
                        fallbackUserId = userId,
                        size = 72.dp,
                        onClick = {
                            profile?.avatarUrl?.let { fullscreenPhoto = it }
                        },
                    )
                    if (ui.isOwnProfile) {
                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = ForzaBallPrimary)
                        }
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile?.displayName.orEmpty(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable {
                                    profile?.avatarUrl?.let { fullscreenPhoto = it }
                                },
                        )
                        if (ui.isOwnProfile) {
                            TextButton(onClick = onEditProfile) {
                                Text(stringResource(R.string.edit))
                            }
                        }
                    }
                    Text(
                        text = "@${profile?.handle.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (ui.isOwnProfile) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.my_posts)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.saved_posts)) },
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
