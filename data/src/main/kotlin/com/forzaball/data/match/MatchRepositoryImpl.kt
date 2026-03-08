package com.forzaball.data.match

import com.forzaball.domain.model.Club
import com.forzaball.domain.model.Match
import com.forzaball.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

class MatchRepositoryImpl(
    // Inject network / database dependencies here when implemented.
    private val placeholder: Any? = null,
) : MatchRepository {

    private val currentMatch = MutableStateFlow<Match?>(null)

    init {
        // Stubbed match so the home screen can render something.
        val now = System.currentTimeMillis()
        val home = Club(id = "1", name = "Forza FC", leagueId = "league_1", crestUrl = null)
        val away = Club(id = "2", name = "Rivals FC", leagueId = "league_1", crestUrl = null)
        currentMatch.value = Match(
            id = "match_1",
            homeClub = home,
            awayClub = away,
            startTimeMillis = now + TimeUnit.HOURS.toMillis(2),
            isLive = false,
        )
    }

    override fun observeNextOrLiveMatchForFavoriteClub(): Flow<Match?> = currentMatch
}

