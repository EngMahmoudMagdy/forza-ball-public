@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.forzaball.feature.search

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forzaball.feature.personalization.LeagueItem
import com.forzaball.ui.theme.ForzaBallPrimary
import com.forzaball.domain.model.Club
import com.forzaball.domain.model.TeamSearchHistoryEntry
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenTeamProfile: (leagueSlug: String, teamId: String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    SearchScreen(
        ui = ui,
        onBack = onBack,
        onLeagueQuery = viewModel::onLeagueQueryChange,
        onTeamQuery = viewModel::onTeamQueryChange,
        onSelectLeague = viewModel::selectLeague,
        onClearLeague = viewModel::clearLeague,
        filteredLeagues = viewModel.filteredLeagues(),
        filteredTeams = viewModel.filteredTeams(),
        onLeagueLayout = viewModel::setLeagueLayout,
        onTeamLayout = viewModel::setTeamLayout,
        onSelectTeam = { club, league ->
            viewModel.recordSearchAndGetEntry(club, league)
            onOpenTeamProfile(league.id, club.id)
        },
        onHistoryClick = { entry -> onOpenTeamProfile(entry.leagueSlug, entry.teamId) },
        onRemoveHistory = viewModel::removeHistoryEntry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    ui: SearchUiState,
    onBack: () -> Unit,
    onLeagueQuery: (String) -> Unit,
    onTeamQuery: (String) -> Unit,
    onSelectLeague: (LeagueItem) -> Unit,
    onClearLeague: () -> Unit,
    filteredLeagues: List<LeagueItem>,
    filteredTeams: List<Club>,
    onLeagueLayout: (SearchListLayout) -> Unit,
    onTeamLayout: (SearchListLayout) -> Unit,
    onSelectTeam: (Club, LeagueItem) -> Unit,
    onHistoryClick: (TeamSearchHistoryEntry) -> Unit,
    onRemoveHistory: (TeamSearchHistoryEntry) -> Unit,
) {
    val league = ui.selectedLeague
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (ui.history.isNotEmpty()) {
                Text(
                    "Recent",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ForzaBallPrimary,
                )
                Spacer(Modifier.height(8.dp))
                ui.history.forEach { entry ->
                    HistoryRow(
                        entry = entry,
                        onClick = { onHistoryClick(entry) },
                        onRemove = { onRemoveHistory(entry) },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                "League",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.leagueQuery,
                onValueChange = onLeagueQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search leagues") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            Spacer(Modifier.height(4.dp))
            ListLayoutToggle(
                current = ui.leagueLayout,
                onSelect = onLeagueLayout,
            )
            Spacer(Modifier.height(8.dp))
            if (league == null) {
                if (ui.leagueLayout == SearchListLayout.List) {
                    filteredLeagues.forEach { item ->
                        LeagueListRow(
                            item = item,
                            onClick = { onSelectLeague(item) },
                        )
                    }
                } else {
                    val rows = filteredLeagues.chunked(2)
                    rows.forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { item ->
                                LeagueGridCell(
                                    item = item,
                                    onClick = { onSelectLeague(item) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Selected: ${league.name}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Change",
                        color = ForzaBallPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable(onClick = onClearLeague)
                            .padding(8.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (ui.isLoadingTeams) {
                    Text("Loading teams…", style = MaterialTheme.typography.bodySmall)
                }
                ui.teamLoadError?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (!ui.isLoadingTeams) {
                    Text(
                        "Team in ${league.name}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ui.teamQuery,
                        onValueChange = onTeamQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search team") },
                    )
                    Spacer(Modifier.height(4.dp))
                    ListLayoutToggle(
                        current = ui.teamLayout,
                        onSelect = onTeamLayout,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (ui.teamLayout == SearchListLayout.List) {
                        filteredTeams.forEach { club ->
                            ClubListRow(
                                club = club,
                                onClick = { onSelectTeam(club, league) },
                            )
                        }
                    } else {
                        val rows = filteredTeams.chunked(2)
                        rows.forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { club ->
                                    ClubGridCell(
                                        club = club,
                                        onClick = { onSelectTeam(club, league) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListLayoutToggle(
    current: SearchListLayout,
    onSelect: (SearchListLayout) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val listSel = current == SearchListLayout.List
        val gridSel = current == SearchListLayout.Grid
        IconButton(
            onClick = { onSelect(SearchListLayout.List) },
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (listSel) Modifier.background(ForzaBallPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    else Modifier,
                ),
        ) {
            Icon(
                Icons.Filled.ViewList,
                "List",
                tint = if (listSel) ForzaBallPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onSelect(SearchListLayout.Grid) },
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (gridSel) Modifier.background(ForzaBallPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    else Modifier,
                ),
        ) {
            Icon(
                Icons.Filled.GridView,
                "Grid",
                tint = if (gridSel) ForzaBallPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: TeamSearchHistoryEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.teamCrestUrl != null) {
                AsyncImage(
                    model = entry.teamCrestUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(entry.teamName.take(1), style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.teamName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                entry.leagueName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onRemove() }, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Close,
                "Remove from history",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LeagueListRow(
    item: LeagueItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.logoUrl != null) {
            AsyncImage(
                model = item.logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.country,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LeagueGridCell(
    item: LeagueItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .padding(vertical = 4.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (item.logoUrl != null) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Spacer(Modifier.size(56.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClubListRow(
    club: Club,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (club.crestUrl != null) {
            AsyncImage(
                model = club.crestUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(club.name.take(1), style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            club.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ClubGridCell(
    club: Club,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .padding(vertical = 4.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (club.crestUrl != null) {
                AsyncImage(
                    model = club.crestUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(club.name.take(1), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = club.name,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
