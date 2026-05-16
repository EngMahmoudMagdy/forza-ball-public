package com.forzaball.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

private const val DATASTORE_NAME = "forzaball_user_prefs"

private val Context.dataStore by preferencesDataStore(
    name = DATASTORE_NAME,
)

private object Keys {
    val COUNTRY = stringPreferencesKey("country")
    /** Legacy — migrated to favorite team fields on read. */
    val FAVORITE_LEAGUES = stringPreferencesKey("favorite_leagues")
    val FAVORITE_CLUBS = stringPreferencesKey("favorite_clubs")
    val FAVORITE_TEAM_ID = stringPreferencesKey("favorite_team_id")
    val FAVORITE_TEAM_LEAGUE = stringPreferencesKey("favorite_team_league")
    val FAVORITE_TEAM_NAME = stringPreferencesKey("favorite_team_name")
    val NICKNAME = stringPreferencesKey("nickname")
    val PROFILE_PHOTO_URL = stringPreferencesKey("profile_photo_url")
    val PROFILE_PHOTO_THUMB_URL = stringPreferencesKey("profile_photo_thumb_url")
    val TEAM_SEARCH_HISTORY_JSON = stringPreferencesKey("team_search_history_json")
}

class PreferencesRepositoryImpl(
    private val context: Context,
    private val json: Json,
) : PreferencesRepository {

    override fun observeUserPreferences(): Flow<UserPreferences> {
        return context.dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    Timber.w(throwable, "Error reading DataStore, emitting empty preferences")
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { prefs -> prefs.toDomain() }
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COUNTRY] = preferences.countryCode.orEmpty()
            prefs.remove(Keys.FAVORITE_LEAGUES)
            prefs.remove(Keys.FAVORITE_CLUBS)
            prefs[Keys.FAVORITE_TEAM_ID] = preferences.favoriteTeamId.orEmpty()
            prefs[Keys.FAVORITE_TEAM_LEAGUE] = preferences.favoriteTeamLeagueSlug.orEmpty()
            prefs[Keys.FAVORITE_TEAM_NAME] = preferences.favoriteTeamName.orEmpty()
            prefs[Keys.NICKNAME] = preferences.nickname.orEmpty()
            prefs[Keys.PROFILE_PHOTO_URL] = preferences.profilePhotoUrl.orEmpty()
            prefs[Keys.PROFILE_PHOTO_THUMB_URL] = preferences.profilePhotoThumbUrl.orEmpty()
            prefs[Keys.TEAM_SEARCH_HISTORY_JSON] = teamSearchHistoryToJson(json, preferences.teamSearchHistory)
        }
    }

    private fun Preferences.toDomain(): UserPreferences {
        val country = this[Keys.COUNTRY]
        val nickname = this[Keys.NICKNAME]
        val profilePhotoUrl = this[Keys.PROFILE_PHOTO_URL]
        val profilePhotoThumbUrl = this[Keys.PROFILE_PHOTO_THUMB_URL]
        val newTeamId = this[Keys.FAVORITE_TEAM_ID]?.takeIf { it.isNotBlank() }
        val newLeague = this[Keys.FAVORITE_TEAM_LEAGUE]?.takeIf { it.isNotBlank() }
        val newName = this[Keys.FAVORITE_TEAM_NAME]?.takeIf { it.isNotBlank() }
        val historyJson = this[Keys.TEAM_SEARCH_HISTORY_JSON]
        val history = teamSearchHistoryFromJson(json, historyJson)
        if (newTeamId != null) {
            return UserPreferences(
                countryCode = country,
                favoriteTeamLeagueSlug = newLeague,
                favoriteTeamId = newTeamId,
                favoriteTeamName = newName,
                nickname = nickname?.takeIf { it.isNotBlank() },
                profilePhotoUrl = profilePhotoUrl?.takeIf { it.isNotBlank() },
                profilePhotoThumbUrl = profilePhotoThumbUrl?.takeIf { it.isNotBlank() },
                teamSearchHistory = history,
            )
        }
        val legacyLeagues = this[Keys.FAVORITE_LEAGUES]?.split(",")?.filter { it.isNotBlank() }.orEmpty()
        val legacyClubs = this[Keys.FAVORITE_CLUBS]?.split(",")?.filter { it.isNotBlank() }.orEmpty()
        return UserPreferences(
            countryCode = country,
            favoriteTeamLeagueSlug = legacyLeagues.firstOrNull(),
            favoriteTeamId = legacyClubs.firstOrNull(),
            favoriteTeamName = null,
            nickname = nickname?.takeIf { it.isNotBlank() },
            profilePhotoUrl = profilePhotoUrl?.takeIf { it.isNotBlank() },
            profilePhotoThumbUrl = profilePhotoThumbUrl?.takeIf { it.isNotBlank() },
            teamSearchHistory = history,
        )
    }
}
