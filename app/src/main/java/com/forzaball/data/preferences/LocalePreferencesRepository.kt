package com.forzaball.data.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.forzaball.ui.locale.AppLocale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val LOCALE_DATASTORE = "forzaball_locale"

private val Context.localeDataStore by preferencesDataStore(name = LOCALE_DATASTORE)

private object LocaleKeys {
    val LANGUAGE = stringPreferencesKey("app_language")
}

interface LocalePreferencesRepository {
    fun observeLocale(): Flow<AppLocale>
    suspend fun setLocale(locale: AppLocale)
}

class LocalePreferencesRepositoryImpl(
    private val context: Context,
) : LocalePreferencesRepository {

    override fun observeLocale(): Flow<AppLocale> =
        context.localeDataStore.data.map { prefs ->
            AppLocale.fromStorage(prefs[LocaleKeys.LANGUAGE])
        }

    override suspend fun setLocale(locale: AppLocale) {
        context.localeDataStore.edit { prefs ->
            prefs[LocaleKeys.LANGUAGE] = locale.storageKey
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(locale.languageTag),
        )
    }
}
