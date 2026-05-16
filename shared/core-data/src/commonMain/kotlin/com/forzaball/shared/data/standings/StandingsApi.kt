package com.forzaball.shared.data.standings

import com.forzaball.shared.data.createPlatformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EspnStandingsApiRootDto(
    val name: String? = null,
    val children: List<EspnStandingsChildGroupDto>? = null,
)

@Serializable
data class EspnStandingsChildGroupDto(
    val standings: EspnStandingsTableDto? = null,
)

@Serializable
data class EspnStandingsTableDto(
    val entries: List<EspnStandingsEntryDto>? = null,
)

@Serializable
data class EspnStandingsEntryDto(
    val team: EspnStandingsTeamRefDto? = null,
    val stats: List<EspnStandingStatDto>? = null,
)

@Serializable
data class EspnStandingsTeamRefDto(
    val id: String? = null,
    val displayName: String? = null,
)

@Serializable
data class EspnStandingStatDto(
    val name: String? = null,
    val type: String? = null,
    val summary: String? = null,
    @SerialName("displayValue")
    val displayValue: String? = null,
    val value: Double? = null,
)

class EspnStandingsApi(
    private val httpClient: HttpClient = createPlatformHttpClient(),
) {
    suspend fun standings(leagueSlug: String): EspnStandingsApiRootDto {
        val slug = leagueSlug.trim()
        return httpClient.get("https://site.api.espn.com/apis/v2/sports/soccer/$slug/standings").body()
    }
}
