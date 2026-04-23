package com.forzaball.app.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.TeamNextMatch
import com.forzaball.domain.model.leagueSlugsForSingleTeamSearch
import com.forzaball.domain.repository.MatchRepository
import com.forzaball.domain.repository.NewsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class TeamSearchProfileState(
    val isLoading: Boolean = true,
    val teamDisplayName: String = "",
    val teamCrestUrl: String? = null,
    val match: Match? = null,
    val isLive: Boolean = false,
    val news: List<NewsArticle> = emptyList(),
    val errorMessage: String? = null,
)

class TeamSearchProfileViewModel(
    private val matchRepository: MatchRepository,
    private val newsRepository: NewsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val leagueSlug: String = savedStateHandle.get<String>("leagueSlug").orEmpty().trim()
    private val teamId: String = savedStateHandle.get<String>("teamId").orEmpty().trim()

    private val _state = MutableStateFlow(TeamSearchProfileState())
    val state: StateFlow<TeamSearchProfileState> = _state.asStateFlow()

    init {
        if (leagueSlug.isNotEmpty() && teamId.isNotEmpty()) {
            load()
        } else {
            _state.update { it.copy(isLoading = false, errorMessage = "Missing team or league") }
        }
    }

    fun load() {
        if (leagueSlug.isEmpty() || teamId.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                coroutineScope {
                    val homeDeferred = async {
                        val slugs = leagueSlugsForSingleTeamSearch(leagueSlug)
                        if (slugs.isEmpty()) return@async null
                        matchRepository.loadFavoriteHighlightAndLive(
                            favoriteLeagueSlugs = slugs,
                            favoriteTeamIds = listOf(teamId),
                            scheduleLeagueSlugs = null,
                        )
                    }
                    val tnmDeferred = async {
                        matchRepository.loadNextMatchForFavoriteTeam(
                            domesticLeagueSlug = leagueSlug,
                            teamId = teamId,
                            fallbackTeamDisplayName = null,
                            includeChampionsLeagueSchedule = leagueSlug != "uefa.champions",
                        )
                    }
                    val newsDeferred = async {
                        newsRepository.loadNewsForSingleTeam(
                            leagueSlug = leagueSlug,
                            teamId = teamId,
                            maxArticles = 30,
                        )
                    }
                    val home = homeDeferred.await()
                    val tnm = tnmDeferred.await()
                    val news = newsDeferred.await()

                    val live = home?.liveMatches?.firstOrNull { m ->
                        m.homeClub.id == teamId || m.awayClub.id == teamId
                    }
                    val highlight = home?.highlightMatch
                    val highlightInvolves = highlight != null &&
                        (highlight.homeClub.id == teamId || highlight.awayClub.id == teamId)
                    val chosen: Match? = when {
                        live != null -> live
                        highlightInvolves -> highlight
                        else -> tnm?.nextMatch
                    }

                    val (name, crest) = when {
                        chosen != null -> nameAndCrestForMatch(chosen, teamId, tnm)
                        tnm != null -> {
                            val n = tnm.teamDisplayName.takeIf { it.isNotBlank() } ?: "Team"
                            n to tnm.teamCrestUrl
                        }
                        else -> "Team" to null
                    }

                    _state.value = TeamSearchProfileState(
                        isLoading = false,
                        teamDisplayName = name,
                        teamCrestUrl = crest,
                        match = chosen,
                        isLive = chosen?.isLive == true,
                        news = news,
                        errorMessage = null,
                    )
                }
            }.onFailure { e ->
                Timber.w(e, "TeamSearchProfile load")
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message,
                    )
                }
            }
        }
    }
}

private fun nameAndCrestForMatch(
    m: Match,
    teamId: String,
    tnm: TeamNextMatch?,
): Pair<String, String?> = when (teamId) {
    m.homeClub.id -> m.homeClub.name to m.homeClub.crestUrl
    m.awayClub.id -> m.awayClub.name to m.awayClub.crestUrl
    else -> (tnm?.teamDisplayName?.takeIf { it.isNotBlank() } ?: "Team") to tnm?.teamCrestUrl
}
