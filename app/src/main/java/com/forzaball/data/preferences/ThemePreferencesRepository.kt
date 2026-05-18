package com.forzaball.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.forzaball.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val THEME_DATASTORE = "forzaball_theme"

private val Context.themeDataStore by preferencesDataStore(name = THEME_DATASTORE)

private object ThemeKeys {
    val MODE = stringPreferencesKey("theme_mode")
}

interface ThemePreferencesRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

class ThemePreferencesRepositoryImpl(
    private val context: Context,
) : ThemePreferencesRepository {

    override fun observeThemeMode(): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            ThemeMode.fromStorage(prefs[ThemeKeys.MODE])
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[ThemeKeys.MODE] = mode.storageKey
        }
        AppSettingsCache.themeMode = mode
    }
}
