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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class MatchRepositoryImpl(
    private val espn: EspnApiService,
) : MatchRepository {

    override suspend fun loadFavoriteHighlightAndLive(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
        scheduleLeagueSlugs: List<String>?,
    ): HomeMatchContent {
        val leagues = favoriteLeagueSlugs.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (leagues.isEmpty()) {
            return HomeMatchContent(highlightMatch = null, liveMatches = emptyList())
        }
        val teamSet = favoriteTeamIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val scheduleSlugs = scheduleLeagueSlugs
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: leagues

        return runCatching {
            Timber.tag("EspnHome").d(
                "loadFavoriteHighlightAndLive ESPN scoreboards leagues=%s teamIds=%s scheduleSlugs=%s",
                leagues,
                teamSet,
                scheduleSlugs,
            )
            val fromBoards = loadMatchesFromScoreboards(leagues)
            Timber.tag("EspnHome").d(
                "loadFavoriteHighlightAndLive scoreboard branch rawMatches=%d",
                fromBoards.size,
            )
            val fromSchedules = if (teamSet.isNotEmpty()) {
                loadTeamSchedulesFromLeagueEndpoints(scheduleSlugs, teamSet.toList())
            } else {
                emptyList()
            }
            Timber.tag("EspnHome").d(
                "loadFavoriteHighlightAndLive schedule branch rawMatches=%d",
                fromSchedules.size,
            )
            val allMatches = (fromBoards + fromSchedules).distinctBy { it.id }

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

            Timber.tag("EspnHome").d(
                "loadFavoriteHighlightAndLive merged=%d filtered=%d live=%d upcoming=%d highlight=%s",
                allMatches.size,
                filtered.size,
                live.size,
                upcoming.size,
                highlight?.id,
            )
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
            val fromSchedules = loadTeamSchedulesFromLeagueEndpoints(leagues, teams)
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
        includeChampionsLeagueSchedule: Boolean,
    ): TeamNextMatch? {
        val tid = teamId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val league = domesticLeagueSlug?.trim()?.takeIf { it.isNotEmpty() }
        val now = System.currentTimeMillis()

        Timber.tag("EspnHome").d(
            "loadNextMatchForFavoriteTeam team=%s domesticLeague=%s includeUcl=%s",
            tid,
            league ?: "(null → all schedule)",
            includeChampionsLeagueSchedule,
        )

        var headerTeam: EspnScheduleHeaderTeamDto? = null
        val collected = mutableListOf<Match>()

        suspend fun ingestSchedule(leagueSlug: String) {
            Timber.tag("EspnHome").d(
                "GET site …/soccer/%s/teams/%s/schedule?fixture=true",
                leagueSlug,
                tid,
            )
            val env = runCatching { espn.teamScheduleInLeague(leagueSlug, tid) }
                .getOrElse { e ->
                    Timber.w(e, "team schedule failed league=%s team=%s", leagueSlug, tid)
                    return
                }
            val mapped = env.events.orEmpty().mapNotNull { it.toMatchFromEvent(leagueSlug) }
            Timber.tag("EspnHome").d(
                "…schedule response events=%d mappedMatches=%d",
                env.events.orEmpty().size,
                mapped.size,
            )
            if (headerTeam == null) headerTeam = env.team
            collected.addAll(mapped)
        }

        if (league != null) {
            ingestSchedule(league)
        } else {
            Timber.tag("EspnHome").d("GET site …/soccer/all/teams/%s/schedule?fixture=true", tid)
            runCatching { espn.teamSchedule(tid) }
                .onSuccess { env ->
                    headerTeam = env.team
                    val mapped = env.events.orEmpty().mapNotNull { it.toMatchFromEvent("all") }
                    Timber.tag("EspnHome").d(
                        "all schedule events=%d mappedMatches=%d",
                        env.events.orEmpty().size,
                        mapped.size,
                    )
                    collected.addAll(mapped)
                }
                .onFailure { e -> Timber.w(e, "all team schedule failed team=%s", tid) }
        }

        if (includeChampionsLeagueSchedule && league != "uefa.champions") {
            ingestSchedule("uefa.champions")
        }

        val merged = collected.distinctBy { it.id }
        var upcoming = merged.filter { !it.isCompleted && !isFinished(it) }
            .sortedBy { it.startTimeMillis }
        if (upcoming.isEmpty() && merged.any { !it.isCompleted }) {
            // Heuristic `isFinished` can mis-read some ESPN status strings; never hide real fixtures.
            upcoming = merged.filter { !it.isCompleted }.sortedBy { it.startTimeMillis }
            Timber.tag("EspnHome").d(
                "loadNextMatchForFavoriteTeam using isCompleted-only filter (was %d after heuristic)",
                merged.count { !it.isCompleted && !isFinished(it) },
            )
        }
        val next = upcoming.firstOrNull { it.startTimeMillis >= now - 60_000L }
            ?: upcoming.firstOrNull()

        Timber.tag("EspnHome").d(
            "loadNextMatchForFavoriteTeam collected=%d merged=%d candidates=%d chosen=%s",
            collected.size,
            merged.size,
            upcoming.size,
            next?.id,
        )

        val h = headerTeam
        return TeamNextMatch(
            teamId = tid,
            teamDisplayName = h?.displayName?.takeIf { it.isNotBlank() }
                ?: fallbackTeamDisplayName.orEmpty(),
            teamCrestUrl = h?.logo,
            nextMatch = next,
        )
    }

    /**
     * Loads team fixtures from competition-specific schedule APIs so domestic league
     * matches are included alongside UEFA Champions League (not only `all/teams/.../schedule`).
     */
    private suspend fun loadTeamSchedulesFromLeagueEndpoints(
        leagueSlugs: List<String>,
        teamIds: List<String>,
    ): List<Match> = coroutineScope {
        val ids = teamIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (ids.isEmpty()) return@coroutineScope emptyList()

        val cleaned = leagueSlugs.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val domesticSlug = cleaned.firstOrNull { it != "uefa.champions" }
        val includeUcl = cleaned.contains("uefa.champions") &&
            domesticSlug != null &&
            domesticSlug != "uefa.champions"

        ids.map { teamId ->
            async {
                buildList {
                    if (domesticSlug != null) {
                        runCatching { espn.teamScheduleInLeague(domesticSlug, teamId) }
                            .onSuccess { env ->
                                addAll(
                                    env.events.orEmpty().mapNotNull { it.toMatchFromEvent(domesticSlug) },
                                )
                            }
                            .onFailure { e ->
                                Timber.w(e, "domestic schedule failed league=%s team=%s", domesticSlug, teamId)
                            }
                    }
                    if (includeUcl) {
                        runCatching { espn.teamScheduleInLeague("uefa.champions", teamId) }
                            .onSuccess { env ->
                                addAll(
                                    env.events.orEmpty().mapNotNull { it.toMatchFromEvent("uefa.champions") },
                                )
                            }
                            .onFailure { e ->
                                Timber.w(e, "ucl schedule failed team=%s", teamId)
                            }
                    }
                    if (domesticSlug == null && !includeUcl) {
                        runCatching { espn.teamSchedule(teamId) }
                            .onSuccess { env ->
                                addAll(
                                    env.events.orEmpty().mapNotNull { it.toMatchFromEvent("all") },
                                )
                            }
                            .onFailure { e ->
                                Timber.w(e, "all schedule failed team=%s", teamId)
                            }
                    }
                }
            }
        }.awaitAll().flatten()
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
                    comp.toMatch(
                        event.id,
                        slug,
                        espnLeagueDisplayNameForSlug(slug),
                        eventLevelDate = event.date,
                    )
                }
            }
        }.flatMap { it.await() }
    }

    private fun isFinished(match: Match): Boolean {
        if (match.isCompleted) return true
        val s = match.statusShort?.trim()?.lowercase().orEmpty()
        // Avoid `"final" in s` — it matches substrings like "semifinal" / "semi-final".
        if (s == "ft" || s == "final" || s == "ended") return true
        if (s.contains("full time") || s.contains("full-time")) return true
        return false
    }

}
