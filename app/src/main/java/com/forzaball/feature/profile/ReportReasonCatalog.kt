package com.forzaball.feature.profile

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.forzaball.R

data class ReportReasonOption(
    val id: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

object ReportReasonCatalog {
    val topFive: List<ReportReasonOption> = listOf(
        ReportReasonOption("spam", R.string.report_reason_spam, Icons.Default.Report),
        ReportReasonOption("harassment", R.string.report_reason_harassment, Icons.Default.SentimentVeryDissatisfied),
        ReportReasonOption("hate", R.string.report_reason_hate, Icons.Default.GppBad),
        ReportReasonOption("violence", R.string.report_reason_violence, Icons.Default.Warning),
        ReportReasonOption("misinformation", R.string.report_reason_misinformation, Icons.Default.Block),
    )

    fun findById(id: String): ReportReasonOption? = topFive.find { it.id == id }
}
