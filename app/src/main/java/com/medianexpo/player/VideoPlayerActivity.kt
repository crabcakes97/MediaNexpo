package com.medianexpo.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
<<<<<<< HEAD
=======
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
>>>>>>> 2c4ab1d (Initial commit after project recovery)
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
<<<<<<< HEAD
=======
import androidx.compose.material3.TextButton
>>>>>>> 2c4ab1d (Initial commit after project recovery)
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
<<<<<<< HEAD
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
=======
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
>>>>>>> 2c4ab1d (Initial commit after project recovery)
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
<<<<<<< HEAD
=======
import androidx.media3.common.PlaybackParameters
>>>>>>> 2c4ab1d (Initial commit after project recovery)
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

<<<<<<< HEAD
=======
// ⏪ Replay 10 — curved arrow + 10
private val IconReplay10: ImageVector
    get() = ImageVector.Builder(
        name = "Replay10",
        defaultWidth = 36.dp,
        defaultHeight = 36.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            // left-pointing triangle (skip back)
            moveTo(11.5f, 12f)
            lineTo(18f, 6f)
            lineTo(18f, 18f)
            close()
            // bar
            moveTo(6f, 6f)
            lineTo(8f, 6f)
            lineTo(8f, 18f)
            lineTo(6f, 18f)
            close()
        }
    }.build()

// ⏩ Forward 10 — skip ahead
private val IconForward10: ImageVector
    get() = ImageVector.Builder(
        name = "Forward10",
        defaultWidth = 36.dp,
        defaultHeight = 36.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            // right-pointing triangle
            moveTo(6f, 6f)
            lineTo(14.5f, 12f)
            lineTo(6f, 18f)
            close()
            // bar
            moveTo(16f, 6f)
            lineTo(18f, 6f)
            lineTo(18f, 18f)
            lineTo(16f, 18f)
            close()
        }
    }.build()

private val IconPauseBars: ImageVector
    get() = ImageVector.Builder(
        name = "PauseBars",
        defaultWidth = 36.dp,
        defaultHeight = 36.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 5f)
            lineTo(10f, 5f)
            lineTo(10f, 19f)
            lineTo(6f, 19f)
            close()
            moveTo(14f, 5f)
            lineTo(18f, 5f)
            lineTo(18f, 19f)
            lineTo(14f, 19f)
            close()
        }
    }.build()

>>>>>>> 2c4ab1d (Initial commit after project recovery)
class VideoPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val videoUriString = intent.getStringExtra("EXTRA_VIDEO_URI")
        val videoUri = videoUriString?.let { Uri.parse(it) }
<<<<<<< HEAD
=======
        val videoTitle = intent.getStringExtra("EXTRA_VIDEO_TITLE") ?: "Video"
>>>>>>> 2c4ab1d (Initial commit after project recovery)

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (videoUri != null) {
                    GestureVideoPlayer(
                        videoUri = videoUri,
<<<<<<< HEAD
=======
                        videoTitle = videoTitle,
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun GestureVideoPlayer(
    videoUri: Uri,
<<<<<<< HEAD
=======
    videoTitle: String = "Video",
>>>>>>> 2c4ab1d (Initial commit after project recovery)
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
<<<<<<< HEAD
=======
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
>>>>>>> 2c4ab1d (Initial commit after project recovery)

    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var volumeFeedback by remember { mutableStateOf<Int?>(null) }
    var brightnessFeedback by remember { mutableStateOf<Int?>(null) }
    var scrubPreview by remember { mutableStateOf<String?>(null) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isSeeking) {
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
<<<<<<< HEAD
            delay(200)
=======
            delay(if (showControls) 250 else 500)
>>>>>>> 2c4ab1d (Initial commit after project recovery)
        }
    }

    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(700)
            seekFeedback = null
        }
    }
    LaunchedEffect(volumeFeedback) {
        if (volumeFeedback != null) {
            delay(900)
            volumeFeedback = null
        }
    }
    LaunchedEffect(brightnessFeedback) {
        if (brightnessFeedback != null) {
            delay(900)
            brightnessFeedback = null
        }
    }

    fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%d:%02d", m, s)
    }

    fun seekBy(deltaMs: Long) {
        val dur = exoPlayer.duration.coerceAtLeast(0L)
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, dur)
        exoPlayer.seekTo(target)
        positionMs = target
        showControls = true
        seekFeedback = if (deltaMs < 0) "${deltaMs / 1000}s" else "+${deltaMs / 1000}s"
    }

<<<<<<< HEAD
=======
    fun cycleSpeed() {
        val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
        val idx = speeds.indexOfFirst { abs(it - playbackSpeed) < 0.01f }
        val next = speeds[(idx + 1).coerceAtLeast(0) % speeds.size]
        playbackSpeed = next
        exoPlayer.playbackParameters = PlaybackParameters(next)
        showControls = true
    }

