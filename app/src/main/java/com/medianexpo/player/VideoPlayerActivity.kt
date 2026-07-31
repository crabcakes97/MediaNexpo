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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

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

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (videoUri != null) {
                    GestureVideoPlayer(
                        videoUri = videoUri,
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
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var volumeFeedback by remember { mutableStateOf<Int?>(null) }
    var brightnessFeedback by remember { mutableStateOf<Int?>(null) }
    var scrubPreview by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3500)
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

                    val dragResult = drag(down.id) { change ->
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
                                // Vertical → brightness (left) or volume (right)
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
                                // Horizontal → scrub preview
                                val duration = exoPlayer.duration.coerceAtLeast(1L)
                                val seekDelta = (totalDx / width * duration * 0.6f).toLong()
                                val target = (exoPlayer.currentPosition + seekDelta)
                                    .coerceIn(0L, duration)
                                val mins = (target / 1000 / 60).toInt()
                                val secs = (target / 1000 % 60).toInt()
                                scrubPreview = String.format("%d:%02d", mins, secs)
                                volumeFeedback = null
                                brightnessFeedback = null
                            }
                        }
                    }

                    if (!dragged) {
                        // Possible tap / double-tap
                        val secondDown = withTimeoutOrNull(250L) {
                            awaitFirstDown(requireUnconsumed = false)
                        }
                        if (secondDown != null) {
                            secondDown.consume()
                            if (secondDown.position.x < width / 2f) {
                                val newPos = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                seekFeedback = "-10s"
                            } else {
                                val dur = exoPlayer.duration.coerceAtLeast(0L)
                                val newPos = (exoPlayer.currentPosition + 10_000L).coerceAtMost(dur)
                                exoPlayer.seekTo(newPos)
                                seekFeedback = "+10s"
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
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 16.dp, start = 12.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VOL", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Volume  ${volumeFeedback ?: 0}%", color = Color.White, fontSize = 16.sp)
                }
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

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Double-tap sides  ·  Swipe up/down  ·  Swipe sideways",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp
            )
        }
    }
}
