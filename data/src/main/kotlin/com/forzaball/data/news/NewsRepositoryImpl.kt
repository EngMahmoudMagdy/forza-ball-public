package com.forzaball.data.news

import com.forzaball.data.network.ApiFootballService
import com.forzaball.data.network.toNewsArticle
import com.forzaball.data.network.toResultNewsArticle
import com.forzaball.data.secrets.FootballSecrets
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.repository.NewsRepository
import timber.log.Timber

class NewsRepositoryImpl(
    private val api: ApiFootballService,
    private val secrets: FootballSecrets,
) : NewsRepository {

    override suspend fun loadNewsForClubs(clubIds: List<String>): List<NewsArticle> {
        if (secrets.apiKey().isBlank()) {
            Timber.w("API_FOOTBALL_KEY missing — skipping news fetch")
            return emptyList()
        }
        val ids = clubIds.mapNotNull { it.toIntOrNull() }
        if (ids.isEmpty()) return emptyList()

        return runCatching {
            val articles = mutableListOf<NewsArticle>()
            val primary = ids.first()
            api.fixtures(team = primary, last = 6).response.orEmpty()
                .map { it.toResultNewsArticle() }
                .forEach { articles.add(it) }

            ids.take(3).forEach { teamId ->
                api.injuries(team = teamId).response.orEmpty()
                    .map { it.toNewsArticle() }
                    .forEach { articles.add(it) }
            }

            articles
                .distinctBy { it.id }
                .sortedByDescending { it.publishedAtMillis }
                .take(30)
        }.getOrElse { e ->
            Timber.e(e, "loadNewsForClubs failed")
            emptyList()
        }
    }
}
