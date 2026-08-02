MediaNexpo — drop-in features

Copy into: app/src/main/java/com/medianexpo/player/

NEW FILES:
  RecentlyPlayedStore.kt  — last 100 played tracks
  LyricsRepository.kt     — SongName.lrc next to audio
  EqPresets.kt            — 12 EQ presets
  SoundMomentsStore.kt    — UNIQUE: save timestamp + note mid-song, jump back later

REPLACE:
  MainActivity.kt
  PlaybackService.kt
  EpubReaderActivity.kt
  AndroidManifest.xml
  BookRepository.kt (Books tab)

WHERE TO FIND THEM
  Settings → Audio Effects     EQ presets + band sliders
  Settings → Playback          Gapless, fade, sleep timer
  Now Playing → Advanced Tools (scrollable, max height)
    A-B loop, Sound Moments, spinning art, lyrics, DJ scratch, speed & pitch
  Recent tab                   play history

SOUND MOMENTS (feature most players don't have)
  Open Advanced Tools while playing → Capture moment @ current time
  Optional note (e.g. "the drop", "favorite verse")
  Tap a saved moment later to seek to that exact second

LYRICS
  Place MySong.lrc next to MySong.mp3
  [00:12.50]Hello world
