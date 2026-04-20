package com.forzaball.data.match

import com.forzaball.data.network.EspnApiService
import com.forzaball.data.network.EspnScheduleHeaderTeamDto
import com.forzaball.data.network.espnLeagueDisplayNameForSlug
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
                            .mapNotNull { it.toMatchFromEvent("all") }
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

    override suspend fun loadNextMatchForFavoriteTeam(
        domesticLeagueSlug: String?,
        teamId: String?,
        fallbackTeamDisplayName: String?,
    ): TeamNextMatch? {
        val tid = teamId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val league = domesticLeagueSlug?.trim()?.takeIf { it.isNotEmpty() }
        val now = System.currentTimeMillis()

        var headerTeam: EspnScheduleHeaderTeamDto? = null
        val collected = mutableListOf<Match>()

        suspend fun ingestSchedule(leagueSlug: String) {
            val env = runCatching { espn.teamScheduleInLeague(leagueSlug, tid) }
                .getOrElse { e ->
                    Timber.w(e, "team schedule failed league=%s team=%s", leagueSlug, tid)
                    return
                }
            if (headerTeam == null) headerTeam = env.team
            env.events.orEmpty().mapNotNullTo(collected) { it.toMatchFromEvent(leagueSlug) }
        }

        if (league != null) {
            ingestSchedule(league)
        } else {
            runCatching { espn.teamSchedule(tid) }
                .onSuccess { env ->
                    headerTeam = env.team
                    env.events.orEmpty().mapNotNullTo(collected) { it.toMatchFromEvent("all") }
                }
                .onFailure { e -> Timber.w(e, "all team schedule failed team=%s", tid) }
        }

        if (league != "uefa.champions") {
            ingestSchedule("uefa.champions")
        }

        val merged = collected.distinctBy { it.id }
        val sorted = merged.filter { !it.isCompleted && !isFinished(it) }
            .sortedBy { it.startTimeMillis }
        val next = sorted.firstOrNull { it.startTimeMillis >= now - 60_000L }
            ?: sorted.firstOrNull()

        val h = headerTeam
        return TeamNextMatch(
            teamId = tid,
            teamDisplayName = h?.displayName?.takeIf { it.isNotBlank() }
                ?: fallbackTeamDisplayName.orEmpty(),
            teamCrestUrl = h?.logo,
            nextMatch = next,
        )
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
                    comp.toMatch(event.id, slug, espnLeagueDisplayNameForSlug(slug))
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

}
