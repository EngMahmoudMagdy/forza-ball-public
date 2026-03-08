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
    val FAVORITE_LEAGUES = stringPreferencesKey("favorite_leagues")
    val FAVORITE_CLUBS = stringPreferencesKey("favorite_clubs")
    val NICKNAME = stringPreferencesKey("nickname")
    val PROFILE_PHOTO_URL = stringPreferencesKey("profile_photo_url")
}

class PreferencesRepositoryImpl(
    private val context: Context,
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
            prefs[Keys.FAVORITE_LEAGUES] = preferences.favoriteLeagues.joinToString(separator = ",")
            prefs[Keys.FAVORITE_CLUBS] = preferences.favoriteClubs.joinToString(separator = ",")
            prefs[Keys.NICKNAME] = preferences.nickname.orEmpty()
            prefs[Keys.PROFILE_PHOTO_URL] = preferences.profilePhotoUrl.orEmpty()
        }
    }

    private fun Preferences.toDomain(): UserPreferences {
        val country = this[Keys.COUNTRY]
        val leagues = this[Keys.FAVORITE_LEAGUES]?.split(",")?.filter { it.isNotBlank() }.orEmpty()
        val clubs = this[Keys.FAVORITE_CLUBS]?.split(",")?.filter { it.isNotBlank() }.orEmpty()
        val nickname = this[Keys.NICKNAME]
        val profilePhotoUrl = this[Keys.PROFILE_PHOTO_URL]
        return UserPreferences(
            countryCode = country,
            favoriteLeagues = leagues,
            favoriteClubs = clubs,
            nickname = nickname?.takeIf { it.isNotBlank() },
            profilePhotoUrl = profilePhotoUrl?.takeIf { it.isNotBlank() },
        )
    }
}

