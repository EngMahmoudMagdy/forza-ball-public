package com.forzaball.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserPreferencesLogicTest {
    @Test
    fun `leagueSlugsForEspnContent returns domestic and ucl for non-ucl domestic`() {
        val preferences = UserPreferences(
            countryCode = "EG",
            favoriteTeamLeagueSlug = " eng.1 ",
            favoriteTeamId = "1",
            favoriteTeamName = "Arsenal",
        )

        assertEquals(listOf("eng.1", "uefa.champions"), preferences.leagueSlugsForEspnContent())
    }

    @Test
    fun `leagueSlugsForEspnContent returns only ucl when domestic is ucl`() {
        val preferences = UserPreferences(
            countryCode = "EG",
            favoriteTeamLeagueSlug = "uefa.champions",
            favoriteTeamId = "1",
            favoriteTeamName = "Arsenal",
        )

        assertEquals(listOf("uefa.champions"), preferences.leagueSlugsForEspnContent())
    }

    @Test
    fun `favoriteTeamIdsList trims and filters blanks`() {
        val valid = UserPreferences(null, "eng.1", " 123 ", "A")
        val blank = UserPreferences(null, "eng.1", "  ", "A")

        assertEquals(listOf("123"), valid.favoriteTeamIdsList())
        assertTrue(blank.favoriteTeamIdsList().isEmpty())
    }

    @Test
    fun `leagueSlugsForSingleTeamSearch excludes ucl for usa and ksa`() {
        assertEquals(listOf("usa.1"), leagueSlugsForSingleTeamSearch("usa.1"))
        assertEquals(listOf("ksa.1"), leagueSlugsForSingleTeamSearch("ksa.1"))
    }

    @Test
    fun `leagueSlugsForSingleTeamSearch includes ucl for european leagues`() {
        assertEquals(
            listOf("esp.1", "uefa.champions"),
            leagueSlugsForSingleTeamSearch(" esp.1 "),
        )
    }

    @Test
    fun `shouldShowUclScores follows supported league rules`() {
        assertTrue(UserPreferences(null, "eng.1", "1", "Arsenal").shouldShowUclScores())
        assertFalse(UserPreferences(null, "usa.1", "1", "Inter Miami").shouldShowUclScores())
        assertFalse(UserPreferences(null, "ksa.1", "1", "Al Hilal").shouldShowUclScores())
        assertFalse(UserPreferences(null, "uefa.champions", "1", "Arsenal").shouldShowUclScores())
    }
}
