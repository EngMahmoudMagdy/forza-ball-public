package com.forzaball.app.feature.feeds

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.forzaball.app.core.shared_ui_components.SwipeRefreshSharedComponent
import com.forzaball.app.ui.theme.ForzaBallPrimary
import com.forzaball.domain.repository.AuthState
import com.forzaball.domain.repository.FeedComment
import com.forzaball.domain.repository.FeedPost
import org.koin.androidx.compose.koinViewModel
import java.util.Date
import java.util.Locale

private const val AVATAR_PLACEHOLDER = "https://i.pravatar.cc/150?u="

@Composable
fun FeedsRoute(
    viewModel: FeedViewModel = koinViewModel(),
    onOpenCreatePost: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
) {
    val ui by viewModel.ui.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var commentPostId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(ui.errorMessage) {
        val msg = ui.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeError()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (authState is AuthState.SignedIn) {
                    ExtendedFloatingActionButton(
                        onClick = onOpenCreatePost,
                        expanded = true,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                            )
                        },
                        text = { Text("New post") },
                        containerColor = ForzaBallPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                        ),
                    )
                }
            },
        ) { padding ->
            FeedsScreen(
                modifier = Modifier.padding(padding),
                ui = ui,
                authState = authState,
                onRefresh = viewModel::refresh,
                onLike = viewModel::toggleLike,
                onDislike = viewModel::toggleDislike,
                onOpenPost = onOpenPost,
                onSharePost = { sharePost(context, it) },
                onOpenCreatePost = onOpenCreatePost,
                onOpenComments = { commentPostId = it },
            )
        }

        commentPostId?.let { postId ->
            val uid = (authState as? AuthState.SignedIn)?.uid
            FeedCommentsBottomSheet(
                postId = postId,
                currentUserId = uid,
                viewModel = viewModel,
                onDismiss = { commentPostId = null },
            )
        }
    }
}

