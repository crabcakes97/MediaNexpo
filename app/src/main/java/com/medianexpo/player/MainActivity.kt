@file:OptIn(ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
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

class SnapshotStateSet<T>(initialElements: Set<T> = emptySet()) : MutableSet<T> {
    private val delegate = mutableStateOf(initialElements.toMutableSet())
    override val size: Int get() = delegate.value.size
    override fun contains(element: T): Boolean = delegate.value.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = delegate.value.containsAll(elements)
    override fun isEmpty(): Boolean = delegate.value.isEmpty()
    override fun iterator(): MutableIterator<T> = delegate.value.iterator()
    override fun add(element: T): Boolean {
        val set = delegate.value.toMutableSet()
        val added = set.add(element)
        if (added) delegate.value = set
        return added
    }
    override fun addAll(elements: Collection<T>): Boolean {
        val set = delegate.value.toMutableSet()
        val added = set.addAll(elements)
        if (added) delegate.value = set
        return added
    }
    override fun clear() {
        delegate.value = mutableSetOf()
    }
    override fun remove(element: T): Boolean {
        val set = delegate.value.toMutableSet()
        val removed = set.remove(element)
        if (removed) delegate.value = set
        return removed
    }
    override fun removeAll(elements: Collection<T>): Boolean {
        val set = delegate.value.toMutableSet()
        val removed = set.removeAll(elements)
        if (removed) delegate.value = set
        return removed
    }
    override fun retainAll(elements: Collection<T>): Boolean {
        val set = delegate.value.toMutableSet()
        val retained = set.retainAll(elements)
        if (retained) delegate.value = set
        return retained
    }
}

fun <T> mutableStateSetOf(vararg elements: T): MutableSet<T> {
    return SnapshotStateSet(mutableSetOf(*elements))
}

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
    BARS("Beat Bars"),
    MIRROR("Mirror Bass"),
    WAVE("Reactive Wave"),
    RIBBON("Neon Ribbon"),
    PULSE("Kick Pulse"),
    BEAT_RING("Beat Ring"),
    EQ_MOUNTAIN("EQ Mountain"),
    SYNTHWAVE("Synthwave"),
    STARBURST("Starburst"),
    TUNNEL("Hyperspace"),
    LIQUID("Liquid Bass"),
    VU_METERS("VU Meters"),
    KALEIDO("Kaleidoscope"),
    SPARKLINE("Sparkline Grid"),
    NEON_VORTEX("Neon Vortex"),
    CYBER_PARTICLES("Cyber Particles"),
    FREQ_POLYGON("Frequency Polygon")
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
    AMOLED_BLUE("AMOLED Blue", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFF00B4D8)),
    AMOLED_RED("AMOLED Red", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFFF3333)),
    AMOLED_GOLD("AMOLED Gold", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFFD700)),
    AMOLED_NEON("AMOLED Cyber", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFF00FFCC)),
    AMOLED_EMERALD("AMOLED Emerald", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF10B981)),
    AMOLED_AMBER("AMOLED Amber", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFFFBEB), Color(0xFFFEF08A), Color(0xFFF59E0B)),
    AMOLED_TEAL("AMOLED Teal", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFF00ACC1)),
    AMOLED_MAGENTA("AMOLED Magenta", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFAF5FF), Color(0xFFE879F9), Color(0xFFE879F9)),
    AMOLED_LIME("AMOLED Lime", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFF7FEE7), Color(0xFFECFCCB), Color(0xFF84CC16)),
    AMOLED_ICE("AMOLED Ice", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFF38BDF8)),
    AMOLED_SUNSET("AMOLED Sunset", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFB923C)),
    AMOLED_LAVENDER("AMOLED Lavender", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFFC084FC)),
    AMOLED_INDIGO("AMOLED Indigo", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFEEF2FF), Color(0xFFE0E7FF), Color(0xFF6366F1)),
    AMOLED_ROSE("AMOLED Rose", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFF43F5E)),
    AMOLED_CYAN("AMOLED Cyan", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFECFEFF), Color(0xFFCFFAFE), Color(0xFF06B6D4)),
    AMOLED_VIOLET("AMOLED Violet", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFF5F3FF), Color(0xFFEDE9FE), Color(0xFF8B5CF6)),
    AMOLED_CORAL("AMOLED Coral", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFF7849)),
    AMOLED_SLATE("AMOLED Slate", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFF64748B)),
    DYNAMIC_AURORA("Dynamic Aurora", Color(0xFF080C14), Color(0xFF101828), Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF38BDF8)),
    ELECTRIC_AMBER("Electric Amber", Color(0xFF110C05), Color(0xFF22180A), Color(0xFFFFFBEB), Color(0xFFFEF08A), Color(0xFFFF9900)),
    NEON_CYAN("Neon Cyan", Color(0xFF050E12), Color(0xFF0A1D24), Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFF00E5FF)),
    MATTE_CHERRY("Matte Cherry", Color(0xFF140709), Color(0xFF240D10), Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFFF2A6D)),
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
    DRACULA("Dracula Gothic", Color(0xFF181825), Color(0xFF282A36), Color(0xFFF8F8F2), Color(0xFFE2E2DC), Color(0xFFFF79C6)),
    STEEL_GRAY("Steel Gray", Color(0xFF111318), Color(0xFF1F242D), Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFF94A3B8)),
    NEON_ORANGE("Neon Orange", Color(0xFF140800), Color(0xFF261200), Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFF6600)),
    HOT_PINK("Hot Pink", Color(0xFF140710), Color(0xFF290E21), Color(0xFFFDF2F8), Color(0xFFFCE7F3), Color(0xFFFF1493)),
    ELECTRIC_INDIGO("Electric Indigo", Color(0xFF0A081A), Color(0xFF151033), Color(0xFFEEF2FF), Color(0xFFE0E7FF), Color(0xFF6366F1)),
    MINT_GREEN("Mint Green", Color(0xFF06150F), Color(0xFF0D291D), Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFF34D399)),
    COPPER("Copper Glow", Color(0xFF140B07), Color(0xFF29170E), Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFD97706)),
    BRONZE("Bronze Age", Color(0xFF141008), Color(0xFF292012), Color(0xFFFEFCE8), Color(0xFFFEF08A), Color(0xFFCA8A04)),
    GRAPHITE("Graphite Slate", Color(0xFF0A0A0A), Color(0xFF171717), Color(0xFFFAFAFA), Color(0xFFF5F5F5), Color(0xFFA3A3A3)),
    NEON_YELLOW("Neon Yellow", Color(0xFF141200), Color(0xFF292400), Color(0xFFFEFCE8), Color(0xFFFEF08A), Color(0xFFCCFF00)),
    BLOOD_MOON("Blood Moon", Color(0xFF1A0000), Color(0xFF330000), Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFDC2626)),
    DEEP_SAPPHIRE("Deep Sapphire", Color(0xFF020617), Color(0xFF0F172A), Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFF3B82F6)),
    RADIOACTIVE("Radioactive Green", Color(0xFF051A05), Color(0xFF0B330B), Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFF22C55E)),
    NEON_PURPLE("Neon Purple", Color(0xFF11001A), Color(0xFF240033), Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFFA855F7)),
    PLASMA_PINK("Plasma Pink", Color(0xFF1A0014), Color(0xFF330028), Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFEC4899))
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
    
    private val favoriteUris = mutableStateSetOf<String>()

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
    private var shareTargetUri by mutableStateOf<Uri?>(null)
    private var shareTargetName by mutableStateOf("")
    private var selectedPhotoItem by mutableStateOf<PhotoItem?>(null)

    private var showTagEditorDialog by mutableStateOf(false)
    private var trackToEdit by mutableStateOf<MusicTrack?>(null)

    private var currentFolderPath by mutableStateOf("MediaStore (All Storage)")
    private var selectedTab by mutableStateOf(0)
    private val videosList = mutableStateListOf<VideoItem>()
    private val photosList = mutableStateListOf<PhotoItem>()
    private val booksList = mutableStateListOf<BookItem>()
    private val audiobookChapterMap = mutableStateMapOf<String, List<AudiobookChapter>>()
    private var currentAudiobookChapters by mutableStateOf<List<AudiobookChapter>>(emptyList())
    private var currentAudiobookIndex by mutableStateOf(0)
    private var currentTheme by mutableStateOf(AppTheme.PURPLE)
    private var useMaterialYou by mutableStateOf(true)
    private var themeModeOption by mutableStateOf(ThemeModeOption.DARK)
    private var selectedVisualizer by mutableStateOf(VisualizerStyle.BARS)
    private var visualizerEnabled by mutableStateOf(true)
    private var lyricsLines by mutableStateOf<List<LyricsRepository.Line>>(emptyList())
    private var showLyricsSheet by mutableStateOf(false)

    
    // Mood playlists
    private val moodMap = mutableStateMapOf<String, String>()
    private val moodKeys = listOf("Bass Heavy", "Energetic", "Chill", "Vocal")
    private val moodIcons = mapOf(
        "Bass Heavy" to "🔊",
        "Energetic" to "⚡",
        "Chill" to "🌊",
        "Vocal" to "🎤"
    )
    
    // Playback modifiers
    private var crossfadeEnabled by mutableStateOf(false)
    private var edgeLightingEnabled by mutableStateOf(false)
    private var isSpinningArtEnabled by mutableStateOf(true)
    private var gaplessPlaybackEnabled by mutableStateOf(true)

    private var isSearchOpen by mutableStateOf(false)
    private var searchQuery by mutableStateOf("")

    private var playbackSpeed by mutableStateOf(1.0f)
    private var playbackPitch by mutableStateOf(1.0f)
    private val speedOptions = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    private val pitchOptions = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.5f, 1.7f, 2.0f)
    private var showSpeedMenu by mutableStateOf(false)
    private var showPitchMenu by mutableStateOf(false)

    private var sleepMinutesLeft by mutableStateOf(0)
    private var sleepFade by mutableStateOf(true)

    private var loopAMs by mutableStateOf<Long?>(null)
    private var loopBMs by mutableStateOf<Long?>(null)
    private var loopEnabled by mutableStateOf(false)

    private val trackBookmarks = mutableStateListOf<Long>()

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

        loadFavorites()
        loadMoods()
        loadBooks()

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

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        playbackSpeed = prefs.getFloat("playback_speed", 1.0f)
        playbackPitch = prefs.getFloat("playback_pitch", 1.0f)
        useMaterialYou = prefs.getBoolean("use_material_you", true)
        isSpinningArtEnabled = prefs.getBoolean("spinning_art_enabled", true)
        gaplessPlaybackEnabled = prefs.getBoolean("gapless_playback_enabled", true)
        crossfadeEnabled = prefs.getBoolean("crossfade_enabled", false)
        edgeLightingEnabled = prefs.getBoolean("edge_lighting_enabled", false)
        visualizerEnabled = prefs.getBoolean("visualizer_enabled", true)

        requestAudioPermissions()
        wifiDirect = WifiDirectShareManager(applicationContext)

        setContent {
            val context = LocalContext.current
            var saveCounter by remember { mutableIntStateOf(0) }
            var volumeSetByCrossfade by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                while (true) {
                    val p = player
                    if (p != null) {
                        isPlayingState = p.isPlaying
                        currentPositionMs = p.currentPosition.coerceAtLeast(0L)
                        totalDurationMs = p.duration.coerceAtLeast(0L)

                        if (crossfadeEnabled && isPlayingState && totalDurationMs > 0 && sleepMinutesLeft <= 0) {
                            val remaining = totalDurationMs - currentPositionMs
                            val targetVol = when {
                                remaining < 4000L -> (remaining / 4000f).coerceIn(0f, 1f)
                                currentPositionMs < 4000L -> (currentPositionMs / 4000f).coerceIn(0f, 1f)
                                else -> 1f
                            }
                            p.volume = targetVol
                            volumeSetByCrossfade = true
                        } else if (volumeSetByCrossfade && sleepMinutesLeft <= 0) {
                            p.volume = 1f
                            volumeSetByCrossfade = false
                        }

                        if (loopEnabled) {
                            val a = loopAMs
                            val b = loopBMs
                            if (a != null && b != null && b > a && currentPositionMs >= b) {
                                p.seekTo(a)
                                currentPositionMs = a
                            }
                        }

                        saveCounter++
                        if (saveCounter >= 20) {
                            saveCounter = 0
                            if (p.isPlaying) {
                                currentTrack?.let { savePlaybackPosition(it.uri, currentPositionMs) }
                            }
                        }
                    }
                    delay(200)
                }
            }

            LaunchedEffect(sleepMinutesLeft, isPlayingState) {
                if (sleepMinutesLeft <= 0) return@LaunchedEffect
                while (sleepMinutesLeft > 0) {
                    if (!isPlayingState) {
                        delay(500)
                        continue
                    }
                    delay(60_000)
                    if (sleepMinutesLeft <= 0) break
                    sleepMinutesLeft -= 1
                    if (sleepMinutesLeft <= 0) {
                        if (sleepFade) {
                            var vol = 1f
                            while (vol > 0.05f) {
                                vol -= 0.1f
                                player?.volume = vol.coerceIn(0f, 1f)
                                delay(200)
                            }
                        }
                        player?.pause()
                        player?.volume = 1f
                        sleepMinutesLeft = 0
                    }
                }
            }

            LaunchedEffect(isPlayingState, currentTrack) {
                while (true) {
                    delay(1000)
                    if (isPlayingState) {
                        currentTrack?.let { track ->
                            analyzeAndSetMood(track.uri, PlaybackService.latestFftData)
                        }
                    }
                }
            }

            BackHandler(enabled = isFiltered || isSearchOpen || selectedPhotoItem != null) {
                if (selectedPhotoItem != null) {
                    selectedPhotoItem = null
                } else if (isSearchOpen) {
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

            val dynamicColorScheme = when {
                useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                else -> null
            }
            val dynamicAccent = dynamicColorScheme?.primary ?: currentTheme.accent
            val effectiveAccent = if (useMaterialYou) dynamicAccent else currentTheme.accent

            val appColorScheme = dynamicColorScheme ?: if (isDarkTheme) darkColorScheme(background = activeBg, primary = effectiveAccent) else lightColorScheme(background = activeBg, primary = effectiveAccent)

            MaterialTheme(colorScheme = appColorScheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize(), color = activeBg) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = activeCardBg,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(effectiveAccent)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("MediaNexpo", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = effectiveAccent)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = effectiveAccent
                                            )
                                        }
                                        IconButton(onClick = { showLyricsSheet = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Lyrics",
                                                tint = if (lyricsLines.isNotEmpty()) effectiveAccent else subTextColor
                                            )
                                        }
                                        IconButton(onClick = { showSettingsDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = effectiveAccent
                                            )
                                        }
                                    }
                                }
                            }

                            if (isSearchOpen) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search songs, artists, videos, favorites...", color = subTextColor) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = effectiveAccent,
                                        unfocusedBorderColor = subTextColor.copy(alpha = 0.5f),
                                        focusedLabelColor = effectiveAccent,
                                        cursorColor = effectiveAccent
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
                                    color = effectiveAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (isFiltered) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Filter: $activeFilterName", color = effectiveAccent, fontSize = 13.sp)
                                    TextButton(onClick = { resetFilter() }) {
                                        Text("Show All Songs ✕", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }

                            val tabs = listOf("Songs", "Artists", "Genres", "Playlists", "Videos", "Photos", "Favorites", "Recent", "Moods", "Books", "About")
                            val pagerState = rememberPagerState(pageCount = { tabs.size })
                            val coroutineScope = rememberCoroutineScope()
                            val tabListState = androidx.compose.foundation.lazy.rememberLazyListState()

                            // Keep selected chip in view when swiping pages — no manual top-bar scroll
                            LaunchedEffect(pagerState.currentPage) {
                                selectedTab = pagerState.currentPage
                                tabListState.animateScrollToItem(
                                    index = pagerState.currentPage,
                                    scrollOffset = -80
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = activeCardBg,
                                shadowElevation = 2.dp
                            ) {
                                androidx.compose.foundation.lazy.LazyRow(
                                    state = tabListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(tabs.size) { index ->
                                        val isSelected = selectedTab == index
                                        val chipBg = if (isSelected) effectiveAccent else Color.Transparent
                                        val chipTextColor = if (isSelected) Color.Black else textColor

                                        androidx.compose.material3.Surface(
                                            onClick = { 
                                                selectedTab = index
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                    tabListState.animateScrollToItem(index, scrollOffset = -80)
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = chipBg,
                                            shadowElevation = if (isSelected) 2.dp else 0.dp
                                        ) {
                                            Text(
                                                text = tabs[index],
                                                color = chipTextColor,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val filteredTracks = remember(searchQuery, displayTracks.size, displayTracks) {
                                if (searchQuery.isBlank()) displayTracks.toList()
                                else displayTracks.filter {
                                    it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.artist.contains(searchQuery, ignoreCase = true) ||
                                    it.genre.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            val filteredArtists = remember(searchQuery, artistsList.size) {
                                if (searchQuery.isBlank()) artistsList.toList()
                                else artistsList.filter { it.contains(searchQuery, ignoreCase = true) }
                            }

                            val filteredGenres = remember(searchQuery, genresList.size) {
                                if (searchQuery.isBlank()) genresList.toList()
                                else genresList.filter { it.contains(searchQuery, ignoreCase = true) }
                            }

                            val filteredVideos = remember(searchQuery, videosList.size) {
                                if (searchQuery.isBlank()) videosList.toList()
                                else videosList.filter { it.title.contains(searchQuery, ignoreCase = true) }
                            }

                            val filteredPhotos = remember(searchQuery, photosList.size) {
                                if (searchQuery.isBlank()) photosList.toList()
                                else photosList.filter { it.title.contains(searchQuery, ignoreCase = true) }
                            }

                            HorizontalPager(
                                state = pagerState,
                                beyondBoundsPageCount = 1,
                                modifier = Modifier.weight(1f)
                            ) { page ->
                                when (page) {
                                    0 -> TrackList(
                                        tracks = filteredTracks,
                                        cardBg = activeCardBg,
                                        textColor = textColor,
                                        subTextColor = subTextColor,
                                        accentColor = effectiveAccent,
                                        currentTrack = currentTrack,
                                        favoriteUris = favoriteUris,
                                        moodMap = moodMap,
                                        moodIcons = moodIcons,
                                        onTrackSelect = { playTrack(it) },
                                        onTrackLongClick = { trackToAddToPlaylist = it },
                                        onToggleFavorite = { toggleFavorite(it.uri) },
                                        onEditTrack = { track ->
                                            trackToEdit = track
                                            showTagEditorDialog = true
                                        }
                                    )
                                    1 -> ArtistList(
                                        artists = filteredArtists,
                                        themeAccent = effectiveAccent,
                                        cardBg = activeCardBg,
                                        textColor = textColor,
                                        onArtistClick = { artistName ->
                                            displayTracks.clear()
                                            displayTracks.addAll(allTracks.filter { it.artist.equals(artistName, ignoreCase = true) })
                                            isFiltered = true
                                            activeFilterName = "Artist: $artistName"
                                            selectedTab = 0
                                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                    )
                                    2 -> GenreList(
                                        genres = filteredGenres,
                                        themeAccent = effectiveAccent,
                                        cardBg = activeCardBg,
                                        textColor = textColor,
                                        onGenreClick = { genreName ->
                                            displayTracks.clear()
                                            displayTracks.addAll(allTracks.filter { it.genre.equals(genreName, ignoreCase = true) })
                                            isFiltered = true
                                            activeFilterName = "Genre: $genreName"
                                            selectedTab = 0
                                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                    )
                                    3 -> PlaylistView(
                                        playlists = playlistsList,
                                        themeAccent = effectiveAccent,
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
                                        favoriteUris = favoriteUris,
                                        onVideoSelect = { video ->
                                            player?.pause()
                                            val intent = Intent(this@MainActivity, VideoPlayerActivity::class.java).apply {
                                                putExtra("EXTRA_VIDEO_URI", video.contentUri.toString())
                                            }
                                            startActivity(intent)
                                        },
                                        onVideoLongClick = { video ->
                                            shareTargetUri = video.contentUri
                                            shareTargetName = video.title
                                        },
                                        onToggleFavorite = { toggleFavorite(it.contentUri) },
                                        searchQuery = searchQuery,
                                        accentColor = effectiveAccent
                                    )
                                    5 -> PhotoList(
                                        photoList = filteredPhotos,
                                        favoriteUris = favoriteUris,
                                        onPhotoSelect = { photo -> selectedPhotoItem = photo },
                                        onPhotoLongClick = { photo ->
                                            shareTargetUri = photo.contentUri
                                            shareTargetName = photo.title
                                        },
                                        onToggleFavorite = { toggleFavorite(it.contentUri) },
                                        searchQuery = searchQuery,
                                        accentColor = effectiveAccent
                                    )
                                    6 -> FavoritesView(
                                        allTracks = allTracks,
                                        videosList = videosList,
                                        photosList = photosList,
                                        favoriteUris = favoriteUris,
                                        cardBg = activeCardBg,
                                        textColor = textColor,
                                        subTextColor = subTextColor,
                                        accentColor = effectiveAccent,
                                        onTrackSelect = { playTrack(it) },
                                        onVideoSelect = { video ->
                                            player?.pause()
                                            val intent = Intent(this@MainActivity, VideoPlayerActivity::class.java).apply {
                                                putExtra("EXTRA_VIDEO_URI", video.contentUri.toString())
                                            }
                                            startActivity(intent)
                                        },
                                        onPhotoSelect = { selectedPhotoItem = it },
                                        onToggleFavorite = { toggleFavorite(it) }
                                    )
                                    7 -> {
                                        var recentTick by remember { mutableIntStateOf(0) }
                                        LaunchedEffect(currentTrack) { recentTick++ }
                                        val recentEntries = remember(recentTick) {
                                            RecentlyPlayedStore.load(this@MainActivity)
                                        }
                                        RecentView(
                                            entries = recentEntries,
                                            cardBg = activeCardBg,
                                            textColor = textColor,
                                            subTextColor = subTextColor,
                                            accentColor = effectiveAccent,
                                            onPlay = { entry ->
                                                val track = allTracks.find { it.uri.toString() == entry.uri }
                                                if (track != null) playTrack(track)
                                                else playTrack(
                                                    MusicTrack(
                                                        name = entry.title,
                                                        title = entry.title,
                                                        artist = entry.artist,
                                                        genre = "Recent",
                                                        uri = android.net.Uri.parse(entry.uri)
                                                    )
                                                )
                                            },
                                            onClear = {
                                                RecentlyPlayedStore.clear(this@MainActivity)
                                                recentTick++
                                            }
                                        )
                                    }
                                    8 -> MoodsView(
                                        moodKeys = moodKeys,
                                        moodMap = moodMap,
                                        moodIcons = moodIcons,
                                        allTracks = allTracks,
                                        themeAccent = effectiveAccent,
                                        cardBg = activeCardBg,
                                        textColor = textColor,
                                        onMoodClick = { moodName ->
                                            displayTracks.clear()
                                            displayTracks.addAll(allTracks.filter { moodMap[it.uri.toString()] == moodName })
                                            isFiltered = true
                                            activeFilterName = "Mood: $moodName"
                                            selectedTab = 0
                                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                    )
                                    9 -> BooksView(
                                        books = booksList,
                                        themeAccent = effectiveAccent,
                                        cardBg = activeCardBg,
                                        textColor = textColor,
                                        onEpubClick = { book ->
                                            val intent = Intent(this@MainActivity, EpubReaderActivity::class.java).apply {
                                                putExtra("EPUB_URI", book.contentUri.toString())
                                            }
                                            startActivity(intent)
                                        },
                                        onAudiobookClick = { book ->
                                            val chapters = audiobookChapterMap[book.path] ?: emptyList()
                                            if (chapters.isNotEmpty()) {
                                                currentAudiobookChapters = chapters
                                                currentAudiobookIndex = 0
                                                val first = chapters[0]
                                                val resumePrefs = getSharedPreferences("audiobook_resume", MODE_PRIVATE)
                                                val resumePos = resumePrefs.getLong(book.path, 0L)
                                                playTrack(
                                                    MusicTrack(
                                                        name = first.title,
                                                        title = first.title,
                                                        artist = book.title,
                                                        genre = "Audiobook",
                                                        uri = first.uri
                                                    )
                                                )
                                                if (resumePos > 0) {
                                                    player?.seekTo(resumePos)
                                                }
                                            }
                                        },
                                        onAddBook = { folderPicker.launch(null) }
                                    )
                                    10 -> AboutView(themeAccent = effectiveAccent, cardBg = activeCardBg, textColor = textColor)
                                }
                            }

                            CurrentPlayerSection(
                                currentTrack = currentTrack,
                                isPlayingState = isPlayingState,
                                isPlayerMinimized = isPlayerMinimized,
                                onToggleMinimize = { isPlayerMinimized = !isPlayerMinimized },
                                currentPositionMs = currentPositionMs,
                                totalDurationMs = totalDurationMs,
                                activeCardBg = activeCardBg,
                                textColor = textColor,
                                subTextColor = subTextColor,
                                accentColor = effectiveAccent,
                                selectedVisualizer = selectedVisualizer,
                                visualizerEnabled = visualizerEnabled,
                                isSpinningArtEnabled = isSpinningArtEnabled,
                                onToggleSpinningArt = { enabled ->
                                    isSpinningArtEnabled = enabled
                                    getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("spinning_art_enabled", enabled).apply()
                                },
                                gaplessPlaybackEnabled = gaplessPlaybackEnabled,
                                onToggleGapless = { enabled ->
                                    gaplessPlaybackEnabled = enabled
                                    getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("gapless_playback_enabled", enabled).apply()
                                },
                                isShuffleEnabled = isShuffleEnabled,
                                repeatModeState = repeatModeState,
                                playbackSpeed = playbackSpeed,
                                playbackPitch = playbackPitch,
                                speedOptions = speedOptions,
                                pitchOptions = pitchOptions,
                                showSpeedMenu = showSpeedMenu,
                                onShowSpeedMenuChange = { showSpeedMenu = it },
                                showPitchMenu = showPitchMenu,
                                onShowPitchMenuChange = { showPitchMenu = it },
                                sleepMinutesLeft = sleepMinutesLeft,
                                loopEnabled = loopEnabled,
                                loopAMs = loopAMs,
                                loopBMs = loopBMs,
                                trackBookmarks = trackBookmarks,
                                onSeek = { seekTarget ->
                                    player?.seekTo(seekTarget)
                                    currentPositionMs = seekTarget
                                },
                                onPlayPause = {
                                    player?.let {
                                        if (it.isPlaying) {
                                            currentTrack?.let { t -> savePlaybackPosition(t.uri, currentPositionMs) }
                                            it.pause()
                                        } else {
                                            it.play()
                                        }
                                    }
                                },
                                onPrevious = { player?.let { if (it.hasPreviousMediaItem()) it.seekToPrevious() } },
                                onNext = { player?.let { if (it.hasNextMediaItem()) it.seekToNext() } },
                                onToggleShuffle = { toggleShuffle() },
                                onCycleRepeat = { cycleRepeatMode() },
                                onSetSpeed = { setSpeed(it) },
                                onSetPitch = { setPitch(it) },
                                onSetSleep = { sleepMinutesLeft = it },
                                onSetLoop = { a, b, enabled ->
                                    loopAMs = a
                                    loopBMs = b
                                    loopEnabled = enabled
                                },
                                onClearBookmarks = { trackBookmarks.clear() },
                                onAddBookmark = { mark ->
                                    if (trackBookmarks.none { kotlin.math.abs(it - mark) < 1500 }) {
                                        trackBookmarks.add(mark)
                                        trackBookmarks.sort()
                                    }
                                },
                                onOpenLyrics = { showLyricsSheet = true },
                                hasLyrics = lyricsLines.isNotEmpty()
                            )
                        }

                        selectedPhotoItem?.let { photo ->
                            FullscreenImageView(
                                uri = photo.contentUri,
                                title = photo.title,
                                accentColor = effectiveAccent,
                                isFavorite = favoriteUris.contains(photo.contentUri.toString()),
                                onDismiss = { selectedPhotoItem = null },
                                onShare = {
                                    shareTargetUri = photo.contentUri
                                    shareTargetName = photo.title
                                },
                                onToggleFavorite = { toggleFavorite(photo.contentUri) }
                            )
                        }

                        if (showLyricsSheet) {
                            val idx = LyricsRepository.lineAt(lyricsLines, currentPositionMs)
                            AlertDialog(
                                onDismissRequest = { showLyricsSheet = false },
                                title = { Text("Lyrics", color = textColor) },
                                text = {
                                    if (lyricsLines.isEmpty()) {
                                        Text(
                                            "No .lrc file found for this track.\nPlace a file named SongName.lrc next to the audio.",
                                            color = subTextColor
                                        )
                                    } else {
                                        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                                            items(lyricsLines.size) { i ->
                                                val line = lyricsLines[i]
                                                Text(
                                                    line.text,
                                                    color = if (i == idx) effectiveAccent else textColor,
                                                    fontSize = if (i == idx) 16.sp else 13.sp,
                                                    fontWeight = if (i == idx) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showLyricsSheet = false }) {
                                        Text("Close", color = effectiveAccent)
                                    }
                                },
                                containerColor = activeCardBg
                            )
                        }

                        if (showTagEditorDialog && trackToEdit != null) {
                            var editTitle by remember { mutableStateOf(trackToEdit!!.title) }
                            var editArtist by remember { mutableStateOf(trackToEdit!!.artist) }
                            var editGenre by remember { mutableStateOf(trackToEdit!!.genre) }

                            AlertDialog(
                                onDismissRequest = { showTagEditorDialog = false },
                                title = { Text("Edit Song Metadata", color = textColor) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = editTitle,
                                            onValueChange = { editTitle = it },
                                            label = { Text("Title", color = subTextColor) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = effectiveAccent, unfocusedBorderColor = subTextColor, focusedLabelColor = effectiveAccent, cursorColor = effectiveAccent)
                                        )
                                        OutlinedTextField(
                                            value = editArtist,
                                            onValueChange = { editArtist = it },
                                            label = { Text("Artist", color = subTextColor) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = effectiveAccent, unfocusedBorderColor = subTextColor, focusedLabelColor = effectiveAccent, cursorColor = effectiveAccent)
                                        )
                                        OutlinedTextField(
                                            value = editGenre,
                                            onValueChange = { editGenre = it },
                                            label = { Text("Genre", color = subTextColor) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = effectiveAccent, unfocusedBorderColor = subTextColor, focusedLabelColor = effectiveAccent, cursorColor = effectiveAccent)
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val idx = allTracks.indexOf(trackToEdit)
                                            if (idx >= 0) {
                                                val updated = trackToEdit!!.copy(title = editTitle, artist = editArtist, genre = editGenre)
                                                allTracks[idx] = updated
                                                val dispIdx = displayTracks.indexOf(trackToEdit)
                                                if (dispIdx >= 0) displayTracks[dispIdx] = updated
                                                if (currentTrack == trackToEdit) currentTrack = updated
                                                saveLibraryToCache(allTracks)
                                            }
                                            showTagEditorDialog = false
                                            trackToEdit = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Save", color = Color.Black)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTagEditorDialog = false; trackToEdit = null }) {
                                        Text("Cancel", color = subTextColor)
                                    }
                                },
                                containerColor = activeCardBg
                            )
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
                                            focusedBorderColor = effectiveAccent,
                                            unfocusedBorderColor = subTextColor,
                                            focusedLabelColor = effectiveAccent,
                                            cursorColor = effectiveAccent
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
                                        colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                        shape = RoundedCornerShape(12.dp)
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

                        shareTargetUri?.let { uri ->
                            AlertDialog(
                                onDismissRequest = { shareTargetUri = null },
                                title = { Text("Share", color = textColor) },
                                text = {
                                    Column {
                                        Text(shareTargetName, color = textColor, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("LAN: same Wi‑Fi · Direct: peer-to-peer", color = subTextColor, fontSize = 12.sp)
                                    }
                                },
                                confirmButton = {
                                    Row {
                                        Button(
                                            onClick = {
                                                if (wifiDirect.isActive) {
                                                    try {
                                                        val cache = java.io.File(cacheDir, "wifi_direct_out").apply { mkdirs() }
                                                        val name = shareTargetName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "shared" }
                                                        val out = java.io.File(cache, name)
                                                        contentResolver.openInputStream(uri)?.use { input ->
                                                            java.io.FileOutputStream(out).use { output -> input.copyTo(output) }
                                                        }
                                                        wifiDirect.queueSend(out, name)
                                                        shareTargetUri = null
                                                        showSettingsDialog = true
                                                    } catch (e: Exception) {
                                                        Log.e("Share", "Direct prepare failed", e)
                                                    }
                                                } else {
                                                    LocalShareService.start(this@MainActivity, shareUri = uri, shareName = shareTargetName)
                                                    shareTargetUri = null
                                                    showSettingsDialog = true
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (wifiDirect.isActive) "Share Direct" else "Share LAN", color = Color.Black)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(onClick = { shareTargetUri = null }) {
                                            Text("Cancel", color = textColor)
                                        }
                                    }
                                },
                                dismissButton = {},
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
                                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
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
                                                    colors = CardDefaults.cardColors(containerColor = activeCardBg),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(IconMusicNote, contentDescription = null, tint = effectiveAccent)
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
                                                if (wifiDirect.isActive) {
                                                    try {
                                                        val cache = java.io.File(cacheDir, "wifi_direct_out").apply { mkdirs() }
                                                        val name = track.name.ifBlank { track.title }.replace(Regex("[^a-zA-Z0-9._-]"), "_")
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
                                            colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (wifiDirect.isActive) "Share Direct" else "Share Wi‑Fi", color = Color.Black)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { showNewPlaylistDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                            shape = RoundedCornerShape(12.dp)
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
                                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                                        item {
                                            Button(
                                                onClick = {
                                                    showSettingsDialog = false
                                                    val savedUriStr = getSharedPreferences("prefs", MODE_PRIVATE).getString("folder_uri", null)
                                                    val uri = savedUriStr?.let { Uri.parse(it) }
                                                    startDeepLibraryScan(uri)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Rescan Media Library", color = Color.Black)
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = activeCardBg),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Material You (Wallpaper Colors)", color = textColor, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                        Text("Extract theme colors from system wallpaper", color = subTextColor, fontSize = 11.sp)
                                                    }
                                                    Switch(
                                                        checked = useMaterialYou,
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = effectiveAccent),
                                                        onCheckedChange = { 
                                                            useMaterialYou = it
                                                            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("use_material_you", it).apply()
                                                        }
                                                    )
                                                }
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
                                                            containerColor = if (isSelected) effectiveAccent else activeCardBg,
                                                            contentColor = if (isSelected) Color.Black else textColor
                                                        ),
                                                        shape = RoundedCornerShape(8.dp)
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
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Visualizer: ${selectedVisualizer.displayName}", color = effectiveAccent, fontSize = 13.sp)
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = effectiveAccent)
                                                    }
                                                }
                                                DropdownMenu(
                                                    expanded = visExpanded,
                                                    onDismissRequest = { visExpanded = false },
                                                    modifier = Modifier.background(activeCardBg).heightIn(max = 280.dp)
                                                ) {
                                                    VisualizerStyle.values().forEach { style ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = style.displayName,
                                                                    color = if (selectedVisualizer == style) effectiveAccent else textColor,
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

                                        if (!useMaterialYou) {
                                            item {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Accent Color Palette (55 Themes):", color = subTextColor, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.height(4.dp))

                                                var themeExpanded by remember { mutableStateOf(false) }
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    OutlinedButton(
                                                        onClick = { themeExpanded = true },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text("Palette: ${currentTheme.displayName}", color = effectiveAccent, fontSize = 13.sp)
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                                    .background(currentTheme.accent)
                                                            ) {}
                                                        }
                                                    }
                                                    DropdownMenu(
                                                        expanded = themeExpanded,
                                                        onDismissRequest = { themeExpanded = false },
                                                        modifier = Modifier.background(activeCardBg).heightIn(max = 280.dp)
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
                                                                        ) {}
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
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Visualizer", color = textColor, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = activeCardBg),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Show Music Visualizer", color = textColor, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                        Text("When off, album art expands to fill the space", color = subTextColor, fontSize = 10.sp)
                                                    }
                                                    Switch(
                                                        checked = visualizerEnabled,
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = effectiveAccent),
                                                        onCheckedChange = {
                                                            visualizerEnabled = it
                                                            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("visualizer_enabled", it).apply()
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Playback & Timers", color = textColor, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))

                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = activeCardBg),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Smooth Fade Transitions", color = textColor, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                        Text("Fade volume in/out at the start and end of songs", color = subTextColor, fontSize = 10.sp)
                                                    }
                                                    Switch(
                                                        checked = crossfadeEnabled,
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = effectiveAccent),
                                                        onCheckedChange = { 
                                                            crossfadeEnabled = it 
                                                            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("crossfade_enabled", it).apply()
                                                        }
                                                    )
                                                }
                                            }

                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = activeCardBg),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Gapless Playback", color = textColor, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                        Text("Eliminate silence gaps between adjacent tracks", color = subTextColor, fontSize = 10.sp)
                                                    }
                                                    Switch(
                                                        checked = gaplessPlaybackEnabled,
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = effectiveAccent),
                                                        onCheckedChange = { 
                                                            gaplessPlaybackEnabled = it 
                                                            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("gapless_playback_enabled", it).apply()
                                                        }
                                                    )
                                                }
                                            }

                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = activeCardBg),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Sleep Timer", color = textColor, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                        val sleepOpts = listOf(0, 5, 15, 30, 45, 60, 90)
                                                        var showSleepMenuLocal by remember { mutableStateOf(false) }
                                                        Box {
                                                            OutlinedButton(
                                                                onClick = { showSleepMenuLocal = true },
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Text(if (sleepMinutesLeft > 0) "$sleepMinutesLeft min" else "Off", color = effectiveAccent, fontSize = 12.sp)
                                                            }
                                                            DropdownMenu(expanded = showSleepMenuLocal, onDismissRequest = { showSleepMenuLocal = false }) {
                                                                sleepOpts.forEach { m ->
                                                                    DropdownMenuItem(
                                                                        text = { Text(if (m == 0) "Off" else "$m minutes") },
                                                                        onClick = { sleepMinutesLeft = m; showSleepMenuLocal = false }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Fade out when sleep timer ends", color = subTextColor, fontSize = 12.sp)
                                                        Switch(
                                                            checked = sleepFade,
                                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = effectiveAccent),
                                                            onCheckedChange = { sleepFade = it }
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Cool Extras", color = textColor, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))

                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = activeCardBg),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Neon Edge Lighting (Party Mode)", color = textColor, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                        Text("Screen borders glow and throb to the bass", color = subTextColor, fontSize = 10.sp)
                                                    }
                                                    Switch(
                                                        checked = edgeLightingEnabled,
                                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = effectiveAccent),
                                                        onCheckedChange = { 
                                                            edgeLightingEnabled = it 
                                                            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("edge_lighting_enabled", it).apply()
                                                        }
                                                    )
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
                                                colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Select Custom Music Folder", color = Color.Black)
                                            }
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Local Wi‑Fi Sharing:", color = subTextColor, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(4.dp))

                                            var shareRunning by remember { mutableStateOf(LocalShareService.isRunning) }
                                            var shareUrl by remember { mutableStateOf(LocalShareService.localUrl) }
                                            var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
                                                    containerColor = if (shareRunning) Color(0xFFB00020) else effectiveAccent
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(if (shareRunning) "Stop Receive Mode" else "Start Receive Mode", color = Color.Black)
                                            }

                                            if (shareRunning) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Others on your Wi‑Fi can open:", color = subTextColor, fontSize = 12.sp)
                                                Text(
                                                    text = shareUrl.ifBlank { "Starting server…" },
                                                    color = effectiveAccent,
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
                                                        Text("Generating QR…", color = Color.Gray, fontSize = 12.sp)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("Scan this QR on another phone (same Wi‑Fi), or type the URL.", color = subTextColor, fontSize = 11.sp)
                                                if (LocalShareService.lastReceivedName != null) {
                                                    Text("Last received: ${LocalShareService.lastReceivedName}", color = effectiveAccent, fontSize = 12.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Send a song: long-press a track → Share on Wi‑Fi", color = subTextColor, fontSize = 11.sp)
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Wi‑Fi Direct (no router):", color = subTextColor, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(wifiDirect.status, color = effectiveAccent, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        if (wifiDirect.isActive) wifiDirect.stop()
                                                        else wifiDirect.start()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (wifiDirect.isActive) Color(0xFFB00020) else effectiveAccent
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(if (wifiDirect.isActive) "Stop Direct" else "Start Direct", color = Color.Black)
                                                }
                                                if (wifiDirect.isActive) {
                                                    Button(
                                                        onClick = { wifiDirect.discover() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                                        shape = RoundedCornerShape(12.dp)
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
                                                        colors = CardDefaults.cardColors(containerColor = activeCardBg),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Column(Modifier.padding(10.dp)) {
                                                            Text(device.deviceName.ifBlank { "Unknown device" }, color = textColor, fontSize = 13.sp)
                                                            Text(device.deviceAddress, color = subTextColor, fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Both phones: Start Direct → Scan → connect. Then long-press a track → Share Wi‑Fi Direct.", color = subTextColor, fontSize = 11.sp)
                                        }
                                        item {
                                            AudioEffectsSettingsSection(textColor = textColor, subTextColor = subTextColor, cardBg = activeCardBg, accentColor = effectiveAccent)
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showSettingsDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = effectiveAccent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Close", color = Color.Black)
                                    }
                                },
                                containerColor = activeCardBg
                            )
                        }
                    }

                    if (edgeLightingEnabled && isPlayingState) {
                        MainActivityInstanceHelper.EdgeLightingView(accentColor = effectiveAccent)
                    }
                }
            }
        }
    }

    private fun loadFavorites() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val set = prefs.getStringSet("favorite_uris", emptySet()) ?: emptySet()
        favoriteUris.clear()
        favoriteUris.addAll(set)
    }

    private fun saveFavorites() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        prefs.edit().putStringSet("favorite_uris", favoriteUris.toSet()).apply()
    }

    private fun loadMoods() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val json = prefs.getString("mood_map", null) ?: return
        try {
            val obj = JSONObject(json)
            moodMap.clear()
            obj.keys().forEach { key ->
                moodMap[key] = obj.getString(key)
            }
        } catch (_: Exception) { }
    }

    private fun saveMoods() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val obj = JSONObject()
        moodMap.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("mood_map", obj.toString()).apply()
    }

    private fun analyzeAndSetMood(uri: Uri, fftData: FloatArray) {
        if (fftData.size < 32) return
        val bass = (fftData[0] + fftData[1] + fftData[2] + fftData[3]) / 4f
        val mid = (fftData[8] + fftData[9] + fftData[10] + fftData[11] + fftData[12]) / 5f
        val high = (fftData[20] + fftData[22] + fftData[24] + fftData[28]) / 4f
        val energy = fftData.average().toFloat()
        val mood = when {
            bass > 0.65f && bass > mid * 1.3f -> "Bass Heavy"
            energy > 0.55f && high > 0.4f -> "Energetic"
            energy < 0.35f && mid < 0.4f -> "Chill"
            mid > 0.45f && high > 0.3f -> "Vocal"
            else -> return
        }
        val key = uri.toString()
        if (moodMap[key] != mood) {
            moodMap[key] = mood
            saveMoods()
        }
    }

    private fun loadBooks() {
        GlobalScope.launch(Dispatchers.IO) {
            val books = BookRepository.scanEpubFiles(this@MainActivity) +
                BookRepository.scanAudiobookFolders(this@MainActivity)
            withContext(Dispatchers.Main) {
                booksList.clear()
                booksList.addAll(books.distinctBy { it.contentUri.toString() })
                booksList.filter { it.isAudiobook }.forEach { book ->
                    val chapters = BookRepository.getAudiobookChapters(this@MainActivity, book.path)
                    if (chapters.isNotEmpty()) {
                        audiobookChapterMap[book.path] = chapters
                    }
                }
            }
        }
    }

    private fun toggleFavorite(uri: Uri) {
        val str = uri.toString()
        if (favoriteUris.contains(str)) {
            favoriteUris.remove(str)
        } else {
            favoriteUris.add(str)
        }
        saveFavorites()
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
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissions.add(Manifest.permission.RECORD_AUDIO)
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
                val scannedPhotos = PhotoRepository.scanLocalPhotos(this@MainActivity)
                photosList.clear()
                photosList.addAll(scannedPhotos)
                true
            } else false
        } catch (e: Exception) {
            Log.e("LocalMusicPlayer", "Failed to load cached library", e)
            false
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun TrackList(
        tracks: List<MusicTrack>,
        cardBg: Color,
        textColor: Color,
        subTextColor: Color,
        accentColor: Color,
        currentTrack: MusicTrack?,
        favoriteUris: Set<String>,
        moodMap: Map<String, String> = emptyMap(),
        moodIcons: Map<String, String> = emptyMap(),
        onTrackSelect: (MusicTrack) -> Unit,
        onTrackLongClick: (MusicTrack) -> Unit,
        onToggleFavorite: (MusicTrack) -> Unit,
        onEditTrack: (MusicTrack) -> Unit
    ) {
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tracks found.", color = subTextColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = tracks,
                    key = { it.uri.toString() }
                ) { track ->
                    val isCurrent = currentTrack == track
                    val isFav = favoriteUris.contains(track.uri.toString())
                    val mood = moodMap[track.uri.toString()]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = { onTrackSelect(track) },
                                onLongClick = { onTrackLongClick(track) }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) accentColor.copy(alpha = 0.2f) else cardBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncAlbumArt(
                                uri = track.uri,
                                existing = track.artwork,
                                accent = accentColor,
                                sizeDp = 40
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title, 
                                    color = if (isCurrent) accentColor else textColor, 
                                    fontSize = 16.sp, 
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (isCurrent) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                    modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (mood != null) {
                                        Text(
                                            text = moodIcons[mood] ?: "🎵",
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "${track.artist} • ${track.genre}",
                                        color = subTextColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                }
                            }

                            IconButton(onClick = { onToggleFavorite(track) }) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) Color.Red else subTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(onClick = { onEditTrack(track) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Tags", tint = subTextColor, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ArtistList(artists: List<String>, themeAccent: Color, cardBg: Color, textColor: Color, onArtistClick: (String) -> Unit) {
        if (artists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No artists found.", color = Color.Gray) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(artists) { artist ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onArtistClick(artist) },
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = themeAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = artist, color = textColor, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GenreList(genres: List<String>, themeAccent: Color, cardBg: Color, textColor: Color, onGenreClick: (String) -> Unit) {
        if (genres.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No genres found.", color = Color.Gray) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(genres) { genre ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onGenreClick(genre) },
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconMusicNote, contentDescription = null, tint = themeAccent)
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
        themeAccent: Color,
        cardBg: Color,
        textColor: Color,
        onCreateNew: () -> Unit,
        onPlayPlaylist: (Playlist) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = onCreateNew,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create New Playlist", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (playlists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No playlists yet. Create one above!", color = Color.Gray) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(playlists) { playlist ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onPlayPlaylist(playlist) },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp)
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
                                Icon(IconPlayArrow, contentDescription = "Play Playlist", tint = themeAccent)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FavoritesView(
        allTracks: List<MusicTrack>,
        videosList: List<VideoItem>,
        photosList: List<PhotoItem>,
        favoriteUris: Set<String>,
        cardBg: Color,
        textColor: Color,
        subTextColor: Color,
        accentColor: Color,
        onTrackSelect: (MusicTrack) -> Unit,
        onVideoSelect: (VideoItem) -> Unit,
        onPhotoSelect: (PhotoItem) -> Unit,
        onToggleFavorite: (Uri) -> Unit
    ) {
        val favTracks = allTracks.filter { favoriteUris.contains(it.uri.toString()) }
        val favVideos = videosList.filter { favoriteUris.contains(it.contentUri.toString()) }
        val favPhotos = photosList.filter { favoriteUris.contains(it.contentUri.toString()) }

        if (favTracks.isEmpty() && favVideos.isEmpty() && favPhotos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites yet! Tap the heart icon on any song, video, or photo.", color = subTextColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (favTracks.isNotEmpty()) {
                    item {
                        Text("Favorite Songs (${favTracks.size})", color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(favTracks, key = { "fav_track_${it.uri}" }) { track ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onTrackSelect(track) },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncAlbumArt(uri = track.uri, existing = track.artwork, accent = accentColor, sizeDp = 40)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        track.title,
                                        color = textColor,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                    Text(
                                        track.artist,
                                        color = subTextColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                }
                                IconButton(onClick = { onToggleFavorite(track.uri) }) {
                                    Icon(Icons.Default.Favorite, contentDescription = "Unfavorite", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                if (favVideos.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Favorite Videos (${favVideos.size})", color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(favVideos, key = { "fav_video_${it.contentUri}" }) { video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onVideoSelect(video) },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))) {
                                    VideoThumbnail(contentUri = video.contentUri, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.title, color = textColor, fontSize = 15.sp, maxLines = 1)
                                    Text("${video.sizeBytes / (1024 * 1024)} MB", color = subTextColor, fontSize = 12.sp)
                                }
                                IconButton(onClick = { onToggleFavorite(video.contentUri) }) {
                                    Icon(Icons.Default.Favorite, contentDescription = "Unfavorite", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                if (favPhotos.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Favorite Photos (${favPhotos.size})", color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(favPhotos, key = { "fav_photo_${it.contentUri}" }) { photo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onPhotoSelect(photo) },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))) {
                                    PhotoThumb(uri = photo.contentUri, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(photo.title, color = textColor, fontSize = 15.sp, maxLines = 1)
                                    Text("Photo", color = subTextColor, fontSize = 12.sp)
                                }
                                IconButton(onClick = { onToggleFavorite(photo.contentUri) }) {
                                    Icon(Icons.Default.Favorite, contentDescription = "Unfavorite", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AboutView(themeAccent: Color, cardBg: Color, textColor: Color) {
        val context = LocalContext.current
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MediaNexpo", fontSize = 24.sp, color = themeAccent)
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
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                        shape = RoundedCornerShape(12.dp)
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
                val scannedPhotos = PhotoRepository.scanLocalPhotos(this@MainActivity)
                photosList.clear()
                photosList.addAll(scannedPhotos)
                
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

    private fun savePlaybackPosition(uri: Uri, positionMs: Long) {
        if (positionMs < 5_000L) return
        if (totalDurationMs > 0 && positionMs > totalDurationMs - 10_000L) {
            getSharedPreferences("resume_positions", MODE_PRIVATE)
                .edit().remove(uri.toString()).apply()
            return
        }
        getSharedPreferences("resume_positions", MODE_PRIVATE)
            .edit()
            .putLong(uri.toString(), positionMs)
            .apply()
        // Also persist audiobook resume by folder path when genre is Audiobook
        currentTrack?.let { track ->
            if (track.genre.equals("Audiobook", ignoreCase = true)) {
                val bookPath = audiobookChapterMap.entries.firstOrNull { entry ->
                    entry.value.any { it.uri == track.uri }
                }?.key
                if (bookPath != null) {
                    getSharedPreferences("audiobook_resume", MODE_PRIVATE)
                        .edit()
                        .putLong(bookPath, positionMs)
                        .apply()
                }
            }
        }
    }

    private fun loadPlaybackPosition(uri: Uri): Long {
        return getSharedPreferences("resume_positions", MODE_PRIVATE)
            .getLong(uri.toString(), 0L)
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
        currentTrack?.let { prev ->
            if (currentPositionMs > 0) savePlaybackPosition(prev.uri, currentPositionMs)
        }

        RecentlyPlayedStore.record(this, track.uri, track.title, track.artist)
        currentTrack = track
        GlobalScope.launch(Dispatchers.IO) {
            val lines = LyricsRepository.loadForUri(this@MainActivity, track.uri)
            withContext(Dispatchers.Main) { lyricsLines = lines }
        }
        trackBookmarks.clear()
        loopEnabled = false
        loopAMs = null
        loopBMs = null
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
fun CurrentPlayerSection(
    currentTrack: MusicTrack?,
    isPlayingState: Boolean,
    isPlayerMinimized: Boolean,
    onToggleMinimize: () -> Unit,
    currentPositionMs: Long,
    totalDurationMs: Long,
    activeCardBg: Color,
    textColor: Color,
    subTextColor: Color,
    accentColor: Color,
    selectedVisualizer: VisualizerStyle,
    visualizerEnabled: Boolean = true,
    isSpinningArtEnabled: Boolean,
    onToggleSpinningArt: (Boolean) -> Unit,
    gaplessPlaybackEnabled: Boolean,
    onToggleGapless: (Boolean) -> Unit,
    isShuffleEnabled: Boolean,
    repeatModeState: Int,
    playbackSpeed: Float,
    playbackPitch: Float,
    speedOptions: List<Float>,
    pitchOptions: List<Float>,
    showSpeedMenu: Boolean,
    onShowSpeedMenuChange: (Boolean) -> Unit,
    showPitchMenu: Boolean,
    onShowPitchMenuChange: (Boolean) -> Unit,
    sleepMinutesLeft: Int,
    loopEnabled: Boolean,
    loopAMs: Long?,
    loopBMs: Long?,
    trackBookmarks: List<Long>,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetPitch: (Float) -> Unit,
    onSetSleep: (Int) -> Unit,
    onSetLoop: (Long?, Long?, Boolean) -> Unit,
    onClearBookmarks: () -> Unit,
    onAddBookmark: (Long) -> Unit,
    onOpenLyrics: () -> Unit = {},
    hasLyrics: Boolean = false
) {
    currentTrack?.let { track ->
        var totalDragDistance by remember { mutableFloatStateOf(0f) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .pointerInput(isPlayerMinimized) {
                    detectVerticalDragGestures(
                        onDragStart = { totalDragDistance = 0f },
                        onDragEnd = {
                            if (totalDragDistance > 25f) {
                                if (!isPlayerMinimized) onToggleMinimize()
                            } else if (totalDragDistance < -25f) {
                                if (isPlayerMinimized) onToggleMinimize()
                            }
                            totalDragDistance = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragDistance += dragAmount
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = activeCardBg)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPlayerMinimized) {
                            if (sleepMinutesLeft > 0) "Now Playing (Sleep: ${sleepMinutesLeft}m) — Swipe Up" else "Now Playing (Minimized — Swipe Up)"
                        } else {
                            if (sleepMinutesLeft > 0) "Now Playing • Sleep Timer Active: ${sleepMinutesLeft}m remaining" else "Now Playing (Swipe Down to Minimize)"
                        },
                        color = if (sleepMinutesLeft > 0) Color(0xFFFFB300) else accentColor,
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleMinimize,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayerMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize",
                                tint = textColor
                            )
                        }
                    }
                }

                if (!isPlayerMinimized && visualizerEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    MainActivityInstanceHelper.MusicVisualizerViewInternal(
                        isPlaying = isPlayingState,
                        style = selectedVisualizer,
                        accentColor = accentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(8000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )

                    val artSize = if (isPlayerMinimized) 36.dp else if (visualizerEnabled) 52.dp else 80.dp

                    Box(
                        modifier = Modifier
                            .size(artSize)
                            .rotate(if (isPlayingState && isSpinningArtEnabled) rotation else 0f)
                    ) {
                        AsyncAlbumArt(
                            uri = track.uri,
                            existing = track.artwork,
                            accent = accentColor,
                            sizeDp = if (isPlayerMinimized) 36 else if (visualizerEnabled) 52 else 80
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            color = textColor,
                            fontSize = if (isPlayerMinimized) 14.sp else 16.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Text(
                            "${track.artist} • ${track.genre}",
                            color = subTextColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }

                    if (isPlayerMinimized) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                imageVector = if (isPlayingState) IconPause else IconPlayArrow,
                                contentDescription = "Play/Pause",
                                tint = accentColor
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
                                onSeek(seekTarget)
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor
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
                        IconButton(onClick = onToggleShuffle) {
                            Icon(IconShuffle, contentDescription = "Shuffle", tint = if (isShuffleEnabled) accentColor else subTextColor)
                        }
                        IconButton(onClick = onPrevious) {
                            Icon(IconSkipPrevious, contentDescription = "Previous", tint = textColor)
                        }
                        Button(
                            onClick = onPlayPause,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = if (isPlayingState) IconPause else IconPlayArrow, contentDescription = "Play/Pause", tint = Color.Black)
                        }
                        IconButton(onClick = onNext) {
                            Icon(IconSkipNext, contentDescription = "Next", tint = textColor)
                        }
                        IconButton(onClick = onCycleRepeat) {
                            Icon(IconRepeat, contentDescription = "Repeat", tint = if (repeatModeState != Player.REPEAT_MODE_OFF) accentColor else subTextColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    var showAdvancedTools by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { showAdvancedTools = !showAdvancedTools },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildString {
                                    append("Advanced Tools & Audio Enhancements")
                                    if (playbackSpeed != 1.0f) append(" • ${playbackSpeed}x")
                                    if (gaplessPlaybackEnabled) append(" • Gapless ON")
                                    if (loopEnabled) append(" • A-B Loop ON")
                                    if (sleepMinutesLeft > 0) append(" • Sleep (${sleepMinutesLeft}m)")
                                },
                                color = accentColor,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = if (showAdvancedTools) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showAdvancedTools) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            colors = CardDefaults.cardColors(containerColor = activeCardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text("A-B Segment Loop", color = accentColor, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val a = loopAMs
                                            if (a == null || (loopBMs != null && a >= loopBMs)) {
                                                onSetLoop(currentPositionMs, loopBMs, false)
                                            } else {
                                                onSetLoop(currentPositionMs, loopBMs, true)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text(if (loopAMs != null) "A: ${formatTime(loopAMs!!)}" else "Set A", color = textColor, fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val b = currentPositionMs
                                            val a = loopAMs
                                            if (a != null && b > a) {
                                                onSetLoop(a, b, true)
                                            } else {
                                                onSetLoop(loopAMs, b, false)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text(if (loopBMs != null) "B: ${formatTime(loopBMs!!)}" else "Set B", color = textColor, fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { onSetLoop(null, null, false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (loopEnabled) Color(0xFFB00020) else Color.DarkGray),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text(if (loopEnabled) "Clear Loop" else "Off", color = Color.White, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = subTextColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // Sound Moments — unique feature: bookmark a feeling in the song
                                val contextMoments = LocalContext.current
                                var momentNote by remember { mutableStateOf("") }
                                var showMomentDialog by remember { mutableStateOf(false) }
                                var momentsTick by remember { mutableIntStateOf(0) }
                                val trackMoments = remember(track.uri, momentsTick) {
                                    SoundMomentsStore.load(contextMoments)
                                        .filter { it.uri == track.uri.toString() }
                                }

                                Text("Sound Moments", color = accentColor, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text("Save this exact second with a note — jump back anytime", color = subTextColor, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { showMomentDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Capture moment @ ${formatTime(currentPositionMs)}", color = accentColor, fontSize = 12.sp)
                                }
                                if (trackMoments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    trackMoments.take(5).forEach { moment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onSeek(moment.positionMs)
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(moment.note, color = textColor, fontSize = 12.sp, maxLines = 1)
                                                Text(formatTime(moment.positionMs), color = subTextColor, fontSize = 10.sp)
                                            }
                                            TextButton(onClick = {
                                                SoundMomentsStore.remove(contextMoments, moment.id)
                                                momentsTick++
                                            }) {
                                                Text("✕", color = Color.Gray, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                                if (showMomentDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showMomentDialog = false },
                                        title = { Text("Capture Sound Moment", color = textColor) },
                                        text = {
                                            Column {
                                                Text("At ${formatTime(currentPositionMs)} in ${track.title}", color = subTextColor, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedTextField(
                                                    value = momentNote,
                                                    onValueChange = { momentNote = it },
                                                    placeholder = { Text("e.g. favorite verse, the drop…", color = subTextColor) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                SoundMomentsStore.add(
                                                    contextMoments,
                                                    track.uri,
                                                    track.title,
                                                    track.artist,
                                                    currentPositionMs,
                                                    momentNote
                                                )
                                                momentNote = ""
                                                showMomentDialog = false
                                                momentsTick++
                                            }) {
                                                Text("Save", color = accentColor)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showMomentDialog = false }) {
                                                Text("Cancel", color = subTextColor)
                                            }
                                        },
                                        containerColor = activeCardBg
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = subTextColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Spinning Album Art", color = textColor, fontSize = 13.sp)
                                    Switch(
                                        checked = isSpinningArtEnabled,
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor),
                                        onCheckedChange = { onToggleSpinningArt(it) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = subTextColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedButton(
                                    onClick = onOpenLyrics,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        if (hasLyrics) "Show Synced Lyrics" else "Lyrics (drop a .lrc next to the song)",
                                        color = accentColor,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = subTextColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(6.dp))

                                Text("DJ Turntable Scratch Pad", color = accentColor, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                var accumulatedDragPx by remember { mutableFloatStateOf(0f) }
                                var dragStartPositionMs by remember { mutableLongStateOf(0L) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .pointerInput(currentTrack) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { 
                                                    accumulatedDragPx = 0f
                                                    dragStartPositionMs = currentPositionMs
                                                },
                                                onDragEnd = { accumulatedDragPx = 0f },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    accumulatedDragPx += dragAmount
                                                    val targetPos = (dragStartPositionMs + (accumulatedDragPx * 50f)).toLong()
                                                    if (totalDurationMs > 0) {
                                                        val clamped = targetPos.coerceIn(0L, totalDurationMs)
                                                        onSeek(clamped)
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Drag horizontally to scratch / scrub audio", color = textColor, fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = subTextColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // Local menu state so constant player recomposition doesn't kill the popup
                                var localSpeedMenu by remember { mutableStateOf(false) }
                                var localPitchMenu by remember { mutableStateOf(false) }
                                fun formatRate(v: Float): String =
                                    if (v == v.toLong().toFloat()) "${v.toLong()}x" else "${v}x"

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Speed", color = subTextColor, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box {
                                            OutlinedButton(
                                                onClick = {
                                                    localPitchMenu = false
                                                    localSpeedMenu = !localSpeedMenu
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(formatRate(playbackSpeed), color = accentColor, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            }
                                            DropdownMenu(
                                                expanded = localSpeedMenu,
                                                onDismissRequest = { localSpeedMenu = false },
                                                modifier = Modifier.heightIn(max = 280.dp)
                                            ) {
                                                speedOptions.forEach { speed ->
                                                    val selected = kotlin.math.abs(playbackSpeed - speed) < 0.001f
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                formatRate(speed),
                                                                color = if (selected) accentColor else textColor,
                                                                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                                            )
                                                        },
                                                        onClick = {
                                                            onSetSpeed(speed)
                                                            localSpeedMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Pitch", color = subTextColor, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box {
                                            OutlinedButton(
                                                onClick = {
                                                    localSpeedMenu = false
                                                    localPitchMenu = !localPitchMenu
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(formatRate(playbackPitch), color = accentColor, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            }
                                            DropdownMenu(
                                                expanded = localPitchMenu,
                                                onDismissRequest = { localPitchMenu = false },
                                                modifier = Modifier.heightIn(max = 280.dp)
                                            ) {
                                                pitchOptions.forEach { pitch ->
                                                    val selected = kotlin.math.abs(playbackPitch - pitch) < 0.001f
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                formatRate(pitch),
                                                                color = if (selected) accentColor else textColor,
                                                                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                                            )
                                                        },
                                                        onClick = {
                                                            onSetPitch(pitch)
                                                            localPitchMenu = false
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
            }
        }
    }
}

object MainActivityInstanceHelper {

    @Composable
    fun EdgeLightingView(accentColor: Color) {
        var pulse by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos {
                    val latest = PlaybackService.latestFftData
                    val bass = if (latest.isNotEmpty()) latest[0] else 0f
                    val target = maxOf(PlaybackService.beatPulse, bass)
                    pulse += (target - pulse) * 0.45f
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 8.dp.toPx() + (pulse * 24.dp.toPx())
            val alpha = (0.2f + pulse * 0.8f).coerceIn(0f, 1f)
            drawRoundRect(
                color = accentColor.copy(alpha = alpha),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                style = Stroke(width = strokeW)
            )
        }
    }

    @Composable
    fun MusicVisualizerViewInternal(isPlaying: Boolean, style: VisualizerStyle, accentColor: Color) {
        val display = remember { FloatArray(32) { 0.05f } }
        var frameTick by remember { mutableIntStateOf(0) }
        var timeSec by remember { mutableFloatStateOf(0f) }
        var beat by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(isPlaying) {
            if (!isPlaying) {
                for (i in display.indices) display[i] = 0.05f
                beat = 0f
                frameTick++
                return@LaunchedEffect
            }
            while (true) {
                withFrameNanos { nanos ->
                    timeSec = (nanos / 1_000_000_000.0).toFloat()
                    val latest = PlaybackService.latestFftData
                    val n = minOf(display.size, latest.size)
                    for (i in 0 until n) {
                        val target = latest[i]
                        // Rise with the hit, fall fast enough to leave gaps between beats
                        val k = if (target > display[i]) 0.75f else 0.32f
                        display[i] += (target - display[i]) * k
                    }
                    val pulse = PlaybackService.beatPulse
                    // Follow onsets tightly so every hit shows up
                    val targetBeat = pulse.coerceIn(0f, 1f)
                    val beatK = if (targetBeat > beat) 0.55f else 0.35f
                    beat += (targetBeat - beat) * beatK
                    frameTick++
                }
            }
        }

        val v = frameTick
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            @Suppress("UNUSED_EXPRESSION")
            v
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val kick = 1f + beat * 0.55f

            when (style) {
                VisualizerStyle.BARS -> {
                    val n = 32
                    val gap = 2f
                    val bw = ((w - gap * (n - 1)) / n).coerceAtLeast(1.5f)
                    for (i in 0 until n) {
                        val amp = display[i % display.size]
                        // Spectrum height + shared beat pump on every bar
                        val boost = (amp * 0.85f + beat * 0.35f + amp * beat * 0.25f).coerceIn(0.04f, 1.25f)
                        val bh = (h * boost).coerceAtLeast(2f)
                        val alpha = (0.5f + amp * 0.35f + beat * 0.2f).coerceIn(0f, 1f)
                        drawRect(
                            accentColor.copy(alpha = alpha),
                            Offset(i * (bw + gap), h - bh),
                            androidx.compose.ui.geometry.Size(bw, bh)
                        )
                    }
                }
                VisualizerStyle.MIRROR -> {
                    val n = 28
                    val gap = 2f
                    val bw = ((w - gap * (n - 1)) / n).coerceAtLeast(1.5f)
                    for (i in 0 until n) {
                        val amp = display[i % display.size] * kick
                        val bh = (cy * amp).coerceAtLeast(2f)
                        val x = i * (bw + gap)
                        drawRect(accentColor, Offset(x, cy - bh), androidx.compose.ui.geometry.Size(bw, bh))
                        drawRect(accentColor.copy(alpha = 0.45f), Offset(x, cy), androidx.compose.ui.geometry.Size(bw, bh))
                    }
                }
                VisualizerStyle.WAVE -> {
                    val path = Path()
                    val n = display.size
                    for (i in 0 until n) {
                        val x = i / (n - 1f) * w
                        val y = cy + (display[i] - 0.35f) * h * 0.9f * kick
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, accentColor.copy(alpha = 0.35f), style = Stroke(12f))
                    drawPath(path, accentColor, style = Stroke(2.5f))
                }
                VisualizerStyle.RIBBON -> {
                    val path = Path()
                    val n = display.size
                    for (i in 0 until n) {
                        val x = i / (n - 1f) * w
                        val y = cy + (display[i] - 0.4f) * h * kick
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, accentColor.copy(alpha = (0.2f + beat * 0.3f).coerceIn(0f, 1f)), style = Stroke(18f))
                    drawPath(path, accentColor, style = Stroke(4f))
                }
                VisualizerStyle.PULSE -> {
                    val r1 = h * 0.12f + beat * h * 0.38f
                    val r2 = h * 0.08f + display[1] * h * 0.22f
                    drawCircle(accentColor.copy(alpha = (0.15f + beat * 0.35f).coerceIn(0f, 1f)), r1, Offset(cx, cy))
                    drawCircle(accentColor.copy(alpha = 0.7f), r2, Offset(cx, cy))
                    for (i in 0 until 12) {
                        val a = i / 12f * Math.PI * 2 + timeSec
                        val d = r1 + display[i % display.size] * 12f
                        drawCircle(accentColor, 2.5f + beat * 3f, Offset(cx + cos(a).toFloat() * d, cy + sin(a).toFloat() * d))
                    }
                }
                VisualizerStyle.BEAT_RING -> {
                    val rings = 5
                    for (r in 0 until rings) {
                        val amp = display[(r * 3) % display.size]
                        val radius = h * 0.1f + r * h * 0.08f + amp * h * 0.15f * kick
                        drawCircle(
                            accentColor.copy(alpha = (0.15f + amp * 0.5f).coerceIn(0f, 1f)),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2f + beat * 4f)
                        )
                    }
                    drawCircle(accentColor.copy(alpha = 0.8f), 6f + beat * 14f, Offset(cx, cy))
                }
                VisualizerStyle.EQ_MOUNTAIN -> {
                    val path = Path()
                    val n = display.size
                    path.moveTo(0f, h)
                    for (i in 0 until n) {
                        val x = i / (n - 1f) * w
                        val y = h - display[i] * h * kick
                        path.lineTo(x, y)
                    }
                    path.lineTo(w, h)
                    path.close()
                    drawPath(path, accentColor.copy(alpha = (0.35f + beat * 0.25f).coerceIn(0f, 1f)))
                    val line = Path()
                    for (i in 0 until n) {
                        val x = i / (n - 1f) * w
                        val y = h - display[i] * h * kick
                        if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                    }
                    drawPath(line, accentColor, style = Stroke(2f))
                }
                VisualizerStyle.SYNTHWAVE -> {
                    val horizon = h * 0.38f
                    val bass = display[0]
                    for (r in 0 until 10) {
                        val t = r / 9f
                        val y = horizon + (h - horizon) * t * t * (1f + bass * 0.6f + beat * 0.3f)
                        drawLine(accentColor.copy(alpha = (0.2f + t * 0.7f).coerceIn(0f, 1f)), Offset(0f, y), Offset(w, y), 1.5f + beat * 2f)
                    }
                    for (c in -7..7) {
                        drawLine(
                            accentColor.copy(alpha = (0.35f + beat * 0.4f).coerceIn(0f, 1f)),
                            Offset(cx, horizon),
                            Offset(cx + c * (w / 14f) * 1.4f, h),
                            1.5f
                        )
                    }
                    drawCircle(accentColor.copy(alpha = 0.9f), h * 0.09f * (1f + beat * 0.4f), Offset(cx, horizon - 4f))
                }
                VisualizerStyle.STARBURST -> {
                    val rays = 24
                    for (i in 0 until rays) {
                        val amp = display[i % display.size]
                        val a = i / rays.toFloat() * Math.PI * 2 + timeSec * 0.4
                        val len = h * 0.15f + amp * h * 0.45f * kick
                        drawLine(
                            accentColor.copy(alpha = (0.4f + amp * 0.6f).coerceIn(0f, 1f)),
                            Offset(cx, cy),
                            Offset(cx + cos(a).toFloat() * len, cy + sin(a).toFloat() * len),
                            strokeWidth = 2f + beat * 3f
                        )
                    }
                    drawCircle(accentColor, 5f + beat * 12f, Offset(cx, cy))
                }
                VisualizerStyle.TUNNEL -> {
                    drawRect(Color.Black)
                    for (i in 0 until 36) {
                        val seed = i * 41.3f
                        val z = ((seed * 0.01f + timeSec * (1.5f + beat * 4f)) % 1f)
                        val ang = seed
                        val rad = z * minOf(w, h) * 0.55f
                        drawCircle(
                            accentColor.copy(alpha = (0.2f + z * 0.8f).coerceIn(0f, 1f)),
                            1.5f + z * 4f * (0.5f + beat),
                            Offset(cx + cos(ang.toDouble()).toFloat() * rad, cy + sin(ang.toDouble()).toFloat() * rad)
                        )
                    }
                    drawCircle(accentColor.copy(alpha = (beat * 0.35f).coerceIn(0f, 1f)), 20f + beat * 40f, Offset(cx, cy), style = Stroke(2f))
                }
                VisualizerStyle.LIQUID -> {
                    val path = Path()
                    val n = 24
                    path.moveTo(0f, h)
                    for (i in 0 until n) {
                        val x = i / (n - 1f) * w
                        val wave = sin((i * 0.55f + timeSec * 3f).toDouble()).toFloat() * 0.08f
                        val y = h - (display[i % display.size] + wave) * h * 0.95f * (0.7f + beat * 0.5f)
                        path.lineTo(x, y)
                    }
                    path.lineTo(w, h)
                    path.close()
                    drawPath(path, accentColor.copy(alpha = 0.45f))
                }
                VisualizerStyle.VU_METERS -> {
                    val meters = 8
                    val gap = 6f
                    val mw = ((w - gap * (meters - 1)) / meters)
                    for (m in 0 until meters) {
                        val amp = display[(m * 3) % display.size] * kick
                        val segs = 10
                        val segH = (h - 4f) / segs
                        val lit = (amp * segs).toInt().coerceIn(1, segs)
                        for (s in 0 until lit) {
                            val t = s / segs.toFloat()
                            val col = when {
                                t > 0.8f -> Color(0xFFFF5252)
                                t > 0.55f -> Color(0xFFFFEE58)
                                else -> accentColor
                            }
                            drawRect(
                                col.copy(alpha = 0.85f),
                                Offset(m * (mw + gap), h - (s + 1) * segH),
                                androidx.compose.ui.geometry.Size(mw, segH - 1.5f)
                            )
                        }
                    }
                }
                VisualizerStyle.KALEIDO -> {
                    val arms = 8
                    for (arm in 0 until arms) {
                        val base = arm / arms.toFloat() * Math.PI * 2 + timeSec * 0.5
                        val path = Path()
                        for (i in 0 until 12) {
                            val amp = display[i % display.size]
                            val a = base + i * 0.08
                            val d = 8f + i * (h * 0.04f) + amp * h * 0.2f * kick
                            val x = cx + cos(a).toFloat() * d
                            val y = cy + sin(a).toFloat() * d
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, accentColor.copy(alpha = (0.55f + beat * 0.35f).coerceIn(0f, 1f)), style = Stroke(2.5f))
                    }
                }
                VisualizerStyle.SPARKLINE -> {
                    val cols = 16
                    val rows = 4
                    val cw = w / cols
                    val rh = h / rows
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val amp = display[(c + r * 3) % display.size] * (0.7f + beat * 0.5f)
                            if (amp < 0.12f) continue
                            drawCircle(
                                accentColor.copy(alpha = (0.3f + amp * 0.7f).coerceIn(0f, 1f)),
                                radius = ((minOf(cw, rh) * 0.15f) + amp * minOf(cw, rh) * 0.3f).coerceIn(0f, minOf(cw, rh)),
                                center = Offset(c * cw + cw / 2f, r * rh + rh / 2f)
                            )
                        }
                    }
                }
                VisualizerStyle.NEON_VORTEX -> {
                    val points = 60
                    val path = Path()
                    for (i in 0 until points) {
                        val t = i / points.toFloat()
                        val amp = display[(i % 16)]
                        val angle = t * Math.PI * 8 + (timeSec * (2f + beat * 2f)) 
                        val radius = t * (minOf(w, h) / 2f) * (1f + amp * 0.6f * kick)
                        val x = cx + cos(angle).toFloat() * radius
                        val y = cy + sin(angle).toFloat() * radius
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, accentColor.copy(alpha = (0.2f + beat * 0.4f).coerceIn(0f, 1f)), style = Stroke(6f + beat * 10f))
                    drawPath(path, accentColor, style = Stroke(2f))
                }
                VisualizerStyle.CYBER_PARTICLES -> {
                    for (i in 0 until 32) {
                        val amp = display[i]
                        val angle = (i * 137.5) + (timeSec * 50f)
                        val distance = (amp * (w / 2f) * kick) + (beat * 10f)
                        val px = cx + cos(angle).toFloat() * distance
                        val py = cy + sin(angle).toFloat() * distance
                        drawCircle(accentColor.copy(alpha = (0.4f + amp).coerceIn(0f, 1f)), radius = 2f + (amp * 12f * kick), center = Offset(px, py))
                    }
                }
                VisualizerStyle.FREQ_POLYGON -> {
                    val path = Path()
                    val n = 24
                    for (i in 0 until n) {
                        val amp = display[i]
                        val angle = i / n.toFloat() * Math.PI * 2 + (timeSec * 0.2f)
                        val r = h * 0.2f + (amp * h * 0.4f * kick)
                        val px = cx + cos(angle).toFloat() * r
                        val py = cy + sin(angle).toFloat() * r
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, accentColor.copy(alpha = (0.15f + beat * 0.35f).coerceIn(0f, 1f)))
                    drawPath(path, accentColor, style = Stroke(3f + beat * 4f))
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun FullscreenImageView(
    uri: Uri,
    title: String,
    accentColor: Color,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {}
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Red else Color.White
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = accentColor)
            }
        }
    }
}

@Composable
fun AudioEffectsSettingsSection(
    textColor: Color = Color.Black,
    subTextColor: Color = Color(0xFF444444),
    cardBg: Color = Color.White.copy(alpha = 0.3f),
    accentColor: Color = Color(0xFFBB86FC)
) {
    var eqEnabled by remember { mutableStateOf(PlaybackService.eqEnabled) }
    var bassLevel by remember { mutableFloatStateOf(PlaybackService.bassStrength.toFloat() / 1000f) }
    var virtLevel by remember { mutableFloatStateOf(PlaybackService.virtualizerStrength.toFloat() / 1000f) }
    var gainLevel by remember { mutableFloatStateOf(PlaybackService.gainMb.toFloat() / 1000f) }

    val bandFreqs = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("EQ Presets", color = textColor, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(EqPresets.all.size) { i ->
                            val preset = EqPresets.all[i]
                            OutlinedButton(
                                onClick = {
                                    EqPresets.apply(preset)
                                    // Sync local slider state
                                    for (b in preset.bands.indices) {
                                        if (b < eqBands.size) {
                                            eqBands[b] = preset.bands[b] / 100f
                                        }
                                    }
                                    bassLevel = preset.bass / 1000f
                                    virtLevel = preset.virtualizer / 1000f
                                    gainLevel = preset.gainMb / 1000f
                                },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(preset.name, color = accentColor, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp)
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
                shape = RoundedCornerShape(12.dp)
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
                                value = level.coerceIn(-15f, 15f),
                                valueRange = -15f..15f,
                                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.3f)),
                                onValueChange = { newLvl ->
                                    eqBands[index] = newLvl
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoList(
    videoList: List<VideoItem>,
    favoriteUris: Set<String>,
    onVideoSelect: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit = {},
    onToggleFavorite: (VideoItem) -> Unit,
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
                    val isFav = favoriteUris.contains(video.contentUri.toString())
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .combinedClickable(
                                onClick = { onVideoSelect(video) },
                                onLongClick = { onVideoLongClick(video) }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            VideoThumbnail(
                                contentUri = video.contentUri,
                                modifier = Modifier.fillMaxSize()
                            )
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
                            IconButton(
                                onClick = { onToggleFavorite(video) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) Color.Red else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoList(
    photoList: List<PhotoItem>,
    favoriteUris: Set<String>,
    onPhotoSelect: (PhotoItem) -> Unit,
    onPhotoLongClick: (PhotoItem) -> Unit = {},
    onToggleFavorite: (PhotoItem) -> Unit,
    searchQuery: String = "",
    accentColor: Color = Color.Cyan
) {
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val filtered = photoList.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
            it.path.contains(searchQuery, ignoreCase = true)
    }
    val folders = filtered.groupBy {
        java.io.File(it.path).parentFile?.name ?: "Internal Storage"
    }

    if (selectedFolder == null) {
        if (folders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No photos found", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                folders.forEach { (folderName, photosInFolder) ->
                    val cover = photosInFolder.firstOrNull()?.contentUri
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clickable { selectedFolder = folderName },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                if (cover != null) {
                                    PhotoThumb(uri = cover, modifier = Modifier.fillMaxSize())
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        folderName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        "${photosInFolder.size} photos",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        val folderPhotos = folders[selectedFolder] ?: emptyList()
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { selectedFolder = null }) {
                    Text("Back", color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    selectedFolder!!,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(folderPhotos, key = { it.contentUri.toString() }) { photo ->
                    val isFav = favoriteUris.contains(photo.contentUri.toString())
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .combinedClickable(
                                onClick = { onPhotoSelect(photo) },
                                onLongClick = { onPhotoLongClick(photo) }
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            PhotoThumb(uri = photo.contentUri, modifier = Modifier.fillMaxSize())
                            IconButton(
                                onClick = { onToggleFavorite(photo) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) Color.Red else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoThumb(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val key = uri.toString()
    var bitmap by remember(key) { mutableStateOf(albumArtCache.get("photo_$key")) }

    LaunchedEffect(key) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(stream, null, bounds)
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        var sample = 1
                        val b2 = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(stream, null, b2)
                        while ((b2.outWidth / sample) > 320 || (b2.outHeight / sample) > 320) sample *= 2
                        null
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = 4
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (loaded != null) {
                albumArtCache.put("photo_$key", loaded)
                bitmap = loaded
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(modifier.background(Color.DarkGray), contentAlignment = Alignment.Center) {
            Text("…", color = Color.White)
        }
    }
}

@Composable
fun RecentView(
    entries: List<RecentlyPlayedStore.Entry>,
    cardBg: Color,
    textColor: Color,
    subTextColor: Color,
    accentColor: Color,
    onPlay: (RecentlyPlayedStore.Entry) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recently Played", color = textColor, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            if (entries.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Clear", color = accentColor)
                }
            }
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Play some songs — history shows up here.", color = subTextColor)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(entries, key = { it.uri + it.playedAt }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onPlay(entry) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.title,
                                    color = textColor,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                                )
                                Text(entry.artist, color = subTextColor, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodsView(
    moodKeys: List<String>,
    moodMap: Map<String, String>,
    moodIcons: Map<String, String>,
    allTracks: List<MusicTrack>,
    themeAccent: Color,
    cardBg: Color,
    textColor: Color,
    onMoodClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Auto-detected moods while you listen",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(moodKeys) { mood ->
            val count = allTracks.count { moodMap[it.uri.toString()] == mood }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMoodClick(mood) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(moodIcons[mood] ?: "🎵", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mood, color = textColor, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("$count songs", color = themeAccent, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = themeAccent)
                }
            }
        }
    }
}

@Composable
fun BooksView(
    books: List<BookItem>,
    themeAccent: Color,
    cardBg: Color,
    textColor: Color,
    onEpubClick: (BookItem) -> Unit,
    onAudiobookClick: (BookItem) -> Unit,
    onAddBook: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Books & Audiobooks", color = textColor, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            TextButton(onClick = onAddBook) {
                Text("+ Add", color = themeAccent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No books found.\nAdd an EPUB or audiobook folder.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(books, key = { it.contentUri.toString() }) { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (book.isAudiobook) onAudiobookClick(book) else onEpubClick(book)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (book.isAudiobook) "🎧" else "📖", fontSize = 26.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, color = textColor, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 2)
                                Text(
                                    if (book.isAudiobook) "Audiobook" else "EPUB",
                                    color = themeAccent,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = themeAccent)
                        }
                    }
                }
            }
        }
    }
}
