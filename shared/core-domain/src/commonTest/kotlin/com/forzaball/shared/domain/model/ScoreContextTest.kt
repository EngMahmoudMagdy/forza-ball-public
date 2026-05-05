package com.forzaball.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScoreContextTest {
    @Test
    fun `normalization trims and validates fields`() {
        val context = ScoreContext(
            favoriteTeamLeagueSlug = " eng.1 ",
            favoriteTeamId = " 359 ",
        )

        assertEquals("eng.1", context.normalizedLeagueSlug())
        assertEquals("359", context.normalizedTeamId())
    }

    @Test
    fun `normalization returns null for blanks`() {
        val context = ScoreContext(
            favoriteTeamLeagueSlug = "   ",
            favoriteTeamId = "",
        )

        assertNull(context.normalizedLeagueSlug())
        assertNull(context.normalizedTeamId())
    }
}
