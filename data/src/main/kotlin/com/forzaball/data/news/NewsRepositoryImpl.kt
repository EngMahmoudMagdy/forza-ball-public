package com.forzaball.data.news

import com.forzaball.data.network.EspnApiService
import com.forzaball.data.network.toNewsArticle
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.leagueSlugsForSingleTeamSearch
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

    override suspend fun loadNewsForSingleTeam(
        leagueSlug: String,
        teamId: String,
        maxArticles: Int,
    ): List<NewsArticle> {
        val tid = teamId.trim().takeIf { it.isNotEmpty() } ?: return emptyList()
        val slugs = leagueSlugsForSingleTeamSearch(leagueSlug)
        if (slugs.isEmpty()) return emptyList()
        val cap = maxArticles.coerceIn(1, 200)
        return runCatching {
            val articles = coroutineScope {
                slugs.map { slug ->
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
            articles
                .filter { tid in it.clubIds }
                .distinctBy { it.id }
                .sortedByDescending { it.publishedAtMillis }
                .take(cap)
        }.getOrElse { e ->
            Timber.e(e, "loadNewsForSingleTeam failed")
            emptyList()
        }
    }

    override suspend fun loadNewsForDomesticLeague(leagueSlug: String?, maxArticles: Int): List<NewsArticle> {
        val slug = leagueSlug?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val cap = maxArticles.coerceIn(1, 500)
        return runCatching {
            Timber.tag("EspnHome").d("GET site …/soccer/%s/news (maxArticles=%d)", slug, cap)
            val envelope = espn.news(slug)
            val rawCount = envelope.articles.orEmpty().size
            val mapped = envelope.articles.orEmpty()
                .mapNotNull { it.toNewsArticle(slug) }
                .distinctBy { it.id }
                .sortedByDescending { it.publishedAtMillis }
            val articles = mapped.take(cap)
            Timber.tag("EspnHome").d(
                "…news %s rawArticles=%d mapped=%d returned=%d",
                slug,
                rawCount,
                mapped.size,
                articles.size,
            )
            articles
        }.getOrElse { e ->
            Timber.e(e, "loadNewsForDomesticLeague failed slug=%s", slug)
            emptyList()
        }
    }
}
