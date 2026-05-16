package com.forzaball.feature.home.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.forzaball.domain.model.Match
import com.forzaball.domain.repository.MatchRepository

private const val PAGE_SIZE = 20

class FixturesPagingSource(
    private val matchRepository: MatchRepository,
    private val leagues: List<String>,
    private val teams: List<String>,
) : PagingSource<Int, Match>() {

    private var cached: List<Match>? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Match> = try {
        if (leagues.isEmpty() && teams.isEmpty()) {
            return LoadResult.Page(emptyList(), null, null)
        }
        if (cached == null) {
            cached = matchRepository.loadMergedFixtures(
                favoriteLeagueSlugs = leagues,
                favoriteTeamIds = teams,
            )
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

    override fun getRefreshKey(state: PagingState<Int, Match>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
}
