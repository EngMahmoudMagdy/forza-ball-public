package com.forzaball.data.feed

import com.forzaball.domain.repository.FeedPost
import com.forzaball.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FeedRepositoryImpl(
    // Inject network / database dependencies here when implemented.
    private val placeholder: Any? = null,
) : FeedRepository {

    private val feed = MutableStateFlow<List<FeedPost>>(emptyList())

    override fun observeFeed(): Flow<List<FeedPost>> = feed
}

