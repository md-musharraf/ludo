package com.example.ludo.core.logging

import android.util.Log

/**
 * Leveled logging with lazy messages. Disabled in release builds (see MainActivity), and R8
 * strips android.util.Log calls there as well, so nothing about game state leaks to logcat.
 * Falls back to stdout/stderr on the JVM (unit tests), where android.util.Log is a stub.
 */
object AppLogger {
    private const val DEFAULT_TAG = "LudoApp"

    @Volatile
    var isLoggingEnabled: Boolean = true

    fun d(tag: String = DEFAULT_TAG, message: () -> String) = log(Log.DEBUG, tag, null, message)
    fun i(tag: String = DEFAULT_TAG, message: () -> String) = log(Log.INFO, tag, null, message)
    fun w(tag: String = DEFAULT_TAG, message: () -> String) = log(Log.WARN, tag, null, message)
    fun w(tag: String, throwable: Throwable?, message: () -> String) = log(Log.WARN, tag, throwable, message)
    fun e(tag: String = DEFAULT_TAG, message: () -> String) = log(Log.ERROR, tag, null, message)
    fun e(tag: String, throwable: Throwable?, message: () -> String) = log(Log.ERROR, tag, throwable, message)

    private fun log(level: Int, tag: String, throwable: Throwable?, message: () -> String) {
        if (!isLoggingEnabled) return
        val text = message()
        try {
            when (level) {
                Log.DEBUG -> Log.d(tag, text, throwable)
                Log.INFO -> Log.i(tag, text, throwable)
                Log.WARN -> Log.w(tag, text, throwable)
                else -> Log.e(tag, text, throwable)
            }
        } catch (_: RuntimeException) {
            val out = if (level >= Log.WARN) System.err else System.out
            out.println("${levelName(level)} [$tag]: $text")
            throwable?.printStackTrace()
        }
    }

    private fun levelName(level: Int) = when (level) {
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        else -> "ERROR"
    }

    /** Measures a block and warns when it exceeds one 60 fps frame (16 ms). */
    inline fun <T> measureTrace(name: String, block: () -> T): T {
        val start = System.nanoTime()
        return try {
            block()
        } finally {
            val durationMs = (System.nanoTime() - start) / 1_000_000.0
            if (durationMs > 16.0) {
                w("PerformanceTrace") { "SLOW OPERATION: $name took ${"%.2f".format(durationMs)}ms" }
            } else {
                d("PerformanceTrace") { "$name completed in ${"%.2f".format(durationMs)}ms" }
            }
        }
    }
}
