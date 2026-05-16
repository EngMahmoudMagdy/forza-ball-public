package com.forzaball.shared.data

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient
