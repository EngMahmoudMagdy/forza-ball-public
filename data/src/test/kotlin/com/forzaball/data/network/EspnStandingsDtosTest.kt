package com.forzaball.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EspnStandingsDtosTest {
    @Test
    fun findEntryForTeam_trims_input_and_searches_all_children() {
        val target = EspnStandingsEntryDto(
            team = EspnStandingsTeamRefDto(id = "42", displayName = "Arsenal"),
            stats = emptyList(),
        )
        val root = EspnStandingsApiRootDto(
            children = listOf(
                EspnStandingsChildGroupDto(
                    standings = EspnStandingsTableDto(entries = emptyList()),
                ),
                EspnStandingsChildGroupDto(
                    standings = EspnStandingsTableDto(entries = listOf(target)),
                ),
            ),
        )

        val found = root.findEntryForTeam(" 42 ")

        assertNotNull(found)
        assertEquals("42", found!!.team?.id)
    }

    @Test
    fun findEntryForTeam_returns_null_for_blank_or_missing_team() {
        val root = EspnStandingsApiRootDto(children = emptyList())
        assertNull(root.findEntryForTeam(" "))
        assertNull(root.findEntryForTeam("999"))
    }

    @Test
    fun toSnapshot_maps_stats_and_record_summary() {
        val entry = EspnStandingsEntryDto(
            team = EspnStandingsTeamRefDto(id = "10", displayName = "Arsenal"),
            stats = listOf(
                EspnStandingStatDto(name = "rank", displayValue = "2"),
                EspnStandingStatDto(name = "gamesPlayed", displayValue = "20"),
                EspnStandingStatDto(name = "wins", displayValue = "14"),
                EspnStandingStatDto(name = "draws", displayValue = "4"),
                EspnStandingStatDto(name = "losses", displayValue = "2"),
                EspnStandingStatDto(name = "points", displayValue = "46"),
                EspnStandingStatDto(name = "pointDifferential", displayValue = "+25"),
                EspnStandingStatDto(type = "total", summary = "14-4-2"),
            ),
        )

        val snapshot = entry.toSnapshot("eng.1", "Premier League")

        assertEquals("eng.1", snapshot.leagueSlug)
        assertEquals("Premier League", snapshot.leagueDisplayName)
        assertEquals("Arsenal", snapshot.teamName)
        assertEquals(2, snapshot.position)
        assertEquals(20, snapshot.played)
        assertEquals(14, snapshot.wins)
        assertEquals(4, snapshot.draws)
        assertEquals(2, snapshot.losses)
        assertEquals(46, snapshot.points)
        assertEquals("+25", snapshot.goalDifferenceDisplay)
        assertEquals("14-4-2", snapshot.recordSummary)
    }
}
