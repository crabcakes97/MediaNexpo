package com.medianexpo.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
<<<<<<< HEAD
=======
import androidx.media3.common.MediaItem
>>>>>>> 2c4ab1d (Initial commit after project recovery)
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
    private var prevBass = 0f
    private val runningAvg = FloatArray(32)
    private val history = mutableListOf<Float>()

    companion object {
        var instance: PlaybackService? = null

<<<<<<< HEAD
=======
        const val CMD_PLAY = "com.medianexpo.player.CMD_PLAY"
        const val CMD_PAUSE = "com.medianexpo.player.CMD_PAUSE"
        const val CMD_TOGGLE = "com.medianexpo.player.CMD_TOGGLE"
        const val CMD_NEXT = "com.medianexpo.player.CMD_NEXT"
        const val CMD_PREV = "com.medianexpo.player.CMD_PREV"
        const val CMD_STOP = "com.medianexpo.player.CMD_STOP"

>>>>>>> 2c4ab1d (Initial commit after project recovery)
        @Volatile
        var latestFftData = FloatArray(32)
        @Volatile
        var latestPcm: ByteArray = ByteArray(0)
        /** 0..1 kick envelope — spikes on bass hits */
        @Volatile
        var beatPulse: Float = 0f
<<<<<<< HEAD
=======
        @Volatile
        var isPlayingNow: Boolean = false
>>>>>>> 2c4ab1d (Initial commit after project recovery)
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
<<<<<<< HEAD
                enabled = false
                release()
            }

            audioVisualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0].coerceAtLeast(128)
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED

