package com.forzaball.data.news

import com.forzaball.data.network.EspnApiService
import com.forzaball.data.network.toNewsArticle
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.repository.NewsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class NewsRepositoryImpl(
    private val espn: EspnApiService,
) : NewsRepository {

    override suspend fun loadNewsForPreferences(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
        maxArticles: Int,
    ): List<NewsArticle> {
        val leagues = favoriteLeagueSlugs.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (leagues.isEmpty()) return emptyList()
        val teamSet = favoriteTeamIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        return runCatching {
            val articles = coroutineScope {
                leagues.map { slug ->
                    async {
                        runCatching { espn.news(slug) }
                            .getOrElse { e ->
                                Timber.w(e, "ESPN news failed for %s", slug)
                                null
                            }
                            ?.articles.orEmpty()
                            .mapNotNull { it.toNewsArticle(slug) }
                    }
                }.flatMap { it.await() }
            }

            val filtered = if (teamSet.isNotEmpty()) {
                articles.filter { article ->
                    article.clubIds.any { it in teamSet } || article.clubIds.isEmpty()
                }
            } else {
                articles
            }

            filtered
                .distinctBy { it.id }
                .sortedByDescending { it.publishedAtMillis }
                .take(maxArticles.coerceAtLeast(1))
        }.getOrElse { e ->
            Timber.e(e, "loadNewsForPreferences failed")
            emptyList()
        }
    }
}
