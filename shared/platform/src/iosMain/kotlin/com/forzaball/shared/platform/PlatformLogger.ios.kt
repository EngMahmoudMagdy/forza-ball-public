package com.forzaball.shared.platform

import platform.Foundation.NSLog

actual object PlatformLogger {
    actual fun d(tag: String, message: String) {
        NSLog("$tag: $message")
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        val errorMessage = throwable?.message ?: "none"
        NSLog("$tag: $message (error=$errorMessage)")
    }
}
