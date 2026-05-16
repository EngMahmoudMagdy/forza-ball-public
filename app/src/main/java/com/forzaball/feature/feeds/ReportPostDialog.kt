package com.forzaball.feature.feeds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.forzaball.R
import com.forzaball.feature.profile.ReportReasonCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportPostDialog(
    onDismiss: () -> Unit,
    onConfirm: (reasonId: String, reasonLabel: String, comment: String?) -> Unit,
) {
    var selectedReasonId by rememberSaveable { mutableStateOf<String?>(null) }
    var comment by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_report_post)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReportReasonCatalog.topFive.forEach { reason ->
                        FilterChip(
                            selected = selectedReasonId == reason.id,
                            onClick = { selectedReasonId = reason.id },
                            label = { Text(stringResource(reason.labelRes)) },
                            leadingIcon = {
                                Icon(
                                    reason.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.report_optional_comment)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reason = ReportReasonCatalog.findById(selectedReasonId ?: return@TextButton) ?: return@TextButton
                    onConfirm(
                        reason.id,
                        "",
                        comment.trim().takeIf { it.isNotEmpty() },
                    )
                },
                enabled = selectedReasonId != null,
            ) {
                Text(stringResource(R.string.report), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
