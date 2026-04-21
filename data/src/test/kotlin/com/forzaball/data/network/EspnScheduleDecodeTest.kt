package com.forzaball.data.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EspnScheduleDecodeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun fixturesSample_decodesAndMapsToMatches() {
        val text =
            javaClass.getResourceAsStream("/fixtures_sample.json")!!.bufferedReader().readText()
        val env = json.decodeFromString(EspnTeamScheduleEnvelopeDto.serializer(), text)
        assertTrue(env.events.orEmpty().isNotEmpty())
        val first = env.events!!.first()
        val comp = first.competitions!!.first()
        assertTrue(
            "decoded competitors",
            comp.competitors.orEmpty().size >= 2,
        )
        assertEquals("2026-04-15T19:00Z", comp.date)
        val home = comp.competitors!!.first { it.homeAway == "home" }
        val away = comp.competitors!!.first { it.homeAway == "away" }
        assertNotNull(home.team)
        assertNotNull(away.team)
        val m0 = first.toMatchFromEvent("esp.1")
        assertTrue("first event should map to Match: competitors=${comp.competitors?.size} date=${comp.date}", m0 != null)
        val matches = env.events.orEmpty().mapNotNull { it.toMatchFromEvent("esp.1") }
        assertTrue("expected mapped matches from sample", matches.isNotEmpty())
    }
}
