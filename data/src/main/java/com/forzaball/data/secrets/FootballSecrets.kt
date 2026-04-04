package com.forzaball.data.secrets

/**
 * API key and base URL are XOR-obfuscated in native code (see [data/src/main/cpp]).
 * Set [API_FOOTBALL_KEY] and optional [API_FOOTBALL_BASE_URL] in **root** `local.properties` (not committed).
 */
object FootballSecrets {
    init {
        System.loadLibrary("forzasecrets")
    }

    @JvmStatic
    private external fun nativeApiKey(): String

    @JvmStatic
    private external fun nativeBaseUrl(): String

    fun apiKey(): String = nativeApiKey()

    fun baseUrl(): String = nativeBaseUrl().ensureTrailingSlash()

    private fun String.ensureTrailingSlash(): String =
        if (endsWith('/')) this else "$this/"
}
