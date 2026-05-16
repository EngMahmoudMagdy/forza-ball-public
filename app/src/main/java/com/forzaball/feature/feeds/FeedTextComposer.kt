package com.forzaball.feature.feeds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.forzaball.R
import com.forzaball.domain.model.FeedContentLimits

@Composable
fun FeedPostTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 6,
) {
    val overLimit = value.length > FeedContentLimits.MAX_POST_CHARS
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= FeedContentLimits.MAX_POST_CHARS) onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            minLines = minLines,
            isError = overLimit,
        )
        CharacterCountLabel(
            current = value.length,
            max = FeedContentLimits.MAX_POST_CHARS,
            overLimit = overLimit,
        )
    }
}

@Composable
fun FeedCommentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLines: Int = 3,
) {
    val overLimit = value.length > FeedContentLimits.MAX_COMMENT_CHARS
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= FeedContentLimits.MAX_COMMENT_CHARS) onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            maxLines = maxLines,
            isError = overLimit,
        )
        CharacterCountLabel(
            current = value.length,
            max = FeedContentLimits.MAX_COMMENT_CHARS,
            overLimit = overLimit,
        )
    }
}

@Composable
private fun CharacterCountLabel(
    current: Int,
    max: Int,
    overLimit: Boolean,
) {
    Text(
        text = stringResource(R.string.character_count, current, max),
        style = MaterialTheme.typography.labelSmall,
        color = if (overLimit) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = TextAlign.End,
    )
}
