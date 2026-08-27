package com.example.ludo.core.logging

import android.util.Log

/**
 * Structured Logging & Observability for Ludo.
 * Provides leveled logging, performance metric tracking, and diagnostic telemetry.
 */
object AppLogger {
    private const val DEFAULT_TAG = "LudoApp"
    var isLoggingEnabled: Boolean = true

    fun d(tag: String = DEFAULT_TAG, message: () -> String) {
        if (isLoggingEnabled) {
            try {
                Log.d(tag, message())
            } catch (_: Exception) {
                println("DEBUG [$tag]: ${message()}")
            }
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: () -> String) {
        if (isLoggingEnabled) {
            try {
                Log.i(tag, message())
            } catch (_: Exception) {
                println("INFO [$tag]: ${message()}")
            }
        }
    }

    fun w(tag: String = DEFAULT_TAG, message: () -> String) {
        if (isLoggingEnabled) {
            try {
                Log.w(tag, message())
            } catch (_: Exception) {
                println("WARN [$tag]: ${message()}")
            }
        }
    }

    fun w(tag: String, throwable: Throwable?, message: () -> String) {
        if (isLoggingEnabled) {
            try {
                if (throwable != null) {
                    Log.w(tag, message(), throwable)
                } else {
                    Log.w(tag, message())
                }
            } catch (_: Exception) {
                println("WARN [$tag]: ${message()}")
                throwable?.printStackTrace()
            }
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: () -> String) {
        if (isLoggingEnabled) {
            try {
                Log.e(tag, message())
            } catch (_: Exception) {
                System.err.println("ERROR [$tag]: ${message()}")
            }
        }
    }

    fun e(tag: String, throwable: Throwable?, message: () -> String) {
        if (isLoggingEnabled) {
            try {
                if (throwable != null) {
                    Log.e(tag, message(), throwable)
                } else {
                    Log.e(tag, message())
                }
            } catch (_: Exception) {
                System.err.println("ERROR [$tag]: ${message()}")
                throwable?.printStackTrace()
            }
        }
    }

    /**
     * Measure execution time of a critical block for performance diagnostics.
     */
    inline fun <T> measureTrace(name: String, block: () -> T): T {
        val start = System.nanoTime()
        return try {
            block()
        } finally {
            val durationMs = (System.nanoTime() - start) / 1_000_000.0
            if (durationMs > 16.0) { // Flag frame drop threshold (>16ms)
                w("PerformanceTrace") { "⚠️ SLOW OPERATION: $name took ${String.format("%.2f", durationMs)}ms" }
            } else {
                d("PerformanceTrace") { "$name completed in ${String.format("%.2f", durationMs)}ms" }
            }
        }
    }
}
