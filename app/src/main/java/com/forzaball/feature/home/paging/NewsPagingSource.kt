package com.forzaball.feature.home.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.repository.NewsRepository

private const val PAGE_SIZE = 20

class NewsPagingSource(
    private val newsRepository: NewsRepository,
    private val domesticLeagueSlug: String?,
) : PagingSource<Int, NewsArticle>() {

    private var cached: List<NewsArticle>? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NewsArticle> = try {
        val slug = domesticLeagueSlug?.trim()?.takeIf { it.isNotEmpty() }
        if (slug == null) {
            return LoadResult.Page(emptyList(), null, null)
        }
        if (cached == null) {
            cached = newsRepository.loadNewsForDomesticLeague(slug, maxArticles = 500)
        }
        val list = cached.orEmpty()
        val page = params.key ?: 0
        val offset = page * PAGE_SIZE
        val data = list.drop(offset).take(PAGE_SIZE)
        LoadResult.Page(
            data = data,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (data.isEmpty() || offset + PAGE_SIZE >= list.size) null else page + 1,
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    override fun getRefreshKey(state: PagingState<Int, NewsArticle>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
}