>>>>>>> 2c4ab1d (Initial commit after project recovery)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val width = size.width.toFloat()
                val height = size.height.toFloat()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    var totalDx = 0f
                    var totalDy = 0f
                    var dragged = false
                    var lastVolStep = 0f
                    var lastBrightStep = 0f

                    drag(down.id) { change ->
                        val dx = change.positionChange().x
                        val dy = change.positionChange().y
                        totalDx += dx
                        totalDy += dy
                        change.consume()

                        if (!dragged && (abs(totalDx) > 12f || abs(totalDy) > 12f)) {
                            dragged = true
                        }

                        if (dragged) {
                            if (abs(totalDy) > abs(totalDx)) {
                                if (startX < width / 2f) {
                                    lastBrightStep += -dy
                                    if (abs(lastBrightStep) > height * 0.02f) {
                                        val lp = activity.window.attributes
                                        val current = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                        val delta = if (lastBrightStep > 0) 0.04f else -0.04f
                                        val newBright = (current + delta).coerceIn(0.05f, 1f)
                                        lp.screenBrightness = newBright
                                        activity.window.attributes = lp
                                        brightnessFeedback = (newBright * 100).toInt()
                                        volumeFeedback = null
                                        lastBrightStep = 0f
                                    }
                                } else {
                                    lastVolStep += -dy
                                    if (abs(lastVolStep) > height * 0.03f) {
                                        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val newVol = (current + if (lastVolStep > 0) 1 else -1)
                                            .coerceIn(0, maxVolume)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        volumeFeedback = ((newVol.toFloat() / maxVolume) * 100).toInt()
                                        brightnessFeedback = null
                                        lastVolStep = 0f
                                    }
                                }
                                scrubPreview = null
                            } else {
                                val duration = exoPlayer.duration.coerceAtLeast(1L)
                                val seekDelta = (totalDx / width * duration * 0.6f).toLong()
                                val target = (exoPlayer.currentPosition + seekDelta)
                                    .coerceIn(0L, duration)
                                scrubPreview = formatTime(target)
                                volumeFeedback = null
                                brightnessFeedback = null
                            }
                        }
                    }

                    if (!dragged) {
                        val secondDown = withTimeoutOrNull(250L) {
                            awaitFirstDown(requireUnconsumed = false)
                        }
                        if (secondDown != null) {
                            secondDown.consume()
                            if (secondDown.position.x < width / 2f) {
                                seekBy(-10_000L)
                            } else {
                                seekBy(10_000L)
                            }
                        } else {
                            showControls = !showControls
                        }
                    } else if (scrubPreview != null && abs(totalDx) > abs(totalDy)) {
                        val duration = exoPlayer.duration.coerceAtLeast(1L)
                        val seekDelta = (totalDx / width * duration * 0.6f).toLong()
                        val target = (exoPlayer.currentPosition + seekDelta)
                            .coerceIn(0L, duration)
                        exoPlayer.seekTo(target)
                        positionMs = target
                        scrubPreview = null
                    } else {
                        scrubPreview = null
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
<<<<<<< HEAD
=======
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
<<<<<<< HEAD
                // Top bar
=======
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(top = 12.dp, start = 4.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
<<<<<<< HEAD
                        text = "Video",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Center transport — core icons only (no material-icons-extended)
=======
                        text = videoTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp, end = 8.dp)
                    )
                }

>>>>>>> 2c4ab1d (Initial commit after project recovery)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    IconButton(
                        onClick = { seekBy(-10_000L) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
<<<<<<< HEAD
                        Text("-10s", color = Color.White, fontSize = 14.sp)
=======
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = IconReplay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text("10", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                        }
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            isPlaying = exoPlayer.isPlaying
                            showControls = true
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
<<<<<<< HEAD
                        if (isPlaying) {
                            // Pause: two bars via text (no Icons.Default.Pause in core set)
                            Text("II", color = Color.White, fontSize = 28.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
=======
                        Icon(
                            imageVector = if (isPlaying) IconPauseBars else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                    }

                    IconButton(
                        onClick = { seekBy(10_000L) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
<<<<<<< HEAD
                        Text("+10s", color = Color.White, fontSize = 14.sp)
                    }
                }

                // Bottom seek bar
=======
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = IconForward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text("10", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                        }
                    }
                }

>>>>>>> 2c4ab1d (Initial commit after project recovery)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    val progress = if (durationMs > 0) {
                        (if (isSeeking) seekValue else positionMs.toFloat() / durationMs)
                            .coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progress,
                        onValueChange = { v ->
                            isSeeking = true
                            seekValue = v
                            showControls = true
                        },
                        onValueChangeFinished = {
                            if (durationMs > 0) {
                                val target = (seekValue * durationMs).toLong()
                                exoPlayer.seekTo(target)
                                positionMs = target
                            }
                            isSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
<<<<<<< HEAD
                            activeTrackColor = Color(0xFFBB86FC),
=======
                            activeTrackColor = Color(0xFFE0B0FF),
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayPos = if (isSeeking && durationMs > 0) {
                            (seekValue * durationMs).toLong()
                        } else positionMs
                        Text(formatTime(displayPos), color = Color.White, fontSize = 12.sp)
                        Text(formatTime(durationMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }

<<<<<<< HEAD
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap for controls  ·  Double-tap ±10s  ·  Swipe volume / brightness",
=======
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { cycleSpeed() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0B0FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Speed  ${playbackSpeed}x",
                            color = Color.Black,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap · Double-tap ±10s · Swipe volume / brightness",
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = seekFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                Text(text = seekFeedback ?: "", color = Color.White, fontSize = 22.sp)
            }
        }

        AnimatedVisibility(
            visible = volumeFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text("Volume  ${volumeFeedback ?: 0}%", color = Color.White, fontSize = 16.sp)
            }
        }

        AnimatedVisibility(
            visible = brightnessFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text("Brightness  ${brightnessFeedback ?: 0}%", color = Color.White, fontSize = 16.sp)
            }
        }

        AnimatedVisibility(
            visible = scrubPreview != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                Text(text = scrubPreview ?: "", color = Color.White, fontSize = 22.sp)
            }
        }
    }
}
