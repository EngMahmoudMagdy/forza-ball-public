package com.forzaball.data.preferences

import com.forzaball.domain.model.TeamSearchHistoryEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private data class TeamSearchHistoryEntryDto(
    val teamId: String,
    val leagueSlug: String,
    val teamName: String,
    val leagueName: String,
    val teamCrestUrl: String? = null,
    val searchedAtMillis: Long = 0L,
)

private val listSerializer = ListSerializer(TeamSearchHistoryEntryDto.serializer())

internal fun teamSearchHistoryToJson(json: Json, entries: List<TeamSearchHistoryEntry>): String {
    if (entries.isEmpty()) return "[]"
    val dtos = entries.map { e ->
        TeamSearchHistoryEntryDto(
            teamId = e.teamId,
            leagueSlug = e.leagueSlug,
            teamName = e.teamName,
            leagueName = e.leagueName,
            teamCrestUrl = e.teamCrestUrl,
            searchedAtMillis = e.searchedAtMillis,
        )
    }
    return json.encodeToString(listSerializer, dtos)
}

internal fun teamSearchHistoryFromJson(json: Json, raw: String?): List<TeamSearchHistoryEntry> {
    val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
    return runCatching {
        val dtos = json.decodeFromString(listSerializer, s)
        dtos.map { d ->
            TeamSearchHistoryEntry(
                teamId = d.teamId,
                leagueSlug = d.leagueSlug,
                teamName = d.teamName,
                leagueName = d.leagueName,
                teamCrestUrl = d.teamCrestUrl,
                searchedAtMillis = d.searchedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            )
        }
    }.getOrDefault(emptyList())
}
