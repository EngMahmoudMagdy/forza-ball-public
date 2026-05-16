package com.forzaball.ui.locale

import java.util.Locale

enum class AppLocale(val storageKey: String, val languageTag: String) {
    English("en", "en"),
    Arabic("ar", "ar"),
    ;

    val locale: Locale get() = Locale.forLanguageTag(languageTag)

    companion object {
        fun fromStorage(key: String?): AppLocale =
            entries.find { it.storageKey == key } ?: English
    }
}
