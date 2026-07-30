package com.medianexpo.player

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

val IconShuffle: ImageVector
    get() = ImageVector.Builder(name = "Shuffle", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(10.59f, 9.17f)
            lineTo(5.41f, 4.0f)
            lineTo(4.0f, 5.41f)
            lineTo(9.17f, 10.58f)
            lineTo(10.59f, 9.17f)
            close()
            moveTo(14.5f, 4.0f)
            lineTo(16.54f, 6.04f)
            lineTo(4.0f, 18.59f)
            lineTo(5.41f, 20.0f)
            lineTo(17.96f, 7.46f)
            lineTo(20.0f, 9.5f)
            lineTo(20.0f, 4.0f)
            lineTo(14.5f, 4.0f)
            close()
            moveTo(14.83f, 13.41f)
            lineTo(13.42f, 14.83f)
            lineTo(16.54f, 17.96f)
            lineTo(14.5f, 20.0f)
            lineTo(20.0f, 20.0f)
            lineTo(20.0f, 14.5f)
            lineTo(17.96f, 16.54f)
            lineTo(14.83f, 13.41f)
            close()
        }
    }.build()

val IconSkipNext: ImageVector
    get() = ImageVector.Builder(name = "SkipNext", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6.0f, 18.0f)
            lineTo(14.5f, 12.0f)
            lineTo(6.0f, 6.0f)
            lineTo(6.0f, 18.0f)
            close()
            moveTo(16.0f, 6.0f)
            lineTo(16.0f, 18.0f)
            lineTo(18.0f, 18.0f)
            lineTo(18.0f, 6.0f)
            lineTo(16.0f, 6.0f)
            close()
        }
    }.build()

val IconSkipPrevious: ImageVector
    get() = ImageVector.Builder(name = "SkipPrevious", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6.0f, 6.0f)
            lineTo(8.0f, 6.0f)
            lineTo(8.0f, 18.0f)
            lineTo(6.0f, 18.0f)
            lineTo(6.0f, 6.0f)
            close()
            moveTo(9.5f, 12.0f)
            lineTo(18.0f, 18.0f)
            lineTo(18.0f, 6.0f)
            lineTo(9.5f, 12.0f)
            close()
        }
    }.build()

val IconRepeat: ImageVector
    get() = ImageVector.Builder(name = "Repeat", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(7.0f, 7.0f)
            lineTo(17.0f, 7.0f)
            lineTo(17.0f, 10.0f)
            lineTo(21.0f, 6.0f)
            lineTo(17.0f, 2.0f)
            lineTo(17.0f, 5.0f)
            lineTo(5.0f, 5.0f)
            lineTo(5.0f, 11.0f)
            lineTo(7.0f, 11.0f)
            lineTo(7.0f, 7.0f)
            close()
            moveTo(17.0f, 17.0f)
            lineTo(7.0f, 17.0f)
            lineTo(7.0f, 14.0f)
            lineTo(3.0f, 18.0f)
            lineTo(7.0f, 22.0f)
            lineTo(7.0f, 19.0f)
            lineTo(19.0f, 19.0f)
            lineTo(19.0f, 13.0f)
            lineTo(17.0f, 13.0f)
            lineTo(17.0f, 17.0f)
            close()
        }
    }.build()

val IconMusicNote: ImageVector
    get() = ImageVector.Builder(name = "MusicNote", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12.0f, 3.0f)
            lineTo(12.0f, 13.55f)
            curveTo(11.41f, 13.21f, 10.73f, 13.0f, 10.0f, 13.0f)
            curveTo(7.79f, 13.0f, 6.0f, 14.79f, 6.0f, 17.0f)
            curveTo(6.0f, 19.21f, 7.79f, 21.0f, 10.0f, 21.0f)
            curveTo(12.21f, 21.0f, 14.0f, 19.21f, 14.0f, 17.0f)
            lineTo(14.0f, 7.0f)
            lineTo(18.0f, 7.0f)
            lineTo(18.0f, 3.0f)
            lineTo(12.0f, 3.0f)
            close()
        }
    }.build()

val IconPause: ImageVector
    get() = ImageVector.Builder(name = "Pause", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6.0f, 19.0f)
            lineTo(10.0f, 19.0f)
            lineTo(10.0f, 5.0f)
            lineTo(6.0f, 5.0f)
            lineTo(6.0f, 19.0f)
            close()
            moveTo(14.0f, 5.0f)
            lineTo(14.0f, 19.0f)
            lineTo(18.0f, 19.0f)
            lineTo(18.0f, 5.0f)
            lineTo(14.0f, 5.0f)
            close()
        }
    }.build()

val IconPlayArrow: ImageVector
    get() = ImageVector.Builder(name = "PlayArrow", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(8.0f, 5.0f)
            lineTo(8.0f, 19.0f)
            lineTo(19.0f, 12.0f)
            lineTo(8.0f, 5.0f)
            close()
        }
    }.build()

data class MusicTrack(
    val name: String,
    val title: String,
    val artist: String,
    val genre: String,
    val uri: Uri,
    val artwork: Bitmap? = null
)

data class Playlist(
    val name: String,
    val tracks: MutableList<MusicTrack> = mutableListOf()
)

enum class VisualizerStyle(val displayName: String) {
    BARS("Spectrum Bars"),
    WAVE("Waveform"),
    PULSE("Pulse Circle"),
    MIRROR("Mirrored Spectrum"),
    DOTS("Dot Matrix"),
    RADAR("Circular Radar"),
    RIBBON("Neon Ribbon"),
    PARTICLES("Particle Ring")
}

enum class ThemeModeOption(val displayName: String) {
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
    SYSTEM("System Default")
}

