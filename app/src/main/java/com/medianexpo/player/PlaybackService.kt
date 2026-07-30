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

    companion object {
        var instance: PlaybackService? = null

        @Volatile
        var latestFftData = FloatArray(20)
        var eqEnabled = true
        var gainMb = 0
        var bassStrength: Short = 500
        var virtualizerStrength: Short = 500
        val bandLevels = ShortArray(5) { 0 }

        fun updateBassBoost(strength: Short) {
            bassStrength = strength
            instance?.bassBoost?.setStrength(strength)
        }

        fun updateVirtualizer(strength: Short) {
            virtualizerStrength = strength
            instance?.virtualizer?.setStrength(strength)
        }

        fun updateGain(gain: Int) {
            gainMb = gain
            instance?.loudnessEnhancer?.setTargetGain(gain)
        }

        fun updateEqEnabled(enabled: Boolean) {
            eqEnabled = enabled
            instance?.equalizer?.enabled = enabled
        }

        fun updateBand(band: Int, level: Short) {
            bandLevels[band] = level
            instance?.equalizer?.setBandLevel(band.toShort(), level)
        }
    }

    private fun initAudioEffects(sessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()

            equalizer = Equalizer(0, sessionId).apply {
                enabled = eqEnabled
                val bands = numberOfBands.toInt()
                for (i in 0 until minOf(bands, bandLevels.size)) {
                    setBandLevel(i.toShort(), bandLevels[i])
                }
            }

            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
                setStrength(bassStrength)
            }

            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = true
                setStrength(virtualizerStrength)
            }

            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = true
                setTargetGain(gainMb)
            }

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
                        override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
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

                        override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft == null || !player.isPlaying) return
                            val mags = FloatArray(20)
                            for (i in 0 until 20) {
                                val real = fft[2 * i].toFloat()
                                val imag = fft[2 * i + 1].toFloat()
                                mags[i] = (hypot(real, imag) / 128f).coerceIn(0.05f, 1f)
                            }
                            latestFftData = mags
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
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
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) return
                val sessionId = player.audioSessionId
                if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                    initAudioEffects(sessionId)
                    initVisualizer(sessionId)
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        instance = null
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
