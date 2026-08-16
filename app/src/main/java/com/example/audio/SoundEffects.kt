package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

object SoundEffects {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var isSfxMuted = false
    private var isMusicMuted = false
    
    private const val SAMPLE_RATE = 22050
    
    // Pre-allocated AudioTracks for zero allocation during gameplay
    private var bounceTrack: AudioTrack? = null
    private var rimTrack: AudioTrack? = null
    private var backboardTrack: AudioTrack? = null
    private var swishTrack: AudioTrack? = null
    private var scoreTrack: AudioTrack? = null
    private var streakTrack: AudioTrack? = null
    private var buzzerTrack: AudioTrack? = null
    private var whistleTrack: AudioTrack? = null
    private var whooshTrack: AudioTrack? = null

    // Background Music Engine
    private var musicJob: Job? = null
    private var isMusicPlaying = false
    private var bgmTrack: AudioTrack? = null

    init {
        scope.launch {
            initBuffers()
        }
    }

    private fun createStaticTrack(buffer: ShortArray): AudioTrack? {
        return try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack(
                audioAttributes,
                audioFormat,
                buffer.size * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(buffer, 0, buffer.size)
            track
        } catch (_: Exception) {
            null
        }
    }

    private fun initBuffers() {
        try {
            bounceTrack = createStaticTrack(generatePcm(freqStart = 150f, freqEnd = 60f, durationMs = 90, decay = 0.85f))
            rimTrack = createStaticTrack(generateDualTonePcm(f1 = 920f, f2 = 1450f, durationMs = 130, decay = 0.7f))
            backboardTrack = createStaticTrack(generatePcm(freqStart = 260f, freqEnd = 90f, durationMs = 110, decay = 0.8f))
            swishTrack = createStaticTrack(generateSwishPcm(durationMs = 200))
            scoreTrack = createStaticTrack(generateChordPcm(freqs = floatArrayOf(523.25f, 659.25f, 783.99f), durationMs = 220))
            streakTrack = createStaticTrack(generatePcm(freqStart = 650f, freqEnd = 1350f, durationMs = 280, decay = 0.4f))
            buzzerTrack = createStaticTrack(generateBuzzerPcm(durationMs = 600))
            whistleTrack = createStaticTrack(generateWhistlePcm(durationMs = 320))
            whooshTrack = createStaticTrack(generateWhooshPcm(durationMs = 110))
        } catch (_: Exception) {}
    }

    fun toggleSfx(): Boolean {
        isSfxMuted = !isSfxMuted
        return isSfxMuted
    }

    fun isSfxMuted(): Boolean = isSfxMuted

    fun toggleMusic(): Boolean {
        isMusicMuted = !isMusicMuted
        if (isMusicMuted) {
            stopMusic()
        } else {
            startMusic()
        }
        return isMusicMuted
    }

    fun isMusicMuted(): Boolean = isMusicMuted

    fun toggleMute(): Boolean {
        val newMuted = !isSfxMuted
        isSfxMuted = newMuted
        isMusicMuted = newMuted
        if (isMusicMuted) {
            stopMusic()
        } else {
            startMusic()
        }
        return newMuted
    }

    fun isAudioMuted(): Boolean = isSfxMuted && isMusicMuted

    // Sound FX Triggers with pre-allocated tracks
    fun playBounce() {
        if (isSfxMuted) return
        playStaticTrack(bounceTrack)
    }

    fun playRimHit() {
        if (isSfxMuted) return
        playStaticTrack(rimTrack)
    }

    fun playBackboard() {
        if (isSfxMuted) return
        playStaticTrack(backboardTrack)
    }

    fun playSwish() {
        if (isSfxMuted) return
        playStaticTrack(swishTrack)
    }

    fun playScore() {
        if (isSfxMuted) return
        playStaticTrack(scoreTrack)
    }

    fun playStreak() {
        if (isSfxMuted) return
        playStaticTrack(streakTrack)
    }

    fun playBuzzer() {
        if (isSfxMuted) return
        playStaticTrack(buzzerTrack)
    }

    fun playWhistle() {
        if (isSfxMuted) return
        playStaticTrack(whistleTrack)
    }

    fun playWhoosh() {
        if (isSfxMuted) return
        playStaticTrack(whooshTrack)
    }

