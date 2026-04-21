package com.forzaball.data.standings

import com.forzaball.data.network.EspnTablesApiService
import com.forzaball.data.network.espnLeagueDisplayNameForSlug
import com.forzaball.data.network.findEntryForTeam
import com.forzaball.data.network.toSnapshot
import com.forzaball.domain.model.TeamStandingSnapshot
import com.forzaball.domain.repository.StandingsRepository
import timber.log.Timber

class StandingsRepositoryImpl(
    private val tables: EspnTablesApiService,
) : StandingsRepository {

    override suspend fun getTeamStanding(leagueSlug: String, teamId: String): TeamStandingSnapshot? {
        val slug = leagueSlug.trim().takeIf { it.isNotEmpty() } ?: return null
        val tid = teamId.trim().takeIf { it.isNotEmpty() } ?: return null
        return runCatching {
            val dto = tables.standings(slug)
            val entry = dto.findEntryForTeam(tid) ?: return@runCatching null
            val leagueLabel = dto.name?.takeIf { it.isNotBlank() } ?: espnLeagueDisplayNameForSlug(slug)
            entry.toSnapshot(slug, leagueLabel)
        }.getOrElse { e ->
            Timber.w(e, "standings failed league=%s team=%s", slug, tid)
            null
        }
    }
}
