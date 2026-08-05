# MediaNexpo

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Open%20Source-orange.svg)](#license)

**MediaNexpo** is an offline-first Android media player for music, videos, photos, and books. Built with Jetpack Compose and Media3 (ExoPlayer). No accounts, no cloud, no tracking.

Created by **RomLord14495 (Kyle)** / [crabcakes97](https://github.com/crabcakes97).

---

## Features

### Audio
- Local playback for common formats (MP3, FLAC, M4A, WAV, OGG, AAC, and more)
- Gapless playback and smooth fade transitions
- 5-band equalizer with bass boost, virtualizer, and pre-amp gain
- Multiple real-time FFT visualizers with beat response
- Shuffle, repeat, seek, speed, and pitch controls
- DJ scratch pad
- Resume positions for songs and audiobooks
- Custom playlists, favorites, recently played
- Mood tags generated while listening
- Optional shake-to-skip with adjustable sensitivity
- Synced lyrics when available

### Video and photos
- Folder-style video browser
- Gesture controls: brightness, volume, and scrub
- Playback speed controls
- Photo library with fullscreen viewer

### Books
- EPUB reader
- Audiobook folders with chapter playback and resume

### Privacy and sharing
- Private vault with PIN (songs, videos, photos)
- Wi-Fi Direct peer-to-peer sharing without a router
- Local HTTP share for same-network browsers

### Customization
- Material You (wallpaper colors) or fixed accent palettes (55 themes)
- Dark, Light, System, and true AMOLED black
- Spinning album art and optional neon edge lighting
- Focus mode for an expanded now-playing view
- Quick Settings play/pause tile

---

## Tech stack

| Area | Stack |
|------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 / ExoPlayer |
| Async | Coroutines |
| Visualizers | Compose Canvas + Android Visualizer FFT |

---

## Requirements

- Android 7.0 (API 24) or higher
- Android Studio Ladybug or newer (for building from source)
- JDK 17

---

## Build

```bash
git clone https://github.com/crabcakes97/MediaNexpo.git
cd MediaNexpo
./gradlew assembleDebug
