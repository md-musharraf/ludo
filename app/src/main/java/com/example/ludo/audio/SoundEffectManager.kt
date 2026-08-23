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
                
                // Release after playing
                kotlinx.coroutines.delay((samples.size * 1000L / SAMPLE_RATE) + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio errors gracefully
            }
        }
    }

    fun playDiceRoll() {
        // Quick rattle / clatter
        val durationMs = 350
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 200.0 + (i % 800) * 0.5 + 50.0 * sin(2.0 * Math.PI * 30.0 * t)
            val envelope = (1.0 - t / (durationMs / 1000.0)).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.6 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playTokenStep() {
        // Crisp pop / click
        val durationMs = 80
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 600.0 + (1.0 - t / 0.08) * 400.0
            val envelope = (1.0 - t / 0.08).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.7 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playLeaveBase() {
        // Upward pleasant chime
        val durationMs = 250
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = if (t < 0.12) 523.25 else 783.99 // C5 -> G5
            val envelope = (1.0 - (t % 0.12) / 0.12).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.8 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playSixRolled() {
        // Triumphant 3-note sparkle
        val durationMs = 400
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        val freqs = doubleArrayOf(587.33, 739.99, 880.0) // D5, F#5, A5
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIndex = (t / 0.12).toInt().coerceIn(0, 2)
            val freq = freqs[noteIndex]
            val localT = t - noteIndex * 0.12
            val envelope = (1.0 - localT / 0.15).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.7 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playCapture() {
        // Crunchy capture / impact sound
        val durationMs = 400
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 450.0 * (1.0 - t / 0.4) + 80.0
            val noise = (Math.random() * 2.0 - 1.0) * 0.3
            val envelope = (1.0 - t / 0.4).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + noise
            samples[i] = (wave * 0.8 * envelope * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    fun playWinFanfare() {
        // Joyful victory fanfare
        val durationMs = 900
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIndex = (t / 0.2).toInt().coerceIn(0, 3)
            val freq = freqs[noteIndex]
            val localT = t - noteIndex * 0.2
            val envelope = (1.0 - localT / 0.25).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.3 * sin(4.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.7 * envelope * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playPcm(samples)
    }

    fun playButtonTap() {
        val durationMs = 40
        val totalSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 800.0
            val envelope = (1.0 - t / 0.04).coerceIn(0.0, 1.0)
            val wave = sin(2.0 * Math.PI * freq * t)
            samples[i] = (wave * 0.4 * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }
}
