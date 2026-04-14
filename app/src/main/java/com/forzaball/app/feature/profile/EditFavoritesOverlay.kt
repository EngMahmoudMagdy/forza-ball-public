package com.forzaball.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.forzaball.app.feature.personalization.PersonalizationStep1Screen
import com.forzaball.app.feature.personalization.PersonalizationStep2Screen

@Composable
fun EditFavoritesOverlay(
    visible: Boolean,
    viewModel: EditFavoritesViewModel,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val state by viewModel.state.collectAsState()

    LaunchedEffect(visible) {
        if (visible) viewModel.resetFromStorage()
    }

    LaunchedEffect(state.closed) {
        if (state.closed) {
            viewModel.clearClosed()
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.step) {
                1 -> PersonalizationStep1Screen(
                    selectedLeagueIds = state.selectedLeagueIds,
                    onToggleLeague = viewModel::toggleLeague,
                    onBack = onDismiss,
                    onNext = viewModel::nextStep,
                    isLoadingTeams = state.isLoadingTeams,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> PersonalizationStep2Screen(
                    selectedLeagueIds = state.selectedLeagueIds,
                    selectedClubIds = state.selectedClubIds,
                    teamsByLeague = state.teamsByLeague,
                    onToggleClub = viewModel::toggleClub,
                    onBack = viewModel::previousStep,
                    onNext = { if (!state.isSaving) viewModel.save() },
                    primaryActionLabel = if (state.isSaving) "Saving…" else "Save",
                    primaryActionEnabled = !state.isSaving,
                    step2OfTotal = 2,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
