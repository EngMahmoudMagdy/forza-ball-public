package com.forzaball.feature.feeds

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.forzaball.R
import com.forzaball.feature.profile.ReportReasonCatalog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.forzaball.domain.model.FeedContentLimits
import com.forzaball.domain.repository.FeedComment
import com.forzaball.domain.repository.FeedPost
import com.forzaball.ui.theme.ForzaBallPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val AVATAR_PLACEHOLDER = "https://i.pravatar.cc/150?u="

/** After comments exist in state, wait for the LazyColumn to compose them, then pause so the list is visible before scrolling to the target row. */
private const val DELAY_MS_COMMENTS_VISIBLE_BEFORE_SCROLL = 600L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPostDetailRoute(
    postId: String,
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    highlightCommentId: String? = null,
    currentUserId: String? = null,
    onOpenUserProfile: (String) -> Unit = {},
    onPostDeleted: () -> Unit = {},
) {
    var post by remember { mutableStateOf<FeedPost?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(postId) {
        viewModel.observePost(postId).collect { p ->
            post = p
            loading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ForzaBallPrimary)
                }
            }
            post == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Post not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Back",
                        color = ForzaBallPrimary,
                        modifier = Modifier.clickable { onBack() },
                    )
                }
            }
            else -> {
                FeedPostDetailContent(
                    post = post!!,
                    viewModel = viewModel,
                    onBack = onBack,
                    highlightCommentId = highlightCommentId,
                    currentUserId = currentUserId,
                    onOpenUserProfile = onOpenUserProfile,
                    onPostDeleted = onPostDeleted,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedPostDetailContent(
    post: FeedPost,
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    highlightCommentId: String?,
    currentUserId: String?,
    onOpenUserProfile: (String) -> Unit,
    onPostDeleted: () -> Unit,
) {
    val context = LocalContext.current
    var showOptions by remember(post.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(post.id) { mutableStateOf(false) }
    var showReport by remember(post.id) { mutableStateOf(false) }
    var draft by rememberSaveable(post.id) { mutableStateOf("") }
    var comments by remember { mutableStateOf<List<FeedComment>>(emptyList()) }
    val listState = rememberLazyListState()
    /** Which comment row receives the animated highlight (notification or newly posted). */
    var highlightSubjectId by remember(post.id) { mutableStateOf<String?>(null) }
    /** Drives [highlightProgress]: 0 = rest, 1 = emphasized (animated). */
    var highlightTarget by remember(post.id) { mutableStateOf(0f) }
    val highlightProgress by animateFloatAsState(
        targetValue = highlightTarget,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "commentHighlight",
    )
    /** Just posted by this user — scroll + same highlight animation. */
    var pendingLocalFlashId by remember(post.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(post.id) {
        viewModel.observeComments(post.id).collect { comments = it }
    }

    // Do NOT use `comments` as a LaunchedEffect key — Firestore emits often and would cancel this coroutine
    // before scroll/animation finishes. Wait for the target id inside the effect with snapshotFlow instead.
    LaunchedEffect(highlightCommentId) {
        val hid = highlightCommentId ?: return@LaunchedEffect
        val listWhenReady = withTimeoutOrNull(20_000) {
            snapshotFlow { comments }.first { list -> list.any { it.id == hid } }
        } ?: return@LaunchedEffect
        val idx = listWhenReady.indexOfFirst { it.id == hid }
        if (idx < 0) return@LaunchedEffect

        val itemIndex = 1 + idx
        waitForCommentsListVisibleThenPause(listState, commentsSize = listWhenReady.size)
        scrollLazyListToComment(listState, itemIndex)
    }

    LaunchedEffect(pendingLocalFlashId) {
        val id = pendingLocalFlashId ?: return@LaunchedEffect
        val listWhenReady = withTimeoutOrNull(20_000) {
            snapshotFlow { comments }.first { list -> list.any { it.id == id } }
        } ?: return@LaunchedEffect
        val idx = listWhenReady.indexOfFirst { it.id == id }
        if (idx < 0) return@LaunchedEffect

        val itemIndex = 1 + idx
        waitForCommentsListVisibleThenPause(listState, commentsSize = listWhenReady.size)
        scrollLazyListToComment(listState, itemIndex)
        yield()
        delay(80)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    PostDetailHeader(
                        post = post,
                        onAuthorClick = { onOpenUserProfile(post.userId) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = post.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.toggleLike(post) },
                        ) {
                            Icon(
                                imageVector = if (post.isLikedByUser) Icons.Default.ThumbUp else Icons.Default.ThumbUp,
                                contentDescription = "Like",
                                tint = if (post.isLikedByUser) ForzaBallPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            modifier = Modifier.clickable { viewModel.toggleDislike(post) },
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        IconButton(
                            onClick = {
                                sharePost(context, post.id)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatPostDetailTimestamp(post.createdAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                items(
                    items = comments,
                    key = { it.id },
                ) { c ->
                    val canDelete = currentUserId != null &&
                        (c.userId == currentUserId || post.userId == currentUserId)
                    val overlayAlpha =
                        if (highlightSubjectId != null && c.id == highlightSubjectId) {
                            highlightProgress * 0.22f
                        } else {
                            0f
                        }
                    CommentRow(
                        comment = c,
                        onToggleLike = {
                            viewModel.toggleCommentLike(post.id, c.id, c.isLikedByUser)
                        },
                        onToggleDislike = {
                            viewModel.toggleCommentDislike(post.id, c.id, c.isDislikedByUser)
                        },
                        canDelete = canDelete,
                        onDelete = { viewModel.deleteComment(post.id, c.id) },
                        onAuthorClick = { onOpenUserProfile(c.userId) },
                        highlightOverlayAlpha = overlayAlpha,
                    )
                }
            }
            val commentValidationError = FeedContentLimits.validateComment(draft)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                FeedCommentTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(R.string.add_comment_placeholder),
                    maxLines = 3,
                )
                Spacer(modifier = Modifier.width(8.dp))
                CommentSendIconButton(
                    enabled = commentValidationError == null,
                    onClick = {
                        viewModel.addComment(post.id, draft) { r ->
                            r.onSuccess { newId ->
                                draft = ""
                                pendingLocalFlashId = newId
                            }
                        }
                    },
                )
            }
        }
    }

    if (showOptions) {
        PostOptionsBottomSheet(
            post = post,
            isOwnPost = post.userId == currentUserId,
            onDismiss = { showOptions = false },
            onSaveToggle = { viewModel.toggleSavePost(post) },
            onDelete = {
                showOptions = false
                showDeleteConfirm = true
            },
            onReport = {
                showOptions = false
                showReport = true
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_post)) },
            text = { Text(stringResource(R.string.confirm_delete_post)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost(post.id, onPostDeleted)
                        showDeleteConfirm = false
                    },
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showReport) {
        ReportPostDialog(
            onDismiss = { showReport = false },
            onConfirm = { reasonId, _, comment ->
                val reason = ReportReasonCatalog.findById(reasonId) ?: return@ReportPostDialog
                val label = context.getString(reason.labelRes)
                viewModel.reportPost(post.id, reasonId, label, comment) {
                    showReport = false
                }
            },
        )
    }
}

@Composable
private fun PostDetailHeader(
    post: FeedPost,
    onAuthorClick: () -> Unit = {},
) {
    val avatarUrl = post.authorAvatarUrl ?: "$AVATAR_PLACEHOLDER${post.userId}"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onAuthorClick),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAuthorClick),
        ) {
            Text(
                text = post.authorName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "@${post.authorUsername}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFeedTimeRelative(post.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Waits until [LazyListState.layoutInfo] reflects all comment rows (header + comments),
 * then waits [DELAY_MS_COMMENTS_VISIBLE_BEFORE_SCROLL] so the user can see the list before scrolling to the highlight.
 */
private suspend fun waitForCommentsListVisibleThenPause(
    listState: LazyListState,
    commentsSize: Int,
) {
    val expectedTotalItems = 1 + commentsSize
    var n = 0
    while (n < 150 && listState.layoutInfo.totalItemsCount < expectedTotalItems) {
        delay(16)
        n++
    }
    delay(DELAY_MS_COMMENTS_VISIBLE_BEFORE_SCROLL)
}

private suspend fun scrollLazyListToComment(listState: LazyListState, itemIndex: Int) {
    val needCount = itemIndex + 1
    var n = 0
    while (n < 120 && listState.layoutInfo.totalItemsCount < needCount) {
        delay(16)
        n++
    }
    delay(48)
    listState.scrollToItem(itemIndex)
    delay(100)
    listState.scrollToItem(itemIndex)
    // Ensure the row is actually laid out in the viewport when possible
    var v = 0
    while (v < 50 && listState.layoutInfo.visibleItemsInfo.none { it.index == itemIndex }) {
        delay(24)
        listState.scrollToItem(itemIndex)
        v++
    }
}

internal fun sharePost(context: android.content.Context, postId: String) {
    val https = "https://forzaball.app/post/$postId"
    val appLink = "forzaball://post/$postId"
    val text = "Check this post on ForzaBall\n$https\n$appLink"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, null))
}

internal fun formatPostDetailTimestamp(millis: Long): String {
    if (millis <= 0L) return ""
    val fmt = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
}

internal fun formatFeedTimeRelative(millis: Long): String {
    if (millis <= 0L) return ""
    val diff = System.currentTimeMillis() - millis
    val s = diff / 1000
    val m = s / 60
    val h = m / 60
    val d = h / 24
    return when {
        s < 60 -> "just now"
        m < 60 -> "${m}m ago"
        h < 24 -> "${h}h ago"
        d < 7 -> "${d}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
    }
}
