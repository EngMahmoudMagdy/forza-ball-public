package com.forzaball.domain.diagnostics

/** Optional trace hook for [com.forzaball.domain.usecase.LoadHomeContentUseCase] (e.g. Timber on Android). */
fun interface HomeLoadTracer {
    operator fun invoke(message: String)
}
