package com.forzaball.data.news

import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class NewsRepositoryImpl(
    // Inject network / database dependencies here when implemented.
    private val placeholder: Any? = null,
) : NewsRepository {

    private val inMemoryNews = MutableStateFlow<List<NewsArticle>>(emptyList())

    override fun observeClubNews(clubIds: List<String>): Flow<List<NewsArticle>> {
        // TODO: Implement using Retrofit + Room + Paging for a real data source.
        return inMemoryNews
    }
}

