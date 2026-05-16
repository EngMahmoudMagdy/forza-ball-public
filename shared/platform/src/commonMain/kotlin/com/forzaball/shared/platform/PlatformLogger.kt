package com.forzaball.shared.platform

expect object PlatformLogger {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
}
