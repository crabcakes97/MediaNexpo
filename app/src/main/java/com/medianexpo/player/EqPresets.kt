package com.medianexpo.player

/**
 * Drop-in EQ presets for PlaybackService band levels (5 bands, millibels).
 * Apply with [apply] — updates companion state and live effects.
 */
object EqPresets {

    data class Preset(
        val name: String,
        val bands: ShortArray, // size 5, mB typically -1500..1500
        val bass: Short = 500,
        val virtualizer: Short = 300,
        val gainMb: Int = 0
    ) {
        override fun equals(other: Any?): Boolean =
            other is Preset && name == other.name && bands.contentEquals(other.bands)
        override fun hashCode(): Int = name.hashCode()
    }

    val all: List<Preset> = listOf(
        Preset("Flat", shortArrayOf(0, 0, 0, 0, 0), bass = 0, virtualizer = 0, gainMb = 0),
        Preset("Bass Boost", shortArrayOf(800, 400, 0, -100, -200), bass = 750, virtualizer = 200),
        Preset("Treble Boost", shortArrayOf(-200, -100, 100, 500, 800), bass = 200, virtualizer = 400),
        Preset("Vocal", shortArrayOf(-300, 200, 600, 400, 0), bass = 200, virtualizer = 150),
        Preset("Electronic", shortArrayOf(500, 200, -200, 300, 600), bass = 600, virtualizer = 500),
        Preset("Rock", shortArrayOf(400, 200, -100, 200, 400), bass = 550, virtualizer = 350),
        Preset("Hip-Hop", shortArrayOf(700, 350, 0, 150, 300), bass = 800, virtualizer = 400),
        Preset("Jazz", shortArrayOf(200, 0, 200, 300, 200), bass = 300, virtualizer = 200),
        Preset("Classical", shortArrayOf(0, 0, 0, 200, 300), bass = 150, virtualizer = 100),
        Preset("Loudness", shortArrayOf(500, 0, -200, 0, 400), bass = 400, virtualizer = 200, gainMb = 400),
        Preset("Night", shortArrayOf(200, 100, 0, -200, -400), bass = 300, virtualizer = 100, gainMb = -200),
        Preset("Podcast", shortArrayOf(-400, 300, 700, 400, -100), bass = 100, virtualizer = 0, gainMb = 200)
    )

    fun apply(preset: Preset) {
        for (i in preset.bands.indices) {
            if (i < PlaybackService.bandLevels.size) {
                PlaybackService.updateBand(i, preset.bands[i])
            }
        }
        PlaybackService.updateBassBoost(preset.bass)
        PlaybackService.updateVirtualizer(preset.virtualizer)
        PlaybackService.updateGain(preset.gainMb)
        PlaybackService.updateEqEnabled(true)
    }
}
