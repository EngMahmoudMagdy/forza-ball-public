package com.forzaball.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EspnDtosMappingTest {
    @Test
    fun toClubs_prefers_default_logo_and_filters_missing_team_ids() {
        val dto = EspnTeamsEnvelopeDto(
            sports = listOf(
                EspnSportDto(
                    leagues = listOf(
                        EspnLeagueTeamsDto(
                            slug = "eng.1",
                            teams = listOf(
                                EspnTeamEntryDto(
                                    team = EspnTeamDetailDto(
                                        id = "10",
                                        displayName = "Arsenal",
                                        logos = listOf(
                                            EspnLogoDto(href = "fallback.png", rel = listOf("dark")),
                                            EspnLogoDto(href = "default.png", rel = listOf("default")),
                                        ),
                                    ),
                                ),
                                EspnTeamEntryDto(
                                    team = EspnTeamDetailDto(
                                        id = null,
                                        displayName = "Broken",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val clubs = dto.toClubs("eng.1")

        assertEquals(1, clubs.size)
        assertEquals("10", clubs.first().id)
        assertEquals("default.png", clubs.first().crestUrl)
    }

    @Test
    fun toMatch_maps_live_completed_and_minute_fields() {
        val comp = EspnCompetitionDto(
            id = "m1",
            date = "2026-04-15T19:00:00Z",
            status = EspnStatusDto(
                displayClock = "67'",
                type = EspnStatusTypeDto(
                    state = "in",
                    completed = false,
                    shortDetail = "67'",
                ),
            ),
            competitors = listOf(
                EspnCompetitorDto(
                    id = "h1",
                    homeAway = "home",
                    score = "2",
                    team = EspnScoreboardTeamDto(id = "h1", displayName = "Home", logo = "h.png"),
                ),
                EspnCompetitorDto(
                    id = "a1",
                    homeAway = "away",
                    score = "1",
                    team = EspnScoreboardTeamDto(id = "a1", displayName = "Away", logo = "a.png"),
                ),
            ),
        )

        val match = comp.toMatch(
            eventId = "event-id",
            leagueSlug = "eng.1",
            leagueDisplayName = null,
        )

        assertNotNull(match)
        assertEquals("m1", match!!.id)
        assertEquals(true, match.isLive)
        assertEquals(false, match.isCompleted)
        assertEquals(67, match.minuteElapsed)
        assertEquals("Premier League", match.leagueName)
        assertEquals(2, match.homeScore)
        assertEquals(1, match.awayScore)
    }

    @Test
    fun toMatch_returns_null_when_competitors_or_date_are_missing() {
        val missingTeams = EspnCompetitionDto(
            id = "m1",
            date = "2026-04-15T19:00:00Z",
            competitors = emptyList(),
        )
        val missingDate = EspnCompetitionDto(
            id = "m1",
            competitors = listOf(
                EspnCompetitorDto(homeAway = "home", team = EspnScoreboardTeamDto(id = "h")),
                EspnCompetitorDto(homeAway = "away", team = EspnScoreboardTeamDto(id = "a")),
            ),
        )

        assertNull(missingTeams.toMatch("e", "eng.1", null))
        assertNull(missingDate.toMatch("e", "eng.1", null))
    }

    @Test
    fun toNewsArticle_maps_and_falls_back_to_mobile_link() {
        val dto = EspnNewsArticleDto(
            id = 999L,
            headline = "Headline",
            description = "Summary",
            published = "2026-04-15T19:00:00Z",
            images = listOf(EspnNewsImageDto(url = "img.png")),
            categories = listOf(
                EspnNewsCategoryDto(type = "team", team = EspnCategoryTeamDto(id = 12)),
                EspnNewsCategoryDto(type = "league"),
            ),
            links = EspnArticleLinksDto(
                web = EspnArticleHrefDto(href = ""),
                mobile = EspnArticleHrefDto(href = "https://m.espn.com/story"),
            ),
        )

        val article = dto.toNewsArticle("eng.1")

        assertNotNull(article)
        assertEquals("espn-eng.1-999", article!!.id)
        assertEquals(listOf("12"), article.clubIds)
        assertEquals("https://m.espn.com/story", article.articleUrl)
    }

    @Test
    fun league_name_mapping_handles_known_and_unknown_slugs() {
        assertEquals("La Liga", espnLeagueDisplayNameForSlug("esp.1"))
        assertEquals("my.custom.slug", espnLeagueDisplayNameForSlug("my.custom.slug"))
        assertFalse(espnLeagueDisplayNameForSlug("eng.1").isBlank())
    }
}
