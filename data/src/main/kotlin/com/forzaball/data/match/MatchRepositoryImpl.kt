package com.forzaball.data.match

import com.forzaball.data.network.EspnApiService
import com.forzaball.data.network.toMatch
import com.forzaball.data.network.toMatchFromEvent
import com.forzaball.domain.model.HomeMatchContent
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.TeamNextMatch
import com.forzaball.domain.repository.MatchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class MatchRepositoryImpl(
    private val espn: EspnApiService,
) : MatchRepository {

    override suspend fun loadFavoriteHighlightAndLive(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
    ): HomeMatchContent {
        val leagues = favoriteLeagueSlugs.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (leagues.isEmpty()) {
            return HomeMatchContent(highlightMatch = null, liveMatches = emptyList())
        }
        val teamSet = favoriteTeamIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        return runCatching {
            val allMatches = loadMatchesFromScoreboards(leagues)

            val filtered = if (teamSet.isNotEmpty()) {
                allMatches.filter { m ->
                    m.homeClub.id in teamSet || m.awayClub.id in teamSet
                }
            } else {
                allMatches
            }

            val live = filtered.filter { it.isLive }.distinctBy { it.id }.sortedBy { it.startTimeMillis }
            val upcoming = filtered.filter { !it.isLive && !isFinished(it) }
                .distinctBy { it.id }
                .sortedBy { it.startTimeMillis }
            val highlight = live.firstOrNull() ?: upcoming.firstOrNull()

            HomeMatchContent(
                highlightMatch = highlight,
                liveMatches = live.take(20),
            )
        }.getOrElse { e ->
            Timber.e(e, "loadFavoriteHighlightAndLive failed")
            HomeMatchContent(highlightMatch = null, liveMatches = emptyList())
        }
    }

    override suspend fun loadMergedFixtures(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
    ): List<Match> {
        val leagues = favoriteLeagueSlugs.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val teams = favoriteTeamIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return runCatching {
            val fromBoards = if (leagues.isNotEmpty()) loadMatchesFromScoreboards(leagues) else emptyList()
            val fromSchedules = coroutineScope {
                teams.map { id ->
                    async {
                        runCatching { espn.teamSchedule(id) }
                            .getOrElse { e ->
                                Timber.w(e, "team schedule failed for %s", id)
                                null
                            }
                            ?.events.orEmpty()
                            .mapNotNull { it.toMatchFromEvent() }
                    }
                }.flatMap { it.await() }
            }
            (fromBoards + fromSchedules)
                .distinctBy { it.id }
                .sortedBy { it.startTimeMillis }
        }.getOrElse { e ->
            Timber.e(e, "loadMergedFixtures failed")
            emptyList()
        }
    }

    override suspend fun loadNextMatchPerFavoriteTeam(teamIds: List<String>): List<TeamNextMatch> {
        val ids = teamIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (ids.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        return coroutineScope {
            ids.map { teamId ->
                async {
                    runCatching {
                        val env = espn.teamSchedule(teamId)
                        val header = env.team
                        val matches = env.events.orEmpty().mapNotNull { it.toMatchFromEvent() }
                        val sorted = matches.filter { !it.isCompleted && !isFinished(it) }
                            .sortedBy { it.startTimeMillis }
                        val next = sorted.firstOrNull { it.startTimeMillis >= now - 60_000L }
                            ?: sorted.firstOrNull()
                        TeamNextMatch(
                            teamId = teamId,
                            teamDisplayName = header?.displayName.orEmpty(),
                            teamCrestUrl = header?.logo,
                            nextMatch = next,
                        )
                    }.getOrElse { e ->
                        Timber.w(e, "next match for team %s", teamId)
                        TeamNextMatch(
                            teamId = teamId,
                            teamDisplayName = "",
                            teamCrestUrl = null,
                            nextMatch = null,
                        )
                    }
                }
            }.map { it.await() }
        }
    }

    private suspend fun loadMatchesFromScoreboards(leagues: List<String>): List<Match> = coroutineScope {
        leagues.map { slug ->
            async {
                val dto = runCatching { espn.scoreboard(slug) }
                    .getOrElse { e ->
                        Timber.w(e, "ESPN scoreboard failed for %s", slug)
                        null
                    } ?: return@async emptyList()
                dto.events.orEmpty().mapNotNull { event ->
                    val comp = event.competitions?.firstOrNull() ?: return@mapNotNull null
                    comp.toMatch(event.id, slug, leagueDisplayNameForSlug(slug))
                }
            }
        }.flatMap { it.await() }
    }

    private fun isFinished(match: Match): Boolean {
        if (match.isCompleted) return true
        val s = match.statusShort?.lowercase().orEmpty()
        if ("final" in s || s == "ft") return true
        return false
    }

    private fun leagueDisplayNameForSlug(slug: String): String = when (slug) {
        "eng.1" -> "Premier League"
        "esp.1" -> "La Liga"
        "ger.1" -> "Bundesliga"
        "uefa.champions" -> "UEFA Champions League"
        "usa.1" -> "MLS"
        "ksa.1" -> "Saudi Pro League"
        else -> slug
    }
}
