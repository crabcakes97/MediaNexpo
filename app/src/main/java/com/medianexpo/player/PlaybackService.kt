package com.medianexpo.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlin.math.hypot

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private var audioVisualizer: Visualizer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    companion object {
        var instance: PlaybackService? = null

        @Volatile
        var latestFftData = FloatArray(20)
        var eqEnabled = true
        var gainMb = 0
        var bassStrength: Short = 500
        var virtualizerStrength: Short = 500
        // Stored in millibels (−1500..1500 typical)
        val bandLevels = ShortArray(5) { 0 }

        fun updateBassBoost(strength: Short) {
            bassStrength = strength.coerceIn(0, 1000)
            try {
                instance?.bassBoost?.setStrength(bassStrength)
                instance?.bassBoost?.enabled = true
            } catch (e: Exception) {
                Log.e("PlaybackService", "BassBoost update failed", e)
            }
        }

        fun updateVirtualizer(strength: Short) {
            virtualizerStrength = strength.coerceIn(0, 1000)
            try {
                instance?.virtualizer?.setStrength(virtualizerStrength)
                instance?.virtualizer?.enabled = true
            } catch (e: Exception) {
                Log.e("PlaybackService", "Virtualizer update failed", e)
            }
        }

        fun updateGain(gain: Int) {
            gainMb = gain.coerceIn(0, 3000)
            try {
                instance?.loudnessEnhancer?.setTargetGain(gainMb)
                instance?.loudnessEnhancer?.enabled = true
            } catch (e: Exception) {
                Log.e("PlaybackService", "Gain update failed", e)
            }
        }

        fun updateEqEnabled(enabled: Boolean) {
            eqEnabled = enabled
            try {
                val svc = instance
                // Master switch: EQ + bass + virtualizer + pre-amp gain
                svc?.equalizer?.enabled = enabled
                svc?.bassBoost?.enabled = enabled
                svc?.virtualizer?.enabled = enabled
                svc?.loudnessEnhancer?.enabled = enabled
                if (enabled) {
                    svc?.applyAllBands()
                    try { svc?.bassBoost?.setStrength(bassStrength) } catch (_: Exception) {}
                    try { svc?.virtualizer?.setStrength(virtualizerStrength) } catch (_: Exception) {}
                    try { svc?.loudnessEnhancer?.setTargetGain(gainMb) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e("PlaybackService", "Effects enable failed", e)
            }
        }

        fun updateBand(band: Int, level: Short) {
            if (band !in bandLevels.indices) return
            bandLevels[band] = level
            try {
                val eq = instance?.equalizer ?: return
                val range = eq.bandLevelRange // [min, max] in mB
                val clamped = level.coerceIn(range[0], range[1])
                bandLevels[band] = clamped
                eq.setBandLevel(band.toShort(), clamped)
                if (!eq.enabled && eqEnabled) {
                    eq.enabled = true
                }
            } catch (e: Exception) {
                Log.e("PlaybackService", "Band $band update failed", e)
            }
        }
    }

    private fun applyAllBands() {
        val eq = equalizer ?: return
        try {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            for (i in 0 until minOf(bands, bandLevels.size)) {
                val clamped = bandLevels[i].coerceIn(range[0], range[1])
                eq.setBandLevel(i.toShort(), clamped)
            }
            eq.enabled = eqEnabled
        } catch (e: Exception) {
            Log.e("PlaybackService", "applyAllBands failed", e)
        }
    }

    private fun initAudioEffects(sessionId: Int) {
        // Skip if already attached to this session — recreating kills live EQ adjustments
        if (sessionId == currentSessionId && equalizer != null) {
            applyAllBands()
            return
        }

        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()

            // Priority 0 can lose to system effects; use a modest non-zero priority
            val priority = 1

            equalizer = Equalizer(priority, sessionId).apply {
                enabled = eqEnabled
                val bands = numberOfBands.toInt()
                val range = bandLevelRange
                Log.d(
                    "PlaybackService",
                    "EQ bands=$bands range=${range[0]}..${range[1]} mB session=$sessionId"
                )
                for (i in 0 until minOf(bands, bandLevels.size)) {
                    val clamped = bandLevels[i].coerceIn(range[0], range[1])
                    setBandLevel(i.toShort(), clamped)
                }
            }

            bassBoost = BassBoost(priority, sessionId).apply {
                enabled = true
                setStrength(bassStrength)
            }

            virtualizer = Virtualizer(priority, sessionId).apply {
                enabled = true
                setStrength(virtualizerStrength)
            }

            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = true
                setTargetGain(gainMb)
            }

            currentSessionId = sessionId
            Log.d("PlaybackService", "Audio effects initialized on session $sessionId")
        } catch (e: Exception) {
            Log.e("PlaybackService", "Failed to initialize audio effects", e)
        }
    }

    private fun initVisualizer(sessionId: Int) {
        try {
            audioVisualizer?.apply {
                enabled = false
                release()
            }

            audioVisualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0].coerceAtLeast(128)
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED

                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform == null || !player.isPlaying) return
                            val mags = FloatArray(20)
                            val step = (waveform.size / 20).coerceAtLeast(1)
                            for (i in 0 until 20) {
                                val idx = (i * step).coerceIn(0, waveform.size - 1)
                                val sample = (waveform[idx].toInt() and 0xFF) - 128
                                mags[i] = (kotlin.math.abs(sample) / 128f).coerceIn(0.05f, 1f)
                            }
                            latestFftData = mags
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || !player.isPlaying) return
                            val mags = FloatArray(20)
                            for (i in 0 until 20) {
                                val reIdx = (2 * i).coerceIn(0, fft.size - 1)
                                val imIdx = (2 * i + 1).coerceIn(0, fft.size - 1)
                                val real = fft[reIdx].toFloat()
                                val imag = fft[imIdx].toFloat()
                                mags[i] = (hypot(real, imag) / 128f).coerceIn(0.05f, 1f)
                            }
                            latestFftData = mags
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2, // release: low CPU
                    false,
                    true
                )
                enabled = true
            }
            Log.d("PlaybackService", "Visualizer attached to session $sessionId")
        } catch (e: Exception) {
            Log.e("PlaybackService", "Visualizer failed", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    initAudioEffects(audioSessionId)
                    initVisualizer(audioSessionId)
                }
            }
        })

        // Attach once if session already exists
        val sid = player.audioSessionId
        if (sid != C.AUDIO_SESSION_ID_UNSET && sid != 0) {
            initAudioEffects(sid)
            initVisualizer(sid)
        }

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        instance = null
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
        try {
            audioVisualizer?.apply { enabled = false; release() }
            audioVisualizer = null
            equalizer?.apply { enabled = false; release() }
            equalizer = null
            bassBoost?.apply { enabled = false; release() }
            bassBoost = null
            virtualizer?.apply { enabled = false; release() }
            virtualizer = null
            loudnessEnhancer?.apply { enabled = false; release() }
            loudnessEnhancer = null
            mediaSession?.run {
                player.release()
                release()
            }
            mediaSession = null
        } catch (e: Exception) {
            Log.e("PlaybackService", "Cleanup failed", e)
        }
        super.onDestroy()
    }
}