    private fun playStaticTrack(track: AudioTrack?) {
        if (track == null) return
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.reloadStaticData()
            track.play()
        } catch (_: Exception) {}
    }

    // -------------------------------------------------------------
    // BACKGROUND MUSIC SYNTHESIZER ENGINE (Gymnasium Athletic Groove)
    // -------------------------------------------------------------
    fun startMusic() {
        if (isMusicMuted || isMusicPlaying) return
        isMusicPlaying = true
        musicJob?.cancel()
        musicJob = scope.launch(Dispatchers.Default) {
            playBgmLoop()
        }
    }

    fun stopMusic() {
        isMusicPlaying = false
        musicJob?.cancel()
        musicJob = null
        try {
            bgmTrack?.pause()
            bgmTrack?.flush()
            bgmTrack?.release()
            bgmTrack = null
        } catch (_: Exception) {}
    }

    private suspend fun playBgmLoop() = withContext(Dispatchers.Default) {
        val bpm = 124
        val beatMs = (60000 / bpm)
        val stepMs = beatMs / 4 // 16th notes ~ 120ms
        val barSteps = 16
        val patternBars = 2
        val totalSteps = barSteps * patternBars

        // Bassline notes frequencies (in Hz)
        val bassNotes = floatArrayOf(
            110.0f, 0f, 110.0f, 0f, 130.81f, 0f, 146.83f, 130.81f,
            110.0f, 0f, 164.81f, 0f, 146.83f, 130.81f, 123.47f, 110.0f,
            98.0f, 0f, 98.0f, 0f, 110.0f, 0f, 123.47f, 110.0f,
            130.81f, 0f, 146.83f, 0f, 164.81f, 146.83f, 130.81f, 123.47f
        )

        val totalSamples = (SAMPLE_RATE * ((stepMs * totalSteps) / 1000.0)).toInt()
        val bgmBuffer = ShortArray(totalSamples)

        val stepSamples = (SAMPLE_RATE * (stepMs / 1000.0)).toInt()

        for (step in 0 until totalSteps) {
            val stepStart = step * stepSamples
            val isKick = (step % 8 == 0) || (step % 8 == 6)
            val isSnare = (step % 8 == 4)
            val isHiHat = (step % 2 == 0)
            val bassFreq = bassNotes[step]

            for (i in 0 until stepSamples) {
                val idx = stepStart + i
                if (idx >= totalSamples) break
                val t = i.toDouble() / SAMPLE_RATE
                val prog = i.toDouble() / stepSamples
                var sample = 0.0

                // Kick drum (Low pitch drop)
                if (isKick) {
                    val kEnv = (1.0 - prog).coerceAtLeast(0.0).pow(4.0)
                    val kFreq = 120.0 * (1.0 - prog * 0.7)
                    sample += sin(2.0 * PI * kFreq * t) * kEnv * 0.45
                }

                // Snare drum (Mid punch + noise)
                if (isSnare) {
                    val sEnv = (1.0 - prog).coerceAtLeast(0.0).pow(3.0)
                    val noise = (kotlin.random.Random.nextDouble() * 2.0 - 1.0)
                    val sTone = sin(2.0 * PI * 210.0 * t)
                    sample += (sTone * 0.25 + noise * 0.35) * sEnv
                }

                // Hi-Hat tick
                if (isHiHat) {
                    val hEnv = (1.0 - (prog * 3.5).coerceAtMost(1.0)).coerceAtLeast(0.0).pow(5.0)
                    val noise = (kotlin.random.Random.nextDouble() * 2.0 - 1.0)
                    sample += noise * hEnv * 0.18
                }

                // Bassline synth
                if (bassFreq > 0f) {
                    val bEnv = (1.0 - prog * 0.65).coerceAtLeast(0.0).pow(1.5)
                    val bWave = sin(2.0 * PI * bassFreq * t) + 0.35 * sin(4.0 * PI * bassFreq * t)
                    sample += bWave * bEnv * 0.28
                }

                val finalVal = (sample * Short.MAX_VALUE * 0.50).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                bgmBuffer[idx] = finalVal.toShort()
            }
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            bgmTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                bgmBuffer.size * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            bgmTrack?.write(bgmBuffer, 0, bgmBuffer.size)
            bgmTrack?.setLoopPoints(0, bgmBuffer.size, -1) // Infinite smooth loop
            bgmTrack?.play()
        } catch (_: Exception) {
            isMusicPlaying = false
        }
    }

    // -------------------------------------------------------------
    // SYNTHESIZERS (Pure Math Waveforms)
    // -------------------------------------------------------------
    private fun generatePcm(freqStart: Float, freqEnd: Float, durationMs: Int, decay: Float): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val currentFreq = freqStart + (freqEnd - freqStart) * progress
            val envelope = (1.0 - progress).coerceAtLeast(0.0).pow(decay.toDouble() * 3.0 + 0.1)
            val wave = sin(2.0 * PI * currentFreq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateDualTonePcm(f1: Float, f2: Float, durationMs: Int, decay: Float): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress).coerceAtLeast(0.0).pow(decay.toDouble() * 3.0 + 0.2)
            val wave = 0.6 * sin(2.0 * PI * f1 * t) + 0.4 * sin(2.0 * PI * f2 * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.75).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateSwishPcm(durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val envelope = sin(PI * progress) * (1.0 - progress * 0.4)
            val tone = sin(2.0 * PI * (780.0 + 350.0 * progress) * t)
            val whisperNoise = (kotlin.random.Random.nextDouble() * 2.0 - 1.0) * 0.4
            val sample = (tone * 0.6 + whisperNoise) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateChordPcm(freqs: FloatArray, durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress).coerceAtLeast(0.0).pow(1.6)
            var wave = 0.0
            for (f in freqs) {
                wave += sin(2.0 * PI * f * t)
            }
            wave /= freqs.size
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateBuzzerPcm(durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        val freq = 220.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val envelope = if (progress > 0.85) (1.0 - (progress - 0.85) / 0.15) else 1.0
            val squareWave = if (sin(2.0 * PI * freq * t) >= 0) 0.8 else -0.8
            buffer[i] = (squareWave * envelope * Short.MAX_VALUE * 0.65).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateWhistlePcm(durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val tremolo = 1.0 + 0.15 * sin(2.0 * PI * 28.0 * t)
            val envelope = (1.0 - progress).coerceAtLeast(0.0).pow(0.5)
            val wave = 0.6 * sin(2.0 * PI * 2250.0 * t * tremolo) + 0.4 * sin(2.0 * PI * 2420.0 * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateWhooshPcm(durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val freq = 420.0 - 280.0 * progress
            val envelope = sin(PI * progress)
            val noise = (kotlin.random.Random.nextDouble() * 2.0 - 1.0) * 0.35
            val wave = (sin(2.0 * PI * freq * t) * 0.65 + noise) * envelope
            buffer[i] = (wave * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun Double.pow(exp: Double): Double = Math.pow(this, exp)
}
