package com.example.ludo.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundEffectManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundEnabled: Boolean = true

    private const val SAMPLE_RATE = 44100

    private fun playPcm(samples: ShortArray) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()

                kotlinx.coroutines.delay((samples.size * 1000L / SAMPLE_RATE) + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio errors gracefully
            }
        }
    }

    fun playDiceRoll() {
        // Futuristic cybernetic dice tumble sound
        val durationMs = 400
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 220.0 + (i % 600) * 0.8 + 80.0 * sin(2.0 * Math.PI * 45.0 * t)
            val envelope = (1.0 - t / (durationMs / 1000.0)).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.3 * sin(4.0 * Math.PI * freq * 1.5 * t)
            samples[i] = (wave * 0.65 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playTokenStep() {
        // Crisp futuristic neon blip
        val durationMs = 70
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 700.0 + (1.0 - t / 0.07) * 500.0
            val envelope = (1.0 - t / 0.07).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.7 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playLeaveBase() {
        // Upward energetic cyber portal chime
        val durationMs = 280
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = if (t < 0.14) 587.33 else 880.0 // D5 -> A5
            val envelope = (1.0 - (t % 0.14) / 0.14).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.2 * sin(4.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.8 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playSixRolled() {
        // Triumphant 3-note futuristic sparkle
        val durationMs = 420
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        val freqs = doubleArrayOf(587.33, 739.99, 1046.50) // D5, F#5, C6
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIndex = (t / 0.13).toInt().coerceIn(0, 2)
            val freq = freqs[noteIndex]
            val localT = t - noteIndex * 0.13
            val envelope = (1.0 - localT / 0.16).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.25 * sin(2.0 * Math.PI * freq * 2.0 * t)
            samples[i] = (wave * 0.75 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playCapture() {
        // Powerful cybernetic impact explosion
        val durationMs = 450
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 500.0 * (1.0 - t / 0.45) + 60.0
            val noise = (Math.random() * 2.0 - 1.0) * 0.35
            val envelope = (1.0 - t / 0.45).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + noise
            samples[i] = (wave * 0.85 * envelope * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    fun playLadderClimb() {
        // Ascending 4-note beam ascension chime
        val durationMs = 500
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIndex = (t / 0.12).toInt().coerceIn(0, 3)
            val freq = freqs[noteIndex]
            val localT = t - noteIndex * 0.12
            val envelope = (1.0 - localT / 0.15).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.3 * sin(2.0 * Math.PI * freq * 2.0 * t)
            samples[i] = (wave * 0.75 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playSnakeSlide() {
        // Descending slithering glissando
        val durationMs = 550
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / (durationMs / 1000.0)
            val freq = 880.0 * (1.0 - progress * 0.7) + 100.0 * sin(2.0 * Math.PI * 25.0 * t)
            val envelope = (1.0 - progress * 0.5).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.2 * (Math.random() * 2.0 - 1.0)
            samples[i] = (wave * 0.7 * envelope * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    fun playWinFanfare() {
        // Grand victory fanfare
        val durationMs = 1000
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51) // C5, E5, G5, C6, E6
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIndex = (t / 0.18).toInt().coerceIn(0, 4)
            val freq = freqs[noteIndex]
            val localT = t - noteIndex * 0.18
            val envelope = (1.0 - localT / 0.25).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.3 * sin(4.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.75 * envelope * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    fun playButtonTap() {
        val durationMs = 35
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 900.0
            val envelope = (1.0 - t / 0.035).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.4 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }
}
