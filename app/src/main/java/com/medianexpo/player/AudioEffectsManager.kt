package com.medianexpo.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

class AudioEffectsManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    companion object {

        var eqEnabled = true

        var gainMb = 0

        var bassStrength: Short = 500

        var virtualizerStrength: Short = 500

        val bandLevels = ShortArray(5) { 0 }

    }

    fun attach(audioSessionId: Int) {

        release()

        try {

            equalizer = Equalizer(0, audioSessionId).apply {

                enabled = eqEnabled

                val bands = numberOfBands.toInt()

                for (i in 0 until minOf(bands, bandLevels.size)) {

                    setBandLevel(i.toShort(), bandLevels[i])

                }

            }

            bassBoost = BassBoost(0, audioSessionId).apply {

                enabled = true

                setStrength(bassStrength)

            }

            virtualizer = Virtualizer(0, audioSessionId).apply {

                enabled = true

                setStrength(virtualizerStrength)

            }

            loudness = LoudnessEnhancer(audioSessionId).apply {

                enabled = true

                setTargetGain(gainMb)

            }

            Log.d("AudioEffects", "Attached to session $audioSessionId")

        } catch (e: Exception) {

            Log.e("AudioEffects", "Unable to attach", e)

        }

    }

    fun setBand(index: Int, level: Short) {

        bandLevels[index] = level

        equalizer?.setBandLevel(index.toShort(), level)

    }

    fun setGain(mb: Int) {

        gainMb = mb

        loudness?.setTargetGain(mb)

    }

    fun setBass(strength: Short) {

        bassStrength = strength

        bassBoost?.setStrength(strength)

    }

    fun setVirtualizer(strength: Short) {

        virtualizerStrength = strength

        virtualizer?.setStrength(strength)

    }

    fun release() {

        equalizer?.release()

        bassBoost?.release()

        virtualizer?.release()

        loudness?.release()

        equalizer = null

        bassBoost = null

        virtualizer = null

        loudness = null

    }

}
