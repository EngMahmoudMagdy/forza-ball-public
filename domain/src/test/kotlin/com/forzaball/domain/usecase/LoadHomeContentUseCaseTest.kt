package com.forzaball.domain.usecase

import com.forzaball.domain.model.Club
import com.forzaball.domain.model.HomeMatchContent
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.TeamNextMatch
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.MatchRepository
import com.forzaball.domain.repository.NewsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadHomeContentUseCaseTest {
    @Test
    fun `invoke builds expected repository arguments and returns composed result`() = runTest {
        val matchRepository = FakeMatchRepository()
        val newsRepository = FakeNewsRepository()
        val traces = mutableListOf<String>()
        val useCase = LoadHomeContentUseCase(
            matchRepository = matchRepository,
            newsRepository = newsRepository,
            tracer = { traces += it },
        )
        val preferences = UserPreferences(
            countryCode = "EG",
            favoriteTeamLeagueSlug = "eng.1",
            favoriteTeamId = "42",
            favoriteTeamName = "Arsenal",
        )

        val result = useCase(preferences)

        assertEquals(listOf("eng.1", "uefa.champions"), matchRepository.highlightLeaguesArg)
        assertEquals(listOf("42"), matchRepository.highlightTeamIdsArg)
        assertEquals(listOf("eng.1"), matchRepository.scheduleLeaguesArg)
        assertEquals("eng.1", newsRepository.domesticLeagueArg)
        assertEquals(200, newsRepository.maxArticlesArg)
        assertEquals("eng.1", matchRepository.nextMatchDomesticLeagueArg)
        assertEquals("42", matchRepository.nextMatchTeamIdArg)
        assertEquals("Arsenal", matchRepository.nextMatchFallbackNameArg)
        assertEquals(false, matchRepository.nextMatchIncludeUclArg)
        assertEquals(matchRepository.homeMatchContentResult, result.matches)
        assertEquals(newsRepository.newsResult, result.news)
        assertEquals(matchRepository.teamNextResult, result.favoriteTeamNextMatch)
        assertEquals(true, traces.first().contains("LoadHomeContent start"))
        assertEquals(true, traces.last().contains("LoadHomeContent complete"))
    }
}

private class FakeMatchRepository : MatchRepository {
    var highlightLeaguesArg: List<String>? = null
    var highlightTeamIdsArg: List<String>? = null
    var scheduleLeaguesArg: List<String>? = null
    var nextMatchDomesticLeagueArg: String? = null
    var nextMatchTeamIdArg: String? = null
    var nextMatchFallbackNameArg: String? = null
    var nextMatchIncludeUclArg: Boolean? = null

    private val sampleClub = Club(
        id = "1",
        name = "Home",
        leagueId = "eng.1",
        crestUrl = null,
    )
    private val sampleMatch = Match(
        id = "m1",
        homeClub = sampleClub,
        awayClub = sampleClub.copy(id = "2", name = "Away"),
        startTimeMillis = 1000L,
        isLive = false,
    )
    val homeMatchContentResult = HomeMatchContent(
        highlightMatch = sampleMatch,
        liveMatches = listOf(sampleMatch),
    )
    val teamNextResult = TeamNextMatch(
        teamId = "42",
        teamDisplayName = "Arsenal",
        teamCrestUrl = null,
        nextMatch = sampleMatch,
    )

    override suspend fun loadFavoriteHighlightAndLive(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
        scheduleLeagueSlugs: List<String>?,
    ): HomeMatchContent {
        highlightLeaguesArg = favoriteLeagueSlugs
        highlightTeamIdsArg = favoriteTeamIds
        scheduleLeaguesArg = scheduleLeagueSlugs
        return homeMatchContentResult
    }

    override suspend fun loadMergedFixtures(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
    ): List<Match> = emptyList()

    override suspend fun loadNextMatchForFavoriteTeam(
        domesticLeagueSlug: String?,
        teamId: String?,
        fallbackTeamDisplayName: String?,
        includeChampionsLeagueSchedule: Boolean,
    ): TeamNextMatch? {
        nextMatchDomesticLeagueArg = domesticLeagueSlug
        nextMatchTeamIdArg = teamId
        nextMatchFallbackNameArg = fallbackTeamDisplayName
        nextMatchIncludeUclArg = includeChampionsLeagueSchedule
        return teamNextResult
    }
}

private class FakeNewsRepository : NewsRepository {
    var domesticLeagueArg: String? = null
    var maxArticlesArg: Int? = null

    val newsResult = listOf(
        NewsArticle(
            id = "n1",
            title = "Title",
            summary = "Summary",
            imageUrl = null,
            publishedAtMillis = 1234L,
            leagueId = "eng.1",
            clubIds = emptyList(),
            articleUrl = null,
        ),
    )

    override suspend fun loadNewsForPreferences(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
        maxArticles: Int,
    ): List<NewsArticle> = emptyList()

    override suspend fun loadNewsForDomesticLeague(leagueSlug: String?, maxArticles: Int): List<NewsArticle> {
        domesticLeagueArg = leagueSlug
        maxArticlesArg = maxArticles
        return newsResult
    }

    override suspend fun loadNewsForSingleTeam(
        leagueSlug: String,
        teamId: String,
        maxArticles: Int,
    ): List<NewsArticle> = emptyList()
}
