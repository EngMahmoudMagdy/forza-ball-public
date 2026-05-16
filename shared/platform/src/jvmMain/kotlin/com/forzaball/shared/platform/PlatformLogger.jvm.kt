package com.forzaball.shared.platform

actual object PlatformLogger {
    actual fun d(tag: String, message: String) {
        println("$tag: $message")
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        println("$tag: $message (${throwable?.message ?: "no-error"})")
    }
}
