package com.forzaball.data.preferences

import com.forzaball.ui.locale.AppLocale
import com.forzaball.ui.theme.ThemeMode

/** Loaded once at app start so the first frame uses saved theme/locale. */
object AppSettingsCache {
    @Volatile
    var themeMode: ThemeMode = ThemeMode.Dark

    @Volatile
    var locale: AppLocale = AppLocale.English
}
