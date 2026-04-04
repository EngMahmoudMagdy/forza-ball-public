package com.forzaball.data.match

import com.forzaball.data.network.ApiFootballService
import com.forzaball.data.network.toMatch
import com.forzaball.data.secrets.FootballSecrets
import com.forzaball.domain.model.HomeMatchContent
import com.forzaball.domain.repository.MatchRepository
import timber.log.Timber

class MatchRepositoryImpl(
    private val api: ApiFootballService,
    private val secrets: FootballSecrets,
) : MatchRepository {

    override suspend fun loadFavoriteHighlightAndLive(clubIds: List<String>): HomeMatchContent {
        if (secrets.apiKey().isBlank()) {
            Timber.w("API_FOOTBALL_KEY missing in local.properties — native key is empty")
            return HomeMatchContent(highlightMatch = null, liveMatches = emptyList())
        }
        val ids = clubIds.mapNotNull { it.toIntOrNull() }
        if (ids.isEmpty()) {
            return HomeMatchContent(highlightMatch = null, liveMatches = emptyList())
        }
        return runCatching {
            val liveDtos = api.fixtures(live = "all").response.orEmpty()
            val idSet = ids.toSet()
            val myLive = liveDtos
                .filter { f ->
                    f.teams.home.id in idSet || f.teams.away.id in idSet
                }
                .map { it.toMatch() }
                .distinctBy { it.id }

            val primary = ids.first()
            val nextFixture = api.fixtures(team = primary, next = 1).response?.firstOrNull()

            val highlight = myLive.firstOrNull()
                ?: nextFixture?.toMatch()

            HomeMatchContent(
                highlightMatch = highlight,
                liveMatches = myLive.take(20),
            )
        }.getOrElse { e ->
            Timber.e(e, "loadFavoriteHighlightAndLive failed")
            HomeMatchContent(highlightMatch = null, liveMatches = emptyList())
        }
    }
}
