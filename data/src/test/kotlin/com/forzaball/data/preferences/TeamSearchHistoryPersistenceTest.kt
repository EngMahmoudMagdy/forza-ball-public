package com.forzaball.data.preferences

import com.forzaball.domain.model.TeamSearchHistoryEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamSearchHistoryPersistenceTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun teamSearchHistoryToJson_roundTrips_entries() {
        val entries = listOf(
            TeamSearchHistoryEntry(
                teamId = "1",
                leagueSlug = "eng.1",
                teamName = "Arsenal",
                leagueName = "Premier League",
                teamCrestUrl = "arsenal.png",
                searchedAtMillis = 1000L,
            ),
        )

        val encoded = teamSearchHistoryToJson(json, entries)
        val decoded = teamSearchHistoryFromJson(json, encoded)

        assertEquals(1, decoded.size)
        assertEquals("1", decoded.first().teamId)
        assertEquals(1000L, decoded.first().searchedAtMillis)
    }

    @Test
    fun teamSearchHistoryFromJson_returns_empty_for_blank_or_invalid_json() {
        assertTrue(teamSearchHistoryFromJson(json, " ").isEmpty())
        assertTrue(teamSearchHistoryFromJson(json, "not-json").isEmpty())
    }

    @Test
    fun teamSearchHistoryFromJson_backfills_missing_timestamp() {
        val raw = """[{"teamId":"1","leagueSlug":"eng.1","teamName":"Arsenal","leagueName":"Premier League","searchedAtMillis":0}]"""

        val decoded = teamSearchHistoryFromJson(json, raw)

        assertEquals(1, decoded.size)
        assertTrue(decoded.first().searchedAtMillis > 0L)
    }
}
