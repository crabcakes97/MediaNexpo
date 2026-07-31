# MediaNexpo 🎵🎥

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Open%20Source-orange.svg)](#license)

**MediaNexpo** is a powerful, lightweight, open-source local audio and video player for Android. Built using modern Jetpack Compose and Android Media3 (ExoPlayer), it combines a clean, gallery-style media browser with high-performance audio processing, intuitive video gestures, custom local Wi-Fi streaming, and zero cloud dependency.

Created with ❤️ by **RomLord14495 (Kyle)** from XDA.

---

## ✨ Key Features

### 🎵 Advanced Audio Player
* **High-Performance Audio Engine:** Built on top of AndroidX Media3 / ExoPlayer for seamless local file playback (`.mp3`, `.flac`, `.m4a`, `.wav`, `.ogg`, `.aac`, etc.).
* **Smart Artwork & Metadata Extraction:** Reads embedded cover art and tags via `MediaMetadataRetriever` without blocking the main UI thread.
* **Audiobook & Podcast Support:** Remembers last-played positions with smart resume functionality.
* **Custom Playlists:** Create, view, and organize custom playlists on the fly.
* **Minimizable Player:** Expand or collapse the now-playing drawer with real-time progress sliders, remaining time indicators, shuffle, and repeat modes.

### 🎥 Gallery-Style Video Browser & Gesture Controls
* **Gallery Folder Grid:** Replaces flat vertical lists with an intuitive 2-column grid displaying folder cards, video counts, and total file sizes.
* **Intuitive Touch Gestures:**
  * **Left Screen Swipe:** Smooth vertical brightness adjustment.
  * **Right Screen Swipe:** Vertical system volume control.
  * **Horizontal Drag:** Precise scrubbing and time seeking.
* **Pitch & Speed Adjustment:** Full variable speed and pitch controls for studying, language learning, or fast-forwarding content.

### 📊 Real-Time Visualizers & Custom DSP / Equalizer
* **5-Band Graphic Equalizer:** Granular frequency adjustments (`60Hz`, `230Hz`, `910Hz`, `3.6kHz`, `14kHz`) with a one-tap Flat Reset.
* **Audio Enhancements:** Includes adjustable **Bass Boost**, **Virtualizer**, and **Pre-amp Gain** controls.
* **8 Dynamic FFT Visualizers:** Real-time audio spectrum rendering built on custom Canvas scopes:
  1. 📊 *Spectrum Bars*
  2. 🌊 *Waveform*
  3. ⭕ *Pulse Circle*
  4. 🪞 *Mirrored Spectrum*
  5. 🟦 *Dot Matrix*
  6. 📡 *Circular Radar*
  7. 🎗️ *Neon Ribbon*
  8. ✨ *Particle Ring*

### 🌐 Local Wi-Fi Sharing & Web Streaming
* **Wi-Fi Direct Support:** Peer-to-peer file sharing directly between devices without an active internet connection.
* **Embedded Web Access:** Built-in lightweight local web server allowing cross-device web browser access and local network streaming.

### 🎨 Deep Customization & UI Polish
* **20 Color Theme Palettes:** Choose between AMOLED Black, Cyber Neon, Dark Purple, Dracula Gothic, Solarized Amber, Cyberpunk, Lava Orange, and more.
* **Theme Modes:** Full support for Dark Mode, Light Mode, and System Default.
* **Unified Search:** Instantly query across **Songs, Artists, Genres, Playlists, and Videos** simultaneously.
* **Storage Freedom:** Supports both Android MediaStore scanning and deep directory indexing via Storage Access Framework (SAF).

---

## 🛠️ Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material3
* **Media Engine:** [AndroidX Media3 / ExoPlayer](https://developer.android.com/media/media3)
* **Concurrency:** Kotlin Coroutines & Flow
* **Graphics:** Jetpack Compose Canvas API

---

## 🚀 Building & Installation

### Prerequisites
* Android Studio Ladybug or newer
* Android SDK 24+ (Android 7.0 Nougat or higher)
* JDK 17

### Build via Command Line

1. **Clone the Repository:**
   ```bash
   git clone [https://github.com/crabcakes97/MediaNexpo.git](https://github.com/crabcakes97/MediaNexpo.git)
   cd MediaNexpo