@Composable
private fun FeedsScreen(
    modifier: Modifier = Modifier,
    ui: FeedUiState,
    authState: AuthState,
    onRefresh: () -> Unit,
    onLike: (FeedPost) -> Unit,
    onDislike: (FeedPost) -> Unit,
    onOpenPost: (String) -> Unit,
    onSharePost: (String) -> Unit,
    onOpenCreatePost: () -> Unit,
    onOpenComments: (String) -> Unit,
) {
    val signedIn = authState is AuthState.SignedIn

    SwipeRefreshSharedComponent(
        isRefresh = ui.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            authState is AuthState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ForzaBallPrimary)
                }
            }
            !signedIn -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Community feed",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sign in to see posts and share your take on football.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            ui.isInitialLoading -> {
                FeedLoadingPlaceholder()
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        PostUpdatePromptCard(onClick = onOpenCreatePost)
                    }
                    if (ui.posts.isEmpty()) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RssFeed,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = ForzaBallPrimary.copy(alpha = 0.7f),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No posts yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Follow people or create the first post — your feed updates in real time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(
                            items = ui.posts,
                            key = { it.id },
                        ) { post ->
                            FeedPostCard(
                                post = post,
                                onLike = { onLike(post) },
                                onDislike = { onDislike(post) },
                                onOpenPost = { onOpenPost(post.id) },
                                onShare = { onSharePost(post.id) },
                                onComment = { onOpenComments(post.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedLoadingPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = ForzaBallPrimary,
                        strokeWidth = 3.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PostUpdatePromptCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = ForzaBallPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.AlternateEmail,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Post Update",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun FeedPostCard(
    post: FeedPost,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onOpenPost: () -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(post.id) { mutableStateOf(false) }
    val avatarUrl = post.authorAvatarUrl ?: "$AVATAR_PLACEHOLDER${post.userId}"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPost),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${post.authorUsername}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatFeedTimeRelativeUppercase(post.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    )
                }
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { expanded = !expanded },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        onClick = onLike,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (post.isLikedByUser) {
                            ForzaBallPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatCompactCount(post.likeCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onDislike),
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (post.isDislikedByUser) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatCompactCount(post.dislikeCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        onClick = onComment,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatCompactCount(post.commentCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatePostOverlay(
    isPosting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val canPost = text.isNotBlank() && text.length <= 500 && !isPosting

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Create post", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { if (canPost) onSubmit(text.trim()) },
                            enabled = canPost,
                        ) {
                            if (isPosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Post", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize(),
            ) {
                Text(
                    text = "What’s on your mind about football?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Write something…") },
                    minLines = 6,
                )
                Text(
                    text = "${text.length}/500",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedCommentsBottomSheet(
    postId: String,
    currentUserId: String?,
    viewModel: FeedViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var comments by remember { mutableStateOf<List<FeedComment>>(emptyList()) }
    var post by remember { mutableStateOf<FeedPost?>(null) }
    var draft by rememberSaveable(postId) { mutableStateOf("") }

    LaunchedEffect(postId) {
        viewModel.observeComments(postId).collect { comments = it }
    }
    LaunchedEffect(postId) {
        viewModel.observePost(postId).collect { post = it }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Single LazyColumn: ModalBottomSheet measures nested scrollables poorly; a Column + LazyColumn
        // often collapses the list to zero height.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    )
                }
            }
            item {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            items(comments, key = { it.id }) { c ->
                val postAuthorId = post?.userId
                val canDelete = currentUserId != null &&
                    (c.userId == currentUserId || postAuthorId == currentUserId)
                CommentRow(
                    comment = c,
                    onToggleLike = {
                        viewModel.toggleCommentLike(postId, c.id, c.isLikedByUser)
                    },
                    onToggleDislike = {
                        viewModel.toggleCommentDislike(postId, c.id, c.isDislikedByUser)
                    },
                    canDelete = canDelete,
                    onDelete = { viewModel.deleteComment(postId, c.id) },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 200) draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a comment…") },
                        maxLines = 3,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CommentSendIconButton(
                        enabled = draft.isNotBlank(),
                        onClick = {
                            val t = draft.trim()
                            if (t.isEmpty()) return@CommentSendIconButton
                            viewModel.addComment(postId, t) { r ->
                                r.onSuccess { draft = "" }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun CommentSendIconButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            modifier = Modifier.size(28.dp),
            tint = if (enabled) ForzaBallPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentRow(
    comment: FeedComment,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    canDelete: Boolean = false,
    onDelete: () -> Unit = {},
    /** 0 = normal; animated overlay alpha for notification / new-comment highlight. */
    highlightOverlayAlpha: Float = 0f,
) {
    val avatar = comment.authorAvatarUrl ?: "$AVATAR_PLACEHOLDER${comment.userId}"
    var showDeleteSheet by remember(comment.id) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = if (canDelete) {
                        { showDeleteSheet = true }
                    } else {
                        null
                    },
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = comment.authorName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = formatFeedTime(comment.createdAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onToggleLike() },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Like",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (comment.isLikedByUser) {
                                        ForzaBallPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatCompactCount(comment.likeCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onToggleDislike() },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbDown,
                                    contentDescription = "Dislike",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (comment.isDislikedByUser) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatCompactCount(comment.dislikeCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (highlightOverlayAlpha > 0.001f) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                ForzaBallPrimary.copy(alpha = highlightOverlayAlpha.coerceIn(0f, 1f)),
                                RoundedCornerShape(12.dp),
                            ),
                    )
                }
            }
        }

        if (showDeleteSheet && canDelete) {
            ModalBottomSheet(
                onDismissRequest = { showDeleteSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                ) {
                    Text(
                        text = "Comment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Delete this comment?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This can’t be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            onDelete()
                            showDeleteSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete comment", fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

internal fun formatCompactCount(n: Int): String {
    if (n < 0) return "0"
    if (n < 1000) return n.toString()
    if (n < 1_000_000) {
        val v = n / 1000.0
        val s = String.format(Locale.US, "%.1f", v).trimEnd('0').trimEnd('.')
        return "${s}K"
    }
    val v = n / 1_000_000.0
    val s = String.format(Locale.US, "%.1f", v).trimEnd('0').trimEnd('.')
    return "${s}M"
}

internal fun formatFeedTimeRelativeUppercase(millis: Long): String {
    if (millis <= 0L) return ""
    val diff = System.currentTimeMillis() - millis
    val s = diff / 1000
    val m = s / 60
    val h = m / 60
    val d = h / 24
    return when {
        s < 60 -> "JUST NOW"
        m < 60 -> if (m == 1L) "1 MINUTE AGO" else "$m MINUTES AGO"
        h < 24 -> if (h == 1L) "1 HOUR AGO" else "$h HOURS AGO"
        d < 7 -> if (d == 1L) "1 DAY AGO" else "$d DAYS AGO"
        else -> java.text.SimpleDateFormat("MMM d", Locale.getDefault())
            .format(Date(millis)).uppercase(Locale.getDefault())
    }
}

internal fun formatFeedTime(millis: Long): String {
    if (millis <= 0L) return ""
    val diff = System.currentTimeMillis() - millis
    val s = diff / 1000
    val m = s / 60
    val h = m / 60
    val d = h / 24
    return when {
        s < 60 -> "Just now"
        m < 60 -> "${m}m"
        h < 24 -> "${h}h"
        d < 7 -> "${d}d"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            .format(java.util.Date(millis))
    }
}
