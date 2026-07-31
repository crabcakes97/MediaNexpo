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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.withFrameNanos
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

    private var isSearchOpen by mutableStateOf(false)
    private var searchQuery by mutableStateOf("")

    // Audiobook / Podcast smart resume + speed + pitch
    private var playbackSpeed by mutableStateOf(1.0f)
    private var playbackPitch by mutableStateOf(1.0f)
    private val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    private val pitchOptions = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f)
    private var showSpeedMenu by mutableStateOf(false)
    private var showPitchMenu by mutableStateOf(false)

    private lateinit var wifiDirect: WifiDirectShareManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
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

        // Restore last used playback speed + pitch
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        playbackSpeed = prefs.getFloat("playback_speed", 1.0f)
        playbackPitch = prefs.getFloat("playback_pitch", 1.0f)

        requestAudioPermissions()
        wifiDirect = WifiDirectShareManager(applicationContext)

        setContent {
            // Position/slider only — visualizer polls FFT by itself (no list recomposition).
            var saveCounter by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    val p = player
                    if (p != null) {
                        isPlayingState = p.isPlaying
                        currentPositionMs = p.currentPosition.coerceAtLeast(0L)
                        totalDurationMs = p.duration.coerceAtLeast(0L)
                        saveCounter++
                        if (saveCounter >= 20) {
                            saveCounter = 0
                            if (p.isPlaying) {
                                currentTrack?.let { savePlaybackPosition(it.uri, currentPositionMs) }
                            }
                        }
                    }
                    delay(200) // slider does not need high fps
                }
            }

            BackHandler(enabled = isFiltered || isSearchOpen) {
                if (isSearchOpen) {
                    isSearchOpen = false
                    searchQuery = ""
                } else {
                    resetFilter()
                }
            }

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = currentTheme.accent
                                    )
                                }
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = currentTheme.accent
                                    )
                                }
                            }
                        }

                        if (isSearchOpen) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search songs, artists, videos...", color = subTextColor) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = currentTheme.accent,
                                    unfocusedBorderColor = subTextColor.copy(alpha = 0.5f),
                                    focusedLabelColor = currentTheme.accent,
                                    cursorColor = currentTheme.accent
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor)
                                        }
                                    }
                                }
                            )
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

                        val filteredTracks = if (searchQuery.isBlank()) {
                            displayTracks
                        } else {
                            displayTracks.filter { 
                                it.title.contains(searchQuery, ignoreCase = true) || 
                                it.artist.contains(searchQuery, ignoreCase = true) ||
                                it.genre.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        val filteredArtists = if (searchQuery.isBlank()) {
                            artistsList
                        } else {
                            artistsList.filter { it.contains(searchQuery, ignoreCase = true) }
                        }

                        val filteredGenres = if (searchQuery.isBlank()) {
                            genresList
                        } else {
                            genresList.filter { it.contains(searchQuery, ignoreCase = true) }
                        }

                        val filteredVideos = if (searchQuery.isBlank()) {
                            videosList
                        } else {
                            videosList.filter { it.title.contains(searchQuery, ignoreCase = true) }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> TrackList(
                                    tracks = if (searchQuery.isBlank()) filteredTracks else filteredTracks.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) },
                                    cardBg = activeCardBg,
                                    textColor = textColor,
                                    subTextColor = subTextColor,
                                    onTrackSelect = { playTrack(it) },
                                    onTrackLongClick = { trackToAddToPlaylist = it }
                                )
                                1 -> ArtistList(
                                    artists = filteredArtists,
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
                                    genres = filteredGenres,
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
                                    videoList = filteredVideos,
                                    onVideoSelect = { video ->
                                        player?.pause()
                                        val intent = Intent(this@MainActivity, VideoPlayerActivity::class.java).apply {
                                            putExtra("EXTRA_VIDEO_URI", video.contentUri.toString())
                                        }
                                        startActivity(intent)
                                    },
                                    searchQuery = searchQuery,
                                    accentColor = currentTheme.accent
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
                                            accentColor = currentTheme.accent
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AsyncAlbumArt(
                                            uri = track.uri,
                                            existing = track.artwork,
                                            accent = currentTheme.accent,
                                            sizeDp = if (isPlayerMinimized) 36 else 52
                                        )

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
                                                onClick = {
                                                    player?.let {
                                                        if (it.isPlaying) {
                                                            currentTrack?.let { t -> savePlaybackPosition(t.uri, currentPositionMs) }
                                                            it.pause()
                                                        } else {
                                                            it.play()
                                                        }
                                                    }
                                                },
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

                                        // Speed + Pitch controls
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Speed", color = subTextColor, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box {
                                                OutlinedButton(
                                                    onClick = { showSpeedMenu = true },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.accent.copy(alpha = 0.5f))
                                                ) {
                                                    Text(
                                                        text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                                                        color = currentTheme.accent,
                                                        fontSize = 13.sp,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showSpeedMenu,
                                                    onDismissRequest = { showSpeedMenu = false },
                                                    modifier = Modifier.background(activeCardBg)
                                                ) {
                                                    speedOptions.forEach { speed ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = if (speed == 1.0f) "1x (Normal)" else "${speed}x",
                                                                    color = if (playbackSpeed == speed) currentTheme.accent else textColor,
                                                                    fontSize = 14.sp
                                                                )
                                                            },
                                                            onClick = {
                                                                setSpeed(speed)
                                                                showSpeedMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text("Pitch", color = subTextColor, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box {
                                                OutlinedButton(
                                                    onClick = { showPitchMenu = true },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.accent.copy(alpha = 0.5f))
                                                ) {
                                                    Text(
                                                        text = if (playbackPitch == 1.0f) "1x" else "${playbackPitch}x",
                                                        color = currentTheme.accent,
                                                        fontSize = 13.sp,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showPitchMenu,
                                                    onDismissRequest = { showPitchMenu = false },
                                                    modifier = Modifier.background(activeCardBg)
                                                ) {
                                                    pitchOptions.forEach { pitch ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = if (pitch == 1.0f) "1x (Normal)" else "${pitch}x",
                                                                    color = if (playbackPitch == pitch) currentTheme.accent else textColor,
                                                                    fontSize = 14.sp
                                                                )
                                                            },
                                                            onClick = {
                                                                setPitch(pitch)
                                                                showPitchMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                                Row {
                                    Button(
                                        onClick = {
                                            // Prefer Wi‑Fi Direct when active; else LAN HTTP share
                                            if (wifiDirect.isActive) {
                                                try {
                                                    val cache = java.io.File(cacheDir, "wifi_direct_out").apply { mkdirs() }
                                                    val name = track.name.ifBlank { track.title }
                                                        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                                    val out = java.io.File(cache, name)
                                                    contentResolver.openInputStream(track.uri)?.use { input ->
                                                        java.io.FileOutputStream(out).use { output -> input.copyTo(output) }
                                                    }
                                                    wifiDirect.queueSend(out, name)
                                                    trackToAddToPlaylist = null
                                                    showSettingsDialog = true
                                                } catch (e: Exception) {
                                                    android.util.Log.e("Share", "Wi‑Fi Direct send prepare failed", e)
                                                }
                                            } else {
                                                LocalShareService.start(
                                                    this@MainActivity,
                                                    shareUri = track.uri,
                                                    shareName = track.name.ifBlank { track.title }
                                                )
                                                trackToAddToPlaylist = null
                                                showSettingsDialog = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                    ) {
                                        Text(
                                            if (wifiDirect.isActive) "Share Direct" else "Share Wi‑Fi",
                                            color = Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showNewPlaylistDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                    ) {
                                        Text("New Playlist", color = Color.Black)
                                    }
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
                                            Text("Rescan Media Library", color = Color.Black)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Theme Mode:", color = subTextColor, fontSize = 14.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ThemeModeOption.values().forEach { mode ->
                                                val isSelected = themeModeOption == mode
                                                Button(
                                                    onClick = { themeModeOption = mode },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSelected) currentTheme.accent else activeCardBg,
                                                        contentColor = if (isSelected) Color.Black else textColor
                                                    ),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isSelected) Color.Transparent else subTextColor.copy(alpha = 0.3f)
                                                    )
                                                ) {
                                                    Text(
                                                        text = mode.displayName,
                                                        fontSize = 10.sp,
                                                        color = if (isSelected) Color.Black else textColor,
                                                        maxLines = 1
                                                    )
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
                                                    Text("Visualizer: ${selectedVisualizer.displayName}", color = currentTheme.accent, fontSize = 13.sp)
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
                                                    Text("Theme: ${currentTheme.displayName}", color = currentTheme.accent, fontSize = 13.sp)
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
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                        ) {
                                            Text("Select Custom Music Folder", color = Color.Black)
                                        }
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Local Wi‑Fi Sharing:", color = subTextColor, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        var shareRunning by remember {
                                            mutableStateOf(LocalShareService.isRunning)
                                        }
                                        var shareUrl by remember {
                                            mutableStateOf(LocalShareService.localUrl)
                                        }
                                        var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

                                        // Keep URL/QR in sync after service starts
                                        LaunchedEffect(shareRunning) {
                                            if (shareRunning) {
                                                for (i in 0 until 20) {
                                                    val url = LocalShareService.localUrl
                                                    if (url.isNotBlank() && !url.contains("0.0.0.0")) {
                                                        shareUrl = url
                                                        break
                                                    }
                                                    delay(100)
                                                }
                                                shareUrl = LocalShareService.localUrl
                                                if (shareUrl.isNotBlank()) {
                                                    qrBitmap = generateQrBitmap(shareUrl, 512)
                                                }
                                            } else {
                                                qrBitmap = null
                                                shareUrl = ""
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (shareRunning) {
                                                    LocalShareService.stop(this@MainActivity)
                                                    shareRunning = false
                                                } else {
                                                    LocalShareService.start(this@MainActivity)
                                                    shareRunning = true
                                                    shareUrl = LocalShareService.localUrl
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (shareRunning) Color(0xFFB00020) else currentTheme.accent
                                            )
                                        ) {
                                            Text(
                                                if (shareRunning) "Stop Receive Mode" else "Start Receive Mode",
                                                color = Color.Black
                                            )
                                        }

                                        if (shareRunning) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Others on your Wi‑Fi can open:",
                                                color = subTextColor,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = shareUrl.ifBlank { "Starting server…" },
                                                color = currentTheme.accent,
                                                fontSize = 14.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White, RoundedCornerShape(12.dp))
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (qrBitmap != null) {
                                                    Image(
                                                        bitmap = qrBitmap!!.asImageBitmap(),
                                                        contentDescription = "QR code",
                                                        modifier = Modifier.size(200.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        "Generating QR…",
                                                        color = Color.Gray,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Scan this QR on another phone (same Wi‑Fi), or type the URL.",
                                                color = subTextColor,
                                                fontSize = 11.sp
                                            )
                                            if (LocalShareService.lastReceivedName != null) {
                                                Text(
                                                    "Last received: ${LocalShareService.lastReceivedName}",
                                                    color = currentTheme.accent,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Send a song: long-press a track → Share on Wi‑Fi",
                                            color = subTextColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Wi‑Fi Direct (no router):", color = subTextColor, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(wifiDirect.status, color = currentTheme.accent, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    if (wifiDirect.isActive) wifiDirect.stop()
                                                    else wifiDirect.start()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (wifiDirect.isActive) Color(0xFFB00020) else currentTheme.accent
                                                )
                                            ) {
                                                Text(
                                                    if (wifiDirect.isActive) "Stop Direct" else "Start Direct",
                                                    color = Color.Black
                                                )
                                            }
                                            if (wifiDirect.isActive) {
                                                Button(
                                                    onClick = { wifiDirect.discover() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                                ) {
                                                    Text("Scan", color = Color.Black)
                                                }
                                            }
                                        }
                                        if (wifiDirect.peers.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Tap a device to connect:", color = subTextColor, fontSize = 12.sp)
                                            wifiDirect.peers.forEach { device ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clickable { wifiDirect.connect(device) },
                                                    colors = CardDefaults.cardColors(containerColor = activeCardBg)
                                                ) {
                                                    Column(Modifier.padding(10.dp)) {
                                                        Text(device.deviceName.ifBlank { "Unknown device" }, color = textColor, fontSize = 13.sp)
                                                        Text(device.deviceAddress, color = subTextColor, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Both phones: Start Direct → Scan → connect. Then long-press a track → Share Wi‑Fi Direct.",
                                            color = subTextColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                    item {
                                        AudioEffectsSettingsSection(textColor = textColor, subTextColor = subTextColor, cardBg = activeCardBg, accentColor = currentTheme.accent)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showSettingsDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent)
                                ) {
                                    Text("Close", color = Color.Black)
                                }
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
        // Wi‑Fi Direct
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

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
        accentColor: Color
    ) {
        // Display values (smoothed) — animate toward targets every frame
        val display = remember { FloatArray(24) { 0.08f } }
        var frameTick by remember { mutableIntStateOf(0) }

        LaunchedEffect(isPlaying) {
            if (!isPlaying) {
                for (i in display.indices) display[i] = 0.08f
                frameTick++
                return@LaunchedEffect
            }
            // Drive at display refresh — smooth lerp, not stepped FFT snaps
            while (true) {
                withFrameNanos {
                    val latest = PlaybackService.latestFftData
                    val n = minOf(display.size, if (latest.isNotEmpty()) latest.size else 0)
                    if (n > 0) {
                        for (i in 0 until n) {
                            val target = latest[i].coerceIn(0.05f, 1f)
                            // Fast attack, slower decay — looks lively
                            val speed = if (target > display[i]) 0.55f else 0.28f
                            display[i] += (target - display[i]) * speed
                        }
                    } else {
                        for (i in display.indices) {
                            display[i] += (0.08f - display[i]) * 0.2f
                        }
                    }
                    frameTick++
                }
            }
        }

        // Read frameTick so Canvas invalidates every frame
        val drawVersion = frameTick

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.25f))
        ) {
            @Suppress("UNUSED_EXPRESSION")
            drawVersion
            val width = size.width
            val height = size.height
            val barCount = 24
            val gap = 2f
            val barWidth = ((width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)

            when (style) {
                VisualizerStyle.BARS, VisualizerStyle.MIRROR -> {
                    val mid = height / 2f
                    for (i in 0 until barCount) {
                        val amp = display[i % display.size]
                        val x = i * (barWidth + gap)
                        if (style == VisualizerStyle.MIRROR) {
                            val h = (mid * amp).coerceAtLeast(2f)
                            drawRect(accentColor, Offset(x, mid - h), androidx.compose.ui.geometry.Size(barWidth, h))
                            drawRect(accentColor.copy(alpha = 0.45f), Offset(x, mid), androidx.compose.ui.geometry.Size(barWidth, h))
                        } else {
                            val barHeight = (height * amp).coerceAtLeast(3f)
                            drawRect(
                                color = accentColor,
                                topLeft = Offset(x, height - barHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }
                    }
                }
                VisualizerStyle.WAVE, VisualizerStyle.RIBBON -> {
                    val path = Path()
                    for (i in 0 until barCount) {
                        val x = (i.toFloat() / (barCount - 1)) * width
                        val y = height - (display[i] * height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    val strokeW = if (style == VisualizerStyle.RIBBON) 4f else 3f
                    drawPath(path, accentColor.copy(alpha = 0.9f), style = Stroke(width = strokeW))
                }
                VisualizerStyle.PULSE -> {
                    var sum = 0f
                    for (v in display) sum += v
                    val avg = sum / display.size
                    drawCircle(
                        color = accentColor.copy(alpha = 0.65f),
                        radius = (height / 3f) + (avg * height / 3f),
                        center = Offset(width / 2, height / 2)
                    )
                }
                VisualizerStyle.DOTS -> {
                    val cols = 16
                    val cellW = width / cols
                    for (c in 0 until cols) {
                        val amp = display[c % display.size]
                        val rows = (amp * 6).toInt().coerceIn(1, 6)
                        for (r in 0 until rows) {
                            drawCircle(
                                color = accentColor,
                                radius = cellW * 0.2f,
                                center = Offset(c * cellW + cellW / 2, height - (r + 1) * (height / 7f))
                            )
                        }
                    }
                }
                VisualizerStyle.RADAR, VisualizerStyle.PARTICLES -> {
                    val count = 16
                    val centerX = width / 2
                    val centerY = height / 2
                    val angleStep = (2 * Math.PI / count).toFloat()
                    for (i in 0 until count) {
                        val amp = display[i % display.size]
                        val base = height / 5f
                        val dist = base + amp * (height / 3f)
                        val angle = i * angleStep
                        val px = centerX + (dist * cos(angle.toDouble())).toFloat()
                        val py = centerY + (dist * sin(angle.toDouble())).toFloat()
                        val r = if (style == VisualizerStyle.PARTICLES) {
                            2.dp.toPx() + amp * 5.dp.toPx()
                        } else {
                            (amp * 4.5.dp.toPx()).coerceAtLeast(2.dp.toPx())
                        }
                        drawCircle(accentColor, r, Offset(px, py))
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
                items(
                    items = tracks,
                    key = { it.uri.toString() }
                ) { track ->
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
                            AsyncAlbumArt(
                                uri = track.uri,
                                existing = track.artwork,
                                accent = currentTheme.accent,
                                sizeDp = 40
                            )

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
                Text("Create New Playlist", color = Color.Black)
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
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/crabcakes97/MediaNexpo"))
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

    private fun decodeSampledBitmap(data: ByteArray, maxSize: Int = 256): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(data, 0, data.size, opts)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractArtwork(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                decodeSampledBitmap(embeddedPicture, 256)
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
                artwork = decodeSampledBitmap(embeddedPicture, 256)
            }
        } catch (e: Exception) {
            Log.e("LocalMusicPlayer", "Failed to extract metadata for $uri", e)
        } finally {
            retriever.release()
        }

        return MusicTrack(fallbackName, title, artist, genre, uri, artwork)
    }

    // ── Smart resume (audiobook / podcast) ──────────────────────────────
    private fun savePlaybackPosition(uri: Uri, positionMs: Long) {
        // Only save if we've listened past 5 seconds and aren't near the end
        if (positionMs < 5_000L) return
        if (totalDurationMs > 0 && positionMs > totalDurationMs - 10_000L) {
            // Near the end – clear so next open starts from beginning
            getSharedPreferences("resume_positions", MODE_PRIVATE)
                .edit().remove(uri.toString()).apply()
            return
        }
        getSharedPreferences("resume_positions", MODE_PRIVATE)
            .edit()
            .putLong(uri.toString(), positionMs)
            .apply()
    }

    private fun loadPlaybackPosition(uri: Uri): Long {
        return getSharedPreferences("resume_positions", MODE_PRIVATE)
            .getLong(uri.toString(), 0L)
    }

    private fun clearPlaybackPosition(uri: Uri) {
        getSharedPreferences("resume_positions", MODE_PRIVATE)
            .edit().remove(uri.toString()).apply()
    }

    private fun applyPlaybackParams() {
        player?.playbackParameters = androidx.media3.common.PlaybackParameters(
            playbackSpeed,
            playbackPitch
        )
    }

    private fun setSpeed(speed: Float) {
        playbackSpeed = speed
        applyPlaybackParams()
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit().putFloat("playback_speed", speed).apply()
    }

    private fun setPitch(pitch: Float) {
        playbackPitch = pitch
        applyPlaybackParams()
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit().putFloat("playback_pitch", pitch).apply()
    }

    private fun playTrack(track: MusicTrack) {
        // Save position of the previous track before switching
        currentTrack?.let { prev ->
            if (currentPositionMs > 0) savePlaybackPosition(prev.uri, currentPositionMs)
        }

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
        val resumePos = loadPlaybackPosition(track.uri)
        if (startIndex >= 0) {
            player?.seekTo(startIndex, resumePos.coerceAtLeast(0L))
        }

        applyPlaybackParams()
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

        val resumePos = loadPlaybackPosition(playlist.tracks.first().uri)
        player?.seekTo(0, resumePos.coerceAtLeast(0L))
        applyPlaybackParams()
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

    override fun onPause() {
        super.onPause()
        currentTrack?.let { savePlaybackPosition(it.uri, currentPositionMs) }
    }

    override fun onDestroy() {
        currentTrack?.let { savePlaybackPosition(it.uri, currentPositionMs) }
        super.onDestroy()
        if (::wifiDirect.isInitialized) wifiDirect.stop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}

@Composable
fun AudioEffectsSettingsSection(
    textColor: Color = Color.Black,
    subTextColor: Color = Color(0xFF444444),
    cardBg: Color = Color.White.copy(alpha = 0.3f),
    accentColor: Color = Color(0xFFBB86FC)
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
    // Android Equalizer uses millibels (−1500..1500). UI shows dB (−15..15).
    val eqBands = remember {
        mutableStateListOf(
            PlaybackService.bandLevels[0].toFloat() / 100f,
            PlaybackService.bandLevels[1].toFloat() / 100f,
            PlaybackService.bandLevels[2].toFloat() / 100f,
            PlaybackService.bandLevels[3].toFloat() / 100f,
            PlaybackService.bandLevels[4].toFloat() / 100f
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
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
                    "Enable All Audio Effects", 
                    color = textColor, 
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Switch(
                    checked = eqEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor),
                    onCheckedChange = { 
                        eqEnabled = it
                        PlaybackService.updateEqEnabled(it)
                    }
                )
            }
        }

        if (eqEnabled) {
            Spacer(modifier = Modifier.height(8.dp))

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
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.3f)),
                        onValueChange = { 
                            bassLevel = it
                            PlaybackService.updateBassBoost((it * 1000).toInt().toShort()) 
                        }
                    )

                    Text("Virtualizer: ${(virtLevel * 100).toInt()}%", color = textColor, fontSize = 12.sp)
                    Slider(
                        value = virtLevel,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.3f)),
                        onValueChange = { 
                            virtLevel = it
                            PlaybackService.updateVirtualizer((it * 1000).toInt().toShort()) 
                        }
                    )

                    Text("Pre-amp Gain: ${(gainLevel * 100).toInt()}%", color = textColor, fontSize = 12.sp)
                    Slider(
                        value = gainLevel,
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.3f)),
                        onValueChange = { 
                            gainLevel = it
                            PlaybackService.updateGain((it * 1000).toInt()) 
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                                    PlaybackService.updateBand(i, 0.toShort()) // 0 mB = flat
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
                                value = level.coerceIn(-15f, 15f),
                                valueRange = -15f..15f,
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.3f)),
                                onValueChange = { newLvl ->
                                    eqBands[index] = newLvl
                                    // Convert dB → millibels for Android Equalizer API
                                    PlaybackService.updateBand(index, (newLvl * 100f).toInt().toShort())
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${if (level >= 0) "+" else ""}${level.toInt()} dB",
                                color = subTextColor,
                                fontSize = 11.sp,
                                modifier = Modifier.width(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}




/** Load a scannable QR bitmap (public QR API — needs INTERNET). Offline falls back to text. */
private suspend fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(content, "UTF-8")
            val api = "https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&margin=1&data=$encoded"
            val conn = java.net.URL(api).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doInput = true
            conn.connect()
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            android.util.Log.e("QR", "QR fetch failed", e)
            try {
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
                val c = android.graphics.Canvas(bmp)
                c.drawColor(android.graphics.Color.WHITE)
                val p = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 28f
                    isAntiAlias = true
                }
                var y = size / 2f - 40f
                for (line in content.chunked(22)) {
                    c.drawText(line, 24f, y, p)
                    y += 36f
                }
                bmp
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class VideoFolder(
    val name: String,
    val videos: List<VideoItem>
)

// Simple in-memory caches to stop scroll jank
private val albumArtCache = android.util.LruCache<String, Bitmap>(64)
private val videoThumbCache = android.util.LruCache<String, Bitmap>(48)

@Composable
private fun AsyncAlbumArt(
    uri: Uri,
    existing: Bitmap?,
    accent: Color,
    sizeDp: Int = 40
) {
    val context = LocalContext.current
    val key = uri.toString()
    var bitmap by remember(key) {
        mutableStateOf(existing ?: albumArtCache.get(key))
    }

    LaunchedEffect(key) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val bytes = retriever.embeddedPicture
                    retriever.release()
                    if (bytes != null) {
                        // downsample
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        var sample = 1
                        while (bounds.outWidth / sample > 128 || bounds.outHeight / sample > 128) {
                            sample *= 2
                        }
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
            if (loaded != null) {
                albumArtCache.put(key, loaded)
                bitmap = loaded
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                IconMusicNote,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size((sizeDp / 2).dp)
            )
        }
    }
}

@Composable
private fun VideoThumbnail(
    contentUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val key = contentUri.toString()
    var bitmap by remember(key) { mutableStateOf(videoThumbCache.get(key)) }

    LaunchedEffect(key) {
        if (bitmap == null) {
            val frame = withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, contentUri)
                    // Scaled frame – much cheaper than full-res
                    val full = retriever.getFrameAtTime(1_000_000)
                    retriever.release()
                    if (full != null && (full.width > 320 || full.height > 320)) {
                        val scale = 320f / maxOf(full.width, full.height)
                        val w = (full.width * scale).toInt().coerceAtLeast(1)
                        val h = (full.height * scale).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(full, w, h, true).also {
                            if (it != full) full.recycle()
                        }
                    } else full
                } catch (_: Exception) {
                    null
                }
            }
            if (frame != null) {
                videoThumbCache.put(key, frame)
                bitmap = frame
            }
        }
    }

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun VideoList(
    videoList: List<VideoItem>,
    onVideoSelect: (VideoItem) -> Unit,
    searchQuery: String = "",
    accentColor: Color = Color.Cyan
) {
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val filteredVideos = videoList.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.path.contains(searchQuery, ignoreCase = true)
    }

    val videoFolders = filteredVideos.groupBy {
        java.io.File(it.path).parentFile?.name ?: "Internal Storage"
    }

    if (selectedFolder == null) {
        // TOP LEVEL – Gallery-style folder cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            videoFolders.forEach { (folderName, videosInFolder) ->
                val totalMb = videosInFolder.sumOf { it.sizeBytes } / (1024 * 1024)
                val coverUri = videosInFolder.firstOrNull()?.contentUri
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clickable { selectedFolder = folderName },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (coverUri != null) {
                                VideoThumbnail(
                                    contentUri = coverUri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Gradient overlay for text readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = folderName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${videosInFolder.size} items · $totalMb MB",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // INSIDE FOLDER – video grid + back button
        val folderVideos = videoFolders[selectedFolder] ?: emptyList()

        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { selectedFolder = null }) {
                    Text(
                        "Back",
                        color = accentColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedFolder!!,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = folderVideos,
                    key = { it.contentUri.toString() }
                ) { video ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clickable { onVideoSelect(video) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            VideoThumbnail(
                                contentUri = video.contentUri,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Bottom gradient + text
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = video.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${(video.sizeBytes / (1024 * 1024))} MB",
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
