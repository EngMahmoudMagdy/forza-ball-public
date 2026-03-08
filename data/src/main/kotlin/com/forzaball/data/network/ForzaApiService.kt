package com.forzaball.data.network

import retrofit2.http.GET

interface ForzaApiService {
    // TODO: Define endpoints for matches, news, feeds, etc.
    @GET("health")
    suspend fun healthCheck()
}