enum class AppTheme(
    val displayName: String,
    val darkBg: Color,
    val darkCardBg: Color,
    val lightBg: Color,
    val lightCardBg: Color,
    val accent: Color
) {
    PURPLE("Dark Purple", Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFFF3E8FF), Color(0xFFE9D5FF), Color(0xFFBB86FC)),
    AMOLED("AMOLED Black", Color(0xFF000000), Color(0xFF111111), Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFF00E676)),
    NEON("Cyber Neon", Color(0xFF0D0E15), Color(0xFF1B1D2A), Color(0xFFFFF0F5), Color(0xFFFFE4E1), Color(0xFFFF0055)),
    BLUE("Midnight Blue", Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFF38BDF8)),
    RED("Crimson Red", Color(0xFF18080A), Color(0xFF280F12), Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFFF4D4D)),
    EMERALD("Forest Emerald", Color(0xFF0A140F), Color(0xFF14241B), Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF2ECC71)),
    AMBER("Solarized Amber", Color(0xFF141008), Color(0xFF241D12), Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFFB300)),
    SUNSET("Sunset Orange", Color(0xFF180C0A), Color(0xFF2A1612), Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFF7043)),
    TEAL("Electric Teal", Color(0xFF081414), Color(0xFF122424), Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFF00ACC1)),
    CYBERPUNK("Cyberpunk", Color(0xFF0F051D), Color(0xFF1D0A35), Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFFE040FB)),
    MATCHA("Matcha Mint", Color(0xFF08120A), Color(0xFF132216), Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF10B981)),
    ROYAL("Royal Gold", Color(0xFF120E05), Color(0xFF221A0A), Color(0xFFFFFBEB), Color(0xFFFEF08A), Color(0xFFF59E0B)),
    SOLAR("Solar Violet", Color(0xFF1A002C), Color(0xFF2D004D), Color(0xFFF5E6FF), Color(0xFFEBD1FF), Color(0xFFD400FF)),
    VAPORWAVE("Vaporwave Pink", Color(0xFF1B001B), Color(0xFF330033), Color(0xFFFFE6FF), Color(0xFFFFCCFF), Color(0xFFFF00A0)),
    ARCTIC("Arctic Ice", Color(0xFF0A192F), Color(0xFF172A45), Color(0xFFE6F1FF), Color(0xFFCCD6F6), Color(0xFF64FFDA)),
    TOXIC("Toxic Lime", Color(0xFF0A1A00), Color(0xFF153300), Color(0xFFF2FFEC), Color(0xFFDCFFC8), Color(0xFFA6FF00)),
    LAVA("Lava Orange", Color(0xFF1F0800), Color(0xFF3D1000), Color(0xFFFFF0EC), Color(0xFFFFDCD2), Color(0xFFFF3D00)),
    COFFEE("Coffee Brown", Color(0xFF140D07), Color(0xFF291A0E), Color(0xFFF7F2EE), Color(0xFFEAE0D5), Color(0xFFD4A373)),
    OCEAN("Ocean Depth", Color(0xFF021019), Color(0xFF052033), Color(0xFFE6F4FA), Color(0xFFC3E4F3), Color(0xFF00B4D8)),
    DRACULA("Dracula Gothic", Color(0xFF181825), Color(0xFF282A36), Color(0xFFF8F8F2), Color(0xFFE2E2DC), Color(0xFFFF79C6))
}

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val player: Player?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val fftDataState = mutableStateListOf<Float>()

    private val allTracks = mutableStateListOf<MusicTrack>()
    private val displayTracks = mutableStateListOf<MusicTrack>()
    private val genresList = mutableStateListOf<String>()
    private val artistsList = mutableStateListOf<String>()
    private val playlistsList = mutableStateListOf<Playlist>()
    
    private var isFiltered by mutableStateOf(false)
    private var activeFilterName by mutableStateOf("")

    private var currentTrack by mutableStateOf<MusicTrack?>(null)
    private var isPlayingState by mutableStateOf(false)
    private var isShuffleEnabled by mutableStateOf(false)
    private var repeatModeState by mutableStateOf(Player.REPEAT_MODE_OFF)
    private var isPlayerMinimized by mutableStateOf(false)
    
    private var currentPositionMs by mutableStateOf(0L)
    private var totalDurationMs by mutableStateOf(0L)
    private var isScanning by mutableStateOf(false)
    
    private var showSettingsDialog by mutableStateOf(false)
    private var showNewPlaylistDialog by mutableStateOf(false)
    private var trackToAddToPlaylist by mutableStateOf<MusicTrack?>(null)

    private var currentFolderPath by mutableStateOf("MediaStore (All Storage)")
    private var selectedTab by mutableStateOf(0)
    private val videosList = mutableStateListOf<VideoItem>()
    private var currentTheme by mutableStateOf(AppTheme.PURPLE)
    private var themeModeOption by mutableStateOf(ThemeModeOption.DARK)
    private var selectedVisualizer by mutableStateOf(VisualizerStyle.BARS)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val savedUriStr = getSharedPreferences("prefs", MODE_PRIVATE).getString("folder_uri", null)
        val uri = savedUriStr?.let { Uri.parse(it) }
        if (!loadCachedLibrary()) {
            startDeepLibraryScan(uri)
        }
    }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("folder_uri", it.toString()).apply()
            currentFolderPath = it.path ?: it.toString()
            startDeepLibraryScan(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controllerFuture?.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPlayingState = isPlaying
                }
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    isPlayingState = controller.isPlaying
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index = controller.currentMediaItemIndex
                    if (index in 0 until displayTracks.size) {
                        currentTrack = displayTracks[index]
                    }
                }
            })
        }, MoreExecutors.directExecutor())

        requestAudioPermissions()

        setContent {
            LaunchedEffect(Unit) {
                while (true) {
                    player?.let { p ->
                        isPlayingState = p.isPlaying
                        if (p.isPlaying) {
                            currentPositionMs = p.currentPosition.coerceAtLeast(0L)
                            totalDurationMs = p.duration.coerceAtLeast(0L)
                            
                            fftDataState.clear()
                            fftDataState.addAll(PlaybackService.latestFftData.toList())
                        }
                    }
                    delay(30)
                }
            }

            BackHandler(enabled = isFiltered) { resetFilter() }

            val isDarkTheme = when (themeModeOption) {
                ThemeModeOption.DARK -> true
                ThemeModeOption.LIGHT -> false
                ThemeModeOption.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val activeBg = if (isDarkTheme) currentTheme.darkBg else currentTheme.lightBg
            val activeCardBg = if (isDarkTheme) currentTheme.darkCardBg else currentTheme.lightCardBg
            val textColor = if (isDarkTheme) Color.White else Color.Black
            val subTextColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme(background = activeBg) else lightColorScheme(background = activeBg)) {
                Surface(modifier = Modifier.fillMaxSize(), color = activeBg) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MediaNexpo", fontSize = 24.sp, color = currentTheme.accent)
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = currentTheme.accent
                                )
                            }
                        }

                        if (isScanning) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = currentTheme.accent
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isFiltered) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Filter: $activeFilterName", color = currentTheme.accent, fontSize = 13.sp)
                                TextButton(onClick = { resetFilter() }) {
                                    Text("Show All Songs ✕", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }

                        val tabs = listOf("Songs", "Artists", "Genres", "Playlists", "Videos", "About")

                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tabs.size) { index ->
                                val isSelected = selectedTab == index
                                val chipBg = if (isSelected) currentTheme.accent else activeCardBg
                                val chipTextColor = if (isSelected) Color.Black else textColor

                                androidx.compose.material3.Surface(
                                    onClick = { selectedTab = index },
                                    shape = RoundedCornerShape(20.dp),
                                    color = chipBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color.Black else subTextColor.copy(alpha = 0.25f)
                                    ),
                                    shadowElevation = if (isSelected) 2.dp else 0.dp
                                ) {
                                    Text(
                                        text = tabs[index],
                                        color = chipTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> TrackList(
                                    tracks = displayTracks,
                                    cardBg = activeCardBg,
                                    textColor = textColor,
                                    subTextColor = subTextColor,
                                    onTrackSelect = { playTrack(it) },
                                    onTrackLongClick = { trackToAddToPlaylist = it }
                                )
                                1 -> ArtistList(
                                    artists = artistsList,
                                    theme = currentTheme,
                                    cardBg = activeCardBg,
                                    textColor = textColor,
                                    onArtistClick = { artistName ->
                                        displayTracks.clear()
                                        displayTracks.addAll(allTracks.filter { it.artist.equals(artistName, ignoreCase = true) })
                                        isFiltered = true
                                        activeFilterName = "Artist: $artistName"
                                        selectedTab = 0
                                    }
                                )
                                2 -> GenreList(
                                    genres = genresList,
                                    theme = currentTheme,
                                    cardBg = activeCardBg,
                                    textColor = textColor,
                                    onGenreClick = { genreName ->
                                        displayTracks.clear()
                                        displayTracks.addAll(allTracks.filter { it.genre.equals(genreName, ignoreCase = true) })
                                        isFiltered = true
                                        activeFilterName = "Genre: $genreName"
                                        selectedTab = 0
                                    }
                                )
                                3 -> PlaylistView(
                                    playlists = playlistsList,
                                    theme = currentTheme,
                                    cardBg = activeCardBg,
                                    textColor = textColor,
                                    onCreateNew = { showNewPlaylistDialog = true },
                                    onPlayPlaylist = { playlist ->
                                        if (playlist.tracks.isNotEmpty()) {
                                            playPlaylist(playlist)
                                        }
                                    }
                                )
                                4 -> VideoList(
                                    videos = videosList,
                                    cardBg = activeCardBg,
                                    textColor = textColor,
                                    subTextColor = subTextColor,
                                    onVideoSelect = { video ->
                                        player?.pause()
                                        val intent = Intent(this@MainActivity, VideoPlayerActivity::class.java).apply {
                                            putExtra("EXTRA_VIDEO_URI", video.contentUri.toString())
                                        }
                                        startActivity(intent)
                                    }
                                )
                                5 -> AboutView(currentTheme, activeCardBg, textColor)
                            }
                        }

                        currentTrack?.let { track ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                                colors = CardDefaults.cardColors(containerColor = activeCardBg)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isPlayerMinimized) "Now Playing (Minimized)" else "Now Playing",
                                            color = currentTheme.accent,
                                            fontSize = 12.sp
                                        )
                                        IconButton(
                                            onClick = { isPlayerMinimized = !isPlayerMinimized },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayerMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Minimize",
                                                tint = textColor
                                            )
                                        }
                                    }

                                    if (!isPlayerMinimized) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        MusicVisualizerView(
                                            isPlaying = isPlayingState,
                                            style = selectedVisualizer,
                                            fftValues = fftDataState,
                                            accentColor = currentTheme.accent
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (track.artwork != null) {
                                            Image(
                                                bitmap = track.artwork.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(if (isPlayerMinimized) 36.dp else 52.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isPlayerMinimized) 36.dp else 52.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(currentTheme.accent.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(IconMusicNote, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(track.title, color = textColor, fontSize = if (isPlayerMinimized) 14.sp else 16.sp, maxLines = 1)
                                            Text("${track.artist} • ${track.genre}", color = subTextColor, fontSize = 11.sp, maxLines = 1)
                                        }

                                        if (isPlayerMinimized) {
                                            IconButton(
                                                onClick = { player?.let { if (it.isPlaying) it.pause() else it.play() } }
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayingState) IconPause else IconPlayArrow,
                                                    contentDescription = "Play/Pause",
                                                    tint = currentTheme.accent
                                                )
                                            }
                                        }
                                    }

                                    if (!isPlayerMinimized) {
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val sliderPosition = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs else 0f
                                        Slider(
                                            value = sliderPosition,
                                            onValueChange = { percent ->
                                                if (totalDurationMs > 0) {
                                                    val seekTarget = (percent * totalDurationMs).toLong()
                                                    player?.seekTo(seekTarget)
                                                    currentPositionMs = seekTarget
                                                }
                                            },
                                            colors = SliderDefaults.colors(
                                                thumbColor = currentTheme.accent,
                                                activeTrackColor = currentTheme.accent
                                            )
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(formatTime(currentPositionMs), color = subTextColor, fontSize = 11.sp)
                                            val remainingMs = (totalDurationMs - currentPositionMs).coerceAtLeast(0L)
                                            Text("-${formatTime(remainingMs)}", color = subTextColor, fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { toggleShuffle() }) {
                                                Icon(IconShuffle, contentDescription = "Shuffle", tint = if (isShuffleEnabled) currentTheme.accent else subTextColor)
                                            }
                                            IconButton(onClick = { player?.let { if (it.hasPreviousMediaItem()) it.seekToPrevious() } }) {
                                                Icon(IconSkipPrevious, contentDescription = "Previous", tint = textColor)
                                            }
                                            Button(
                                                onClick = { player?.let { if (it.isPlaying) it.pause() else it.play() } },
                                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                            ) {
                                                Icon(imageVector = if (isPlayingState) IconPause else IconPlayArrow, contentDescription = "Play/Pause", tint = Color.Black)
                                            }
                                            IconButton(onClick = { player?.let { if (it.hasNextMediaItem()) it.seekToNext() } }) {
                                                Icon(IconSkipNext, contentDescription = "Next", tint = textColor)
                                            }
                                            IconButton(onClick = { cycleRepeatMode() }) {
                                                Icon(IconRepeat, contentDescription = "Repeat", tint = if (repeatModeState != Player.REPEAT_MODE_OFF) currentTheme.accent else subTextColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- PLAYLIST DIALOGS ---
                    if (showNewPlaylistDialog) {
                        var newPlaylistName by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showNewPlaylistDialog = false },
                            title = { Text("Create New Playlist", color = textColor) },
                            text = {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    label = { Text("Playlist Name", color = subTextColor) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = currentTheme.accent,
                                        unfocusedBorderColor = subTextColor,
                                        focusedLabelColor = currentTheme.accent,
                                        cursorColor = currentTheme.accent
                                    )
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newPlaylistName.isNotBlank()) {
                                            playlistsList.add(Playlist(name = newPlaylistName.trim()))
                                            showNewPlaylistDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                ) {
                                    Text("Create", color = Color.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNewPlaylistDialog = false }) {
                                    Text("Cancel", color = subTextColor)
                                }
                            },
                            containerColor = activeCardBg
                        )
                    }

                    trackToAddToPlaylist?.let { track ->
                        AlertDialog(
                            onDismissRequest = { trackToAddToPlaylist = null },
                            title = { Text("Add to Playlist", color = textColor) },
                            text = {
                                if (playlistsList.isEmpty()) {
                                    Text("No playlists created yet.", color = subTextColor)
                                } else {
                                    LazyColumn {
                                        items(playlistsList) { playlist ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable {
                                                        if (!playlist.tracks.contains(track)) {
                                                            playlist.tracks.add(track)
                                                        }
                                                        trackToAddToPlaylist = null
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = activeCardBg)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(IconMusicNote, contentDescription = null, tint = currentTheme.accent)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(playlist.name, color = textColor, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showNewPlaylistDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                ) {
                                    Text("➕ New Playlist", color = Color.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { trackToAddToPlaylist = null }) {
                                    Text("Cancel", color = subTextColor)
                                }
                            },
                            containerColor = activeCardBg
                        )
                    }

                    if (showSettingsDialog) {
                        AlertDialog(
                            onDismissRequest = { showSettingsDialog = false },
                            title = { Text("Settings & Themes", color = textColor) },
                            text = {
                                LazyColumn {
                                    item {
                                        Button(
                                            onClick = {
                                                showSettingsDialog = false
                                                val savedUriStr = getSharedPreferences("prefs", MODE_PRIVATE).getString("folder_uri", null)
                                                val uri = savedUriStr?.let { Uri.parse(it) }
                                                startDeepLibraryScan(uri)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                        ) {
                                            Text("🔄 Rescan Media Library", color = Color.Black)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Theme Mode:", color = subTextColor, fontSize = 14.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ThemeModeOption.values().forEach { mode ->
                                                Button(
                                                    onClick = { themeModeOption = mode },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (themeModeOption == mode) currentTheme.accent else Color.DarkGray)
                                                ) {
                                                    Text(mode.displayName, fontSize = 10.sp, color = textColor, maxLines = 1)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Visualizer Style:", color = subTextColor, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        var visExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { visExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = activeCardBg)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("📊 Visualizer: ${selectedVisualizer.displayName}", color = currentTheme.accent, fontSize = 13.sp)
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = currentTheme.accent)
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = visExpanded,
                                                onDismissRequest = { visExpanded = false },
                                                modifier = Modifier.background(activeCardBg)
                                            ) {
                                                VisualizerStyle.values().forEach { style ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = style.displayName,
                                                                color = if (selectedVisualizer == style) currentTheme.accent else textColor,
                                                                fontSize = 13.sp
                                                            )
                                                        },
                                                        onClick = {
                                                            selectedVisualizer = style
                                                            visExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Color Theme Palette (${AppTheme.values().size} Themes):", color = subTextColor, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        var themeExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { themeExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = activeCardBg)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("🎨 Theme: ${currentTheme.displayName}", color = currentTheme.accent, fontSize = 13.sp)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                                            .background(currentTheme.accent)
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = themeExpanded,
                                                onDismissRequest = { themeExpanded = false },
                                                modifier = Modifier
                                                    .background(activeCardBg)
                                                    .heightIn(max = 300.dp)
                                            ) {
                                                AppTheme.values().forEach { theme ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(
                                                                    text = theme.displayName,
                                                                    color = if (currentTheme == theme) theme.accent else textColor,
                                                                    fontSize = 13.sp
                                                                )
                                                                Spacer(modifier = Modifier.width(16.dp))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(14.dp)
                                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                                        .background(theme.accent)
                                                                )
                                                            }
                                                        },
                                                        onClick = {
                                                            currentTheme = theme
                                                            themeExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Storage Directory:", color = subTextColor, fontSize = 14.sp)
                                        Text(currentFolderPath, color = textColor, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = {
                                                showSettingsDialog = false
                                                folderPicker.launch(null)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Select Custom Music Folder")
                                        }
                                    }
                                    item {
                                        AudioEffectsSettingsSection(textColor = textColor, subTextColor = subTextColor, cardBg = activeCardBg)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showSettingsDialog = false }) { Text("Close") }
                            },
                            containerColor = activeCardBg
                        )
                    }
                }
            }
        }
    }

    private fun resetFilter() {
        displayTracks.clear()
        displayTracks.addAll(allTracks)
        isFiltered = false
        activeFilterName = ""
    }

    private fun requestAudioPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissions.add(Manifest.permission.RECORD_AUDIO)

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            val savedUriStr = getSharedPreferences("prefs", MODE_PRIVATE).getString("folder_uri", null)
            val uri = savedUriStr?.let { Uri.parse(it) }
            if (!loadCachedLibrary()) {
                startDeepLibraryScan(uri)
            }
        }
    }

    private fun saveLibraryToCache(tracks: List<MusicTrack>) {
        try {
            val jsonArray = JSONArray()
            tracks.forEach { track ->
                val obj = JSONObject().apply {
                    put("name", track.name)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("genre", track.genre)
                    put("uri", track.uri.toString())
                }
                jsonArray.put(obj)
            }
            getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("cached_library_json", jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e("LocalMusicPlayer", "Failed to cache library", e)
        }
    }

    private fun loadCachedLibrary(): Boolean {
        val jsonStr = getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("cached_library_json", null)
            ?: return false

        return try {
            val jsonArray = JSONArray(jsonStr)
            val discoveredTracks = mutableListOf<MusicTrack>()
            val discoveredGenres = mutableSetOf<String>()
            val discoveredArtists = mutableSetOf<String>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.optString("name")
                val title = obj.optString("title")
                val artist = obj.optString("artist")
                val genre = obj.optString("genre")
                val uriStr = obj.optString("uri")
                val uri = Uri.parse(uriStr)

                discoveredTracks.add(MusicTrack(name, title, artist, genre, uri, null))
                if (genre != "Unknown Genre" && genre.isNotBlank()) discoveredGenres.add(genre)
                if (artist != "Unknown Artist" && artist.isNotBlank()) discoveredArtists.add(artist)
            }

            if (discoveredTracks.isNotEmpty()) {
                allTracks.clear()
                allTracks.addAll(discoveredTracks)
                displayTracks.clear()
                displayTracks.addAll(discoveredTracks)
                genresList.clear()
                genresList.addAll(discoveredGenres)
                artistsList.clear()
                artistsList.addAll(discoveredArtists)
                val scannedVideos = VideoRepository.scanLocalVideos(this@MainActivity)
                videosList.clear()
                videosList.addAll(scannedVideos)
                true
            } else false
        } catch (e: Exception) {
            Log.e("LocalMusicPlayer", "Failed to load cached library", e)
            false
        }
    }

    @Composable
    private fun MusicVisualizerView(
        isPlaying: Boolean,
        style: VisualizerStyle,
        fftValues: List<Float>,
        accentColor: Color
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            val width = size.width
            val height = size.height

            if (!isPlaying || fftValues.isEmpty()) {
                drawLine(
                    color = accentColor.copy(alpha = 0.3f),
                    start = Offset(0f, height / 2),
                    end = Offset(width, height / 2),
                    strokeWidth = 2.dp.toPx()
                )
                return@Canvas
            }

            when (style) {
                VisualizerStyle.BARS -> {
                    val barCount = fftValues.size
                    val barWidth = width / (barCount * 1.5f)
                    for (i in 0 until barCount) {
                        val amplitude = fftValues[i]
                        val barHeight = (height * amplitude).coerceAtLeast(4f)
                        val x = i * (barWidth * 1.5f) + barWidth / 2
                        drawLine(
                            color = accentColor,
                            start = Offset(x, height),
                            end = Offset(x, height - barHeight),
                            strokeWidth = barWidth
                        )
                    }
                }
                VisualizerStyle.WAVE -> {
                    val path = Path()
                    path.moveTo(0f, height / 2)
                    val points = fftValues.size
                    for (i in 0 until points) {
                        val x = (width / (points - 1)) * i
                        val amplitude = fftValues[i]
                        val y = height / 2 + (amplitude * (height / 2f) * if (i % 2 == 0) 1f else -1f)
                        path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                VisualizerStyle.PULSE -> {
                    val averageAmplitude = fftValues.average().toFloat()
                    val pulseRadius = (height / 2.5f) * (averageAmplitude * 1.2f + 0.3f)
                    drawCircle(
                        color = accentColor.copy(alpha = 0.85f),
                        radius = pulseRadius.coerceIn(4f, height / 2f),
                        center = Offset(width / 2, height / 2),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                VisualizerStyle.MIRROR -> {
                    val count = fftValues.size
                    val barWidth = width / (count * 1.4f)
                    for (i in 0 until count) {
                        val amp = fftValues[i]
                        val barLen = (height / 2f) * amp
                        val x = i * (barWidth * 1.4f) + barWidth / 2
                        drawLine(
                            color = accentColor,
                            start = Offset(x, height / 2 - barLen),
                            end = Offset(x, height / 2 + barLen),
                            strokeWidth = barWidth
                        )
                    }
                }
                VisualizerStyle.DOTS -> {
                    val cols = fftValues.size
                    val rows = 5
                    val dotRadius = 3.dp.toPx()
                    val colSpacing = width / cols
                    val rowSpacing = height / (rows + 1)
                    for (c in 0 until cols) {
                        val amp = fftValues[c]
                        val activeDots = (amp * rows).toInt().coerceIn(1, rows)
                        for (r in 0 until rows) {
                            val dotY = height - ((r + 1) * rowSpacing)
                            val dotX = c * colSpacing + colSpacing / 2
                            val isLit = r < activeDots
                            drawCircle(
                                color = if (isLit) accentColor else accentColor.copy(alpha = 0.15f),
                                radius = dotRadius,
                                center = Offset(dotX, dotY)
                            )
                        }
                    }
                }
                VisualizerStyle.RADAR -> {
                    val centerX = width / 2
                    val centerY = height / 2
                    val count = fftValues.size
                    val angleStep = (2 * Math.PI / count).toFloat()
                    for (i in 0 until count) {
                        val amp = fftValues[i]
                        val radius = (height / 2.2f) * amp
                        val angle = i * angleStep
                        val endX = centerX + (radius * cos(angle.toDouble())).toFloat()
                        val endY = centerY + (radius * sin(angle.toDouble())).toFloat()
                        drawLine(
                            color = accentColor,
                            start = Offset(centerX, centerY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                }
                VisualizerStyle.RIBBON -> {
                    val path = Path()
                    path.moveTo(0f, height)
                    val points = fftValues.size
                    for (i in 0 until points) {
                        val x = (width / (points - 1)) * i
                        val y = height - (fftValues[i] * height)
                        path.lineTo(x, y)
                    }
                    path.lineTo(width, height)
                    path.close()
                    drawPath(
                        path = path,
                        color = accentColor.copy(alpha = 0.45f)
                    )
                }
                VisualizerStyle.PARTICLES -> {
                    val centerX = width / 2
                    val centerY = height / 2
                    val count = fftValues.size
                    val angleStep = (2 * Math.PI / count).toFloat()
                    for (i in 0 until count) {
                        val amp = fftValues[i]
                        val baseRadius = (height / 4f)
                        val dist = baseRadius + (amp * (height / 3f))
                        val angle = i * angleStep
                        val px = centerX + (dist * cos(angle.toDouble())).toFloat()
                        val py = centerY + (dist * sin(angle.toDouble())).toFloat()
                        drawCircle(
                            color = accentColor,
                            radius = (amp * 4.5.dp.toPx()).coerceAtLeast(2.dp.toPx()),
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun TrackList(
        tracks: List<MusicTrack>,
        cardBg: Color,
        textColor: Color,
        subTextColor: Color,
        onTrackSelect: (MusicTrack) -> Unit,
        onTrackLongClick: (MusicTrack) -> Unit
    ) {
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tracks found.", color = subTextColor)
            }
        } else {
            LazyColumn {
                items(tracks) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = { onTrackSelect(track) },
                                onLongClick = { onTrackLongClick(track) }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentTrack == track) cardBg.copy(alpha = 0.8f) else cardBg
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (track.artwork != null) {
                                Image(
                                    bitmap = track.artwork.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(currentTheme.accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(IconMusicNote, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.size(20.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(text = track.title, color = textColor, fontSize = 16.sp, maxLines = 1)
                                Text(text = "${track.artist} • ${track.genre}", color = subTextColor, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ArtistList(artists: List<String>, theme: AppTheme, cardBg: Color, textColor: Color, onArtistClick: (String) -> Unit) {
        if (artists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No artists found.", color = Color.Gray) }
        } else {
            LazyColumn {
                items(artists) { artist ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onArtistClick(artist) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = theme.accent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = artist, color = textColor, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GenreList(genres: List<String>, theme: AppTheme, cardBg: Color, textColor: Color, onGenreClick: (String) -> Unit) {
        if (genres.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No genres found.", color = Color.Gray) }
        } else {
            LazyColumn {
                items(genres) { genre ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onGenreClick(genre) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconMusicNote, contentDescription = null, tint = theme.accent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = genre, color = textColor, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PlaylistView(
        playlists: List<Playlist>,
        theme: AppTheme,
        cardBg: Color,
        textColor: Color,
        onCreateNew: () -> Unit,
        onPlayPlaylist: (Playlist) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = onCreateNew,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
                Text("➕ Create New Playlist", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (playlists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No playlists yet. Create one above!", color = Color.Gray) }
            } else {
                LazyColumn {
                    items(playlists) { playlist ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onPlayPlaylist(playlist) },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222)),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(playlist.name, color = textColor, fontSize = 16.sp)
                                    Text("${playlist.tracks.size} Tracks", color = Color.Gray, fontSize = 12.sp)
                                }
                                Icon(IconPlayArrow, contentDescription = "Play Playlist", tint = theme.accent)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AboutView(theme: AppTheme, cardBg: Color, textColor: Color) {
        val context = LocalContext.current
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MediaNexpo", fontSize = 24.sp, color = theme.accent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Created by RomLord14495 (aka Kyle) from XDA", fontSize = 13.sp, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Lightweight Open-Source Local Audio Player with SAF Support", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/crabcakes97/LocalMusicPlayer/"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) {
                        Text("View on GitHub", color = Color.Black)
                    }
                }
            }
        }
    }

    private fun startDeepLibraryScan(customFolderUri: Uri?) {
        isScanning = true
        allTracks.clear()
        displayTracks.clear()
        genresList.clear()
        artistsList.clear()

        GlobalScope.launch(Dispatchers.IO) {
            val discoveredTracks = mutableListOf<MusicTrack>()
            val discoveredGenres = mutableSetOf<String>()
            val discoveredArtists = mutableSetOf<String>()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.DISPLAY_NAME
            )

            try {
                val cursor = contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )

                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val genreCol = it.getColumnIndex(MediaStore.Audio.Media.GENRE)
                    val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val title = it.getString(titleCol) ?: "Unknown Title"
                        val artist = it.getString(artistCol) ?: "Unknown Artist"
                        val genre = if (genreCol >= 0) it.getString(genreCol) ?: "Unknown Genre" else "Unknown Genre"
                        val displayName = it.getString(nameCol) ?: title
                        val contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())

                        val artwork = extractArtwork(contentUri)
                        discoveredTracks.add(MusicTrack(displayName, title, artist, genre, contentUri, artwork))
                        if (genre != "Unknown Genre") discoveredGenres.add(genre)
                        if (artist != "Unknown Artist") discoveredArtists.add(artist)
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalMusicPlayer", "MediaStore query error", e)
            }

            if (customFolderUri != null) {
                try {
                    val root = DocumentFile.fromTreeUri(this@MainActivity, customFolderUri)
                    root?.let { traverseRecursive(it, discoveredTracks, discoveredGenres, discoveredArtists) }
                } catch (e: Exception) {
                    Log.e("LocalMusicPlayer", "SAF folder scan error", e)
                }
            }

            withContext(Dispatchers.Main) {
                val distinctList = discoveredTracks.distinctBy { "${it.title}_${it.artist}" }
                allTracks.clear()
                allTracks.addAll(distinctList)
                displayTracks.clear()
                displayTracks.addAll(distinctList)
                genresList.clear()
                genresList.addAll(discoveredGenres)
                artistsList.clear()
                artistsList.addAll(discoveredArtists)
                val scannedVideos = VideoRepository.scanLocalVideos(this@MainActivity)
                videosList.clear()
                videosList.addAll(scannedVideos)
                
                saveLibraryToCache(distinctList)
                isScanning = false
            }
        }
    }

    private fun traverseRecursive(dir: DocumentFile, outTracks: MutableList<MusicTrack>, outGenres: MutableSet<String>, outArtists: MutableSet<String>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                traverseRecursive(file, outTracks, outGenres, outArtists)
            } else if (file.name != null && (
                file.name!!.endsWith(".mp3", true) ||
                file.name!!.endsWith(".flac", true) ||
                file.name!!.endsWith(".m4a", true) ||
                file.name!!.endsWith(".wav", true) ||
                file.name!!.endsWith(".ogg", true) ||
                file.name!!.endsWith(".aac", true) ||
                file.name!!.endsWith(".mp4", true) ||
                file.name!!.endsWith(".mkv", true) ||
                file.name!!.endsWith(".webm", true) ||
                file.name!!.endsWith(".mov", true) ||
                file.name!!.endsWith(".avi", true)
            )) {
                val metadata = getTrackMetadata(file.uri, file.name!!)
                outTracks.add(metadata)
                if (metadata.genre != "Unknown Genre") outGenres.add(metadata.genre)
                if (metadata.artist != "Unknown Artist") outArtists.add(metadata.artist)
            }
        }
    }

    private fun extractArtwork(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            } else null
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun getTrackMetadata(uri: Uri, fallbackName: String): MusicTrack {
        val retriever = MediaMetadataRetriever()
        var title = fallbackName
        var artist = "Unknown Artist"
        var genre = "Unknown Genre"
        var artwork: Bitmap? = null

        try {
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let { genre = it }
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                artwork = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            }
        } catch (e: Exception) {
            Log.e("LocalMusicPlayer", "Failed to extract metadata for $uri", e)
        } finally {
            retriever.release()
        }

        return MusicTrack(fallbackName, title, artist, genre, uri, artwork)
    }

    private fun playTrack(track: MusicTrack) {
        currentTrack = track
        player?.stop()
        player?.clearMediaItems()
        
        val items = displayTracks.map { trackItem ->
            val metadata = MediaMetadata.Builder()
                .setTitle(trackItem.title)
                .setArtist(trackItem.artist)
                .setGenre(trackItem.genre)
                .build()

            MediaItem.Builder()
                .setUri(trackItem.uri)
                .setMediaMetadata(metadata)
                .build()
        }
        player?.setMediaItems(items)
        
        val startIndex = displayTracks.indexOf(track)
        if (startIndex >= 0) {
            player?.seekTo(startIndex, 0L)
        }
        
        player?.prepare()
        player?.playWhenReady = true
        player?.play()
    }

    private fun playPlaylist(playlist: Playlist) {
        if (playlist.tracks.isEmpty()) return
        currentTrack = playlist.tracks.first()
        player?.stop()
        player?.clearMediaItems()
        
        val items = playlist.tracks.map { trackItem ->
            val metadata = MediaMetadata.Builder()
                .setTitle(trackItem.title)
                .setArtist(trackItem.artist)
                .setGenre(trackItem.genre)
                .build()

            MediaItem.Builder()
                .setUri(trackItem.uri)
                .setMediaMetadata(metadata)
                .build()
        }
        player?.setMediaItems(items)
        
        player?.seekTo(0, 0L)
        player?.prepare()
        player?.playWhenReady = true
        player?.play()
    }

    private fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        player?.shuffleModeEnabled = isShuffleEnabled
    }

    private fun cycleRepeatMode() {
        repeatModeState = when (repeatModeState) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player?.repeatMode = repeatModeState
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}


// --- Upgraded Audio Effects UI (Live Updating) ---
@Composable
fun AudioEffectsSettingsSection(
    textColor: Color = Color.Black,
    subTextColor: Color = Color(0xFF444444),
    cardBg: Color = Color.White.copy(alpha = 0.3f)
) {
    var eqEnabled by remember { 
        mutableStateOf(PlaybackService.eqEnabled) 
    }
    var bassLevel by remember { 
        mutableFloatStateOf(PlaybackService.bassStrength.toFloat() / 1000f) 
    }
    var virtLevel by remember { 
        mutableFloatStateOf(PlaybackService.virtualizerStrength.toFloat() / 1000f) 
    }
    var gainLevel by remember { 
        mutableFloatStateOf(PlaybackService.gainMb.toFloat() / 1000f) 
    }

    val bandFreqs = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val eqBands = remember {
        mutableStateListOf(
            PlaybackService.bandLevels[0].toFloat(),
            PlaybackService.bandLevels[1].toFloat(),
            PlaybackService.bandLevels[2].toFloat(),
            PlaybackService.bandLevels[3].toFloat(),
            PlaybackService.bandLevels[4].toFloat()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // --- MASTER TOGGLE ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Enable Equalizer & Effects", 
                    color = textColor, 
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { 
                        eqEnabled = it
                        PlaybackService.updateEqEnabled(it)
                    }
                )
            }
        }

        if (eqEnabled) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- AUDIO BOOST & EFFECTS CARD ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Audio Enhancements", color = textColor, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Bass Boost: ${(bassLevel * 100).toInt()}%", color = textColor, fontSize = 12.sp)
                    Slider(
                        value = bassLevel,
                        onValueChange = { 
                            bassLevel = it
                            PlaybackService.updateBassBoost((it * 1000).toInt().toShort()) 
                        }
                    )

                    Text("Virtualizer: ${(virtLevel * 100).toInt()}%", color = textColor, fontSize = 12.sp)
                    Slider(
                        value = virtLevel,
                        onValueChange = { 
                            virtLevel = it
                            PlaybackService.updateVirtualizer((it * 1000).toInt().toShort()) 
                        }
                    )

                    Text("Pre-amp Gain: ${(gainLevel * 100).toInt()}%", color = textColor, fontSize = 12.sp)
                    Slider(
                        value = gainLevel,
                        onValueChange = { 
                            gainLevel = it
                            PlaybackService.updateGain((it * 1000).toInt()) 
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- EQUALIZER BANDS CARD ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Equalizer Bands", color = textColor, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        TextButton(
                            onClick = {
                                for (i in eqBands.indices) {
                                    eqBands[i] = 0f
                                    PlaybackService.updateBand(i, 0.toShort())
                                }
                            }
                        ) {
                            Text("Flat (Reset)", color = textColor, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }

                    eqBands.forEachIndexed { index, level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = bandFreqs.getOrElse(index) { "Band ${index+1}" },
                                color = textColor,
                                fontSize = 12.sp,
                                modifier = Modifier.width(60.dp)
                            )
                            Slider(
                                value = level,
                                valueRange = -15f..15f,
                                onValueChange = { newLvl ->
                                    eqBands[index] = newLvl
                                    PlaybackService.updateBand(index, newLvl.toInt().toShort())
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun VideoList(
    videos: List<VideoItem>,
    cardBg: Color,
    textColor: Color,
    subTextColor: Color,
    onVideoSelect: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    if (videos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No videos found on device", color = subTextColor, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(videos.size) { index ->
                val video = videos[index]

                var thumbnailBitmap by remember(video.contentUri) {
                    mutableStateOf<Bitmap?>(null)
                }

                LaunchedEffect(video.contentUri) {
                    withContext(Dispatchers.IO) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                thumbnailBitmap = context.contentResolver.loadThumbnail(
                                    video.contentUri,
                                    android.util.Size(120, 120),
                                    null
                                )
                            } else {
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(context, video.contentUri)
                                thumbnailBitmap = retriever.frameAtTime
                                retriever.release()
                            }
                        } catch (e: Exception) {
                            thumbnailBitmap = null
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { onVideoSelect(video) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (thumbnailBitmap != null) {
                            Image(
                                bitmap = thumbnailBitmap!!.asImageBitmap(),
                                contentDescription = video.title,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardBg.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                color = textColor,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            val sizeMb = video.sizeBytes / (1024 * 1024)
                            Text(
                                text = "${sizeMb} MB",
                                color = subTextColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
