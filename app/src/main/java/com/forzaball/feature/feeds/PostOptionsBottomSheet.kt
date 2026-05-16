package com.forzaball.feature.feeds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.forzaball.R
import com.forzaball.domain.repository.FeedPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: FeedPost,
    isOwnPost: Boolean,
    onDismiss: () -> Unit,
    onSaveToggle: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.post_options),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            PostOptionRow(
                icon = if (post.isSavedByUser) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = stringResource(if (post.isSavedByUser) R.string.unsave_post else R.string.save_post),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    onSaveToggle()
                    onDismiss()
                },
            )
            if (isOwnPost) {
                PostOptionRow(
                    icon = Icons.Default.Delete,
                    label = stringResource(R.string.delete_post),
                    tint = Color(0xFFD32F2F),
                    onClick = onDelete,
                )
            } else {
                PostOptionRow(
                    icon = Icons.Default.Report,
                    label = stringResource(R.string.report_post),
                    tint = Color(0xFFD32F2F),
                    onClick = onReport,
                )
            }
        }
    }
}

@Composable
private fun PostOptionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            modifier = Modifier.padding(start = 16.dp),
            color = tint,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
