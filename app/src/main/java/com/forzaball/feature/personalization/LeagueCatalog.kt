package com.forzaball.feature.personalization

import androidx.compose.runtime.Immutable

@Immutable
data class LeagueItem(val id: String, val name: String, val country: String, val logoUrl: String?)

@Immutable
data class ClubItem(val id: String, val name: String, val leagueSlug: String, val crestUrl: String?)

/**
 * Curated ESPN soccer league slugs for onboarding and profile.
 * @see [ESPN Site API](https://site.api.espn.com/apis/site/v2/sports/soccer/)
 */
val catalogLeagues = listOf(
    LeagueItem(
        id = "eng.1",
        name = "Premier League",
        country = "England",
        logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/17.png",
    ),
    LeagueItem(
        id = "esp.1",
        name = "La Liga",
        country = "Spain",
        logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/15.png",
    ),
    LeagueItem(
        id = "ger.1",
        name = "Bundesliga",
        country = "Germany",
        logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/10.png",
    ),
    LeagueItem(
        id = "uefa.champions",
        name = "UEFA Champions League",
        country = "Europe",
        logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2.png",
    ),
    LeagueItem(
        id = "usa.1",
        name = "MLS",
        country = "USA",
        logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/19.png",
    ),
    LeagueItem(
        id = "ksa.1",
        name = "Saudi Pro League",
        country = "Saudi Arabia",
        logoUrl = null,
    ),
)

/** Leagues users pick a domestic club from (UCL is merged into schedules separately). */
val domesticLeagueCatalog = catalogLeagues.filter { it.id != "uefa.champions" }
