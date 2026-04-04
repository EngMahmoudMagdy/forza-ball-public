package com.forzaball.data.network

import com.forzaball.data.secrets.FootballSecrets
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Sends the key expected by api-sports / API-Football ([documentation](https://www.api-football.com/documentation-v3)).
 */
class ApiFootballAuthInterceptor(
    private val secrets: FootballSecrets,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = secrets.apiKey()
        val request = if (key.isNotBlank()) {
            chain.request().newBuilder()
                .header("x-apisports-key", key)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