=======
                try { enabled = false } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
            audioVisualizer = null

            if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return

            val range = Visualizer.getCaptureSizeRange()
            // Prefer a mid/high capture size for clearer frequency bins
            val size = (range[1]).coerceAtMost(1024).coerceAtLeast(range[0].coerceAtLeast(256))

            audioVisualizer = Visualizer(sessionId).apply {
                captureSize = size
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
<<<<<<< HEAD
                            if (waveform == null) return
                            PlaybackService.latestPcm = waveform.copyOf()
=======
                            if (waveform != null) PlaybackService.latestPcm = waveform.copyOf()
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
<<<<<<< HEAD
                            if (fft == null || !player.isPlaying) return
                            val bins = 32
                            val mags = FloatArray(bins)
                            val usable = (fft.size / 2 - 2).coerceAtLeast(bins)
                            for (i in 0 until bins) {
                                val src = 1 + (i * usable / bins)
                                val reIdx = (2 * src).coerceIn(0, fft.size - 1)
                                val imIdx = (2 * src + 1).coerceIn(0, fft.size - 1)
                                val mag = hypot(fft[reIdx].toFloat(), fft[imIdx].toFloat())
                                val weight = 1.2f - (i / bins.toFloat()) * 0.35f
                                val norm = (mag * weight / 90f).coerceIn(0f, 1.4f)
                                mags[i] = (0.06f + norm * 0.94f).coerceIn(0.06f, 1f)
                            }
                            // Spectral flux across ALL bins = onsets anywhere in the mix
                            // (kick, snare, hats, synth hits — not bass-only)
                            var flux = 0f
                            for (i in 0 until bins) {
                                val d = mags[i] - runningAvg[i]
                                if (d > 0f) flux += d
                                // Slow envelope follow so flux measures true onsets
                                runningAvg[i] = runningAvg[i] * 0.85f + mags[i] * 0.15f
                            }
                            flux /= bins
                            val energy = mags.average().toFloat()
                            // Short history for adaptive threshold
                            history.add(flux)
                            if (history.size > 20) history.removeAt(0)
                            val mean = history.average().toFloat()
                            val thr = mean * 1.25f + 0.02f
                            val onset = if (flux > thr) ((flux - thr) / (thr + 0.05f)).coerceIn(0f, 1f) else 0f
                            // Quick attack on onset, steady decay between hits
                            val pulse = if (onset > 0.05f) {
                                (PlaybackService.beatPulse * 0.4f + onset * 0.85f + energy * 0.1f).coerceIn(0f, 1f)
                            } else {
                                (PlaybackService.beatPulse * 0.82f + energy * 0.08f).coerceIn(0f, 1f)
                            }
                            prevBass = energy
=======
                            if (fft == null) return
                            // Keep last frame when paused so UI does not hard-blank mid transition
                            if (!player.isPlaying) {
                                PlaybackService.beatPulse *= 0.85f
                                return
                            }

                            val bins = 32
                            val mags = FloatArray(bins)
                            // fft[0]/fft[1] are DC; pairs (re,im) after that
                            val maxPairs = (fft.size / 2 - 1).coerceAtLeast(bins)
                            var peak = 1f
                            val raw = FloatArray(bins)
                            for (i in 0 until bins) {
                                // log-ish spacing: more resolution in lower-mids, still cover highs
                                val t = (i + 1).toFloat() / bins
                                val src = (1 + (t * t * maxPairs)).toInt().coerceIn(1, maxPairs)
                                val reIdx = (2 * src).coerceIn(0, fft.size - 1)
                                val imIdx = (2 * src + 1).coerceIn(0, fft.size - 1)
                                val re = fft[reIdx].toFloat()
                                val im = fft[imIdx].toFloat()
                                val mag = hypot(re, im)
                                raw[i] = mag
                                if (mag > peak) peak = mag
                            }
                            // Adaptive normalize so every track fills the bars
                            val denom = peak.coerceAtLeast(8f)
                            for (i in 0 until bins) {
                                val n = (raw[i] / denom).coerceIn(0f, 1f)
                                // slight bass emphasis without killing highs
                                val weight = 1.05f - (i / bins.toFloat()) * 0.25f
                                val smoothed = runningAvg[i] * 0.35f + (n * weight) * 0.65f
                                runningAvg[i] = smoothed
                                mags[i] = smoothed.coerceIn(0.04f, 1f)
                            }

                            // Onset / beat from full-spectrum energy jump
                            val energy = mags.average().toFloat()
                            val flux = (energy - prevBass).coerceAtLeast(0f)
                            prevBass = energy * 0.7f + prevBass * 0.3f
                            history.add(flux)
                            if (history.size > 16) history.removeAt(0)
                            val mean = if (history.isEmpty()) 0f else history.average().toFloat()
                            val thr = mean * 1.15f + 0.015f
                            val onset = if (flux > thr) ((flux - thr) / (thr + 0.04f)).coerceIn(0f, 1f) else 0f
                            val pulse = if (onset > 0.04f) {
                                (PlaybackService.beatPulse * 0.35f + onset * 0.9f + energy * 0.15f).coerceIn(0f, 1f)
                            } else {
                                (PlaybackService.beatPulse * 0.78f + energy * 0.18f).coerceIn(0f, 1f)
                            }
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                            PlaybackService.beatPulse = pulse
                            PlaybackService.latestFftData = mags
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
<<<<<<< HEAD
                    true,  // waveform for Lissajous
                    true   // fft
                )
                enabled = true
            }
            Log.d("PlaybackService", "Visualizer attached to session $sessionId")
        } catch (e: Exception) {
            Log.e("PlaybackService", "Visualizer failed", e)
=======
                    false,
                    true
                )
                enabled = true
            }
            currentSessionId = sessionId
            Log.d("PlaybackService", "Visualizer attached to session $sessionId size=$size")
        } catch (e: Exception) {
            Log.e("PlaybackService", "Visualizer failed", e)
            audioVisualizer = null
>>>>>>> 2c4ab1d (Initial commit after project recovery)
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
<<<<<<< HEAD
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Re-attach visualizer when playback actually starts (fixes "works only on adb" on some OEMs)
=======
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Track change often kills OEM visualizers — force rebind next play
                try { audioVisualizer?.enabled = false } catch (_: Exception) {}
                val sid = player.audioSessionId
                if (sid != C.AUDIO_SESSION_ID_UNSET && sid != 0) {
                    initAudioEffects(sid)
                    initVisualizer(sid)
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingNow = isPlaying
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                if (isPlaying) {
                    val sid = player.audioSessionId
                    if (sid != C.AUDIO_SESSION_ID_UNSET && sid != 0) {
                        if (audioVisualizer == null || currentSessionId != sid) {
                            initAudioEffects(sid)
                            initVisualizer(sid)
                        } else {
<<<<<<< HEAD
                            try { audioVisualizer?.enabled = true } catch (_: Exception) {}
=======
                            try {
                                audioVisualizer?.enabled = true
                            } catch (_: Exception) {
                                initVisualizer(sid)
                            }
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                        }
                    }
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

<<<<<<< HEAD
=======
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            CMD_PLAY -> try { player.play() } catch (_: Exception) {}
            CMD_PAUSE -> try { player.pause() } catch (_: Exception) {}
            CMD_TOGGLE -> try {
                if (player.isPlaying) player.pause() else player.play()
            } catch (_: Exception) {}
            CMD_NEXT -> try { if (player.hasNextMediaItem()) player.seekToNextMediaItem() } catch (_: Exception) {}
            CMD_PREV -> try { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() } catch (_: Exception) {}
            CMD_STOP -> try { player.stop() } catch (_: Exception) {}
        }
        return super.onStartCommand(intent, flags, startId)
    }

>>>>>>> 2c4ab1d (Initial commit after project recovery)
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
