package com.wootv.app.presentation.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wootv.app.presentation.theme.*
import com.wootv.app.presentation.viewmodel.PlayerViewModel
import com.wootv.app.presentation.util.DebugLogger
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

@UnstableApi
@Composable
fun PlayerScreen(
    channelId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val channel by viewModel.channel.collectAsStateWithLifecycle()
    val currentProgram by viewModel.currentProgram.collectAsStateWithLifecycle()

    // Configure ExoPlayer with large buffer for stable IPTV streaming on Fire TV
    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,  // Min buffer ms (30s)
                120_000, // Max buffer ms (120s)
                5_000,   // Buffer for playback ms
                15_000   // Buffer for rebuffer ms
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    var showOsd by remember { mutableStateOf(true) }

    // region debug-point hp2-player-listener
    // Add player listener for debugging stream auto-pause issue
    remember(exoPlayer) {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                channel?.let { ch ->
                    DebugLogger.logPlayerState(
                        channelName = ch.name,
                        isPlaying = exoPlayer.isPlaying,
                        playbackState = playbackState,
                        bufferedPercentage = exoPlayer.bufferedPercentage
                    )

                    // Fix: Replay when playback ends (STATE_ENDED = 4)
                    // This handles IPTV streams that end unexpectedly
                    if (playbackState == Player.STATE_ENDED) {
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            DebugLogger.logEvent("playbackEnded", mapOf(
                                "channel" to ch.name,
                                "action" to "replaying"
                            ))
                            exoPlayer.seekTo(0)
                            exoPlayer.playWhenReady = true
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                channel?.let { ch ->
                    DebugLogger.logEvent("isPlayingChanged", mapOf(
                        "isPlaying" to isPlaying,
                        "channel" to ch.name,
                        "position" to exoPlayer.currentPosition
                    ))
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                channel?.let { ch ->
                    DebugLogger.logPlayerError(
                        channelName = ch.name,
                        errorMessage = error.message ?: "Unknown error",
                        errorStack = error.stackTrace?.joinToString("\n") ?: "No stack trace"
                    )

                    // Fix: Retry playback on error only if screen is active/resumed
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        DebugLogger.logEvent("playbackError", mapOf(
                            "channel" to ch.name,
                            "action" to "retrying"
                        ))
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                }
            }
        }
    }.also { listener ->
        exoPlayer.addListener(listener)
    }

    // Periodic buffer status logging
    LaunchedEffect(channel) {
        while (channel != null) {
            channel?.let { ch ->
                DebugLogger.logBufferStatus(
                    channelName = ch.name,
                    bufferedPosition = exoPlayer.bufferedPosition,
                    bufferedDuration = exoPlayer.bufferedPosition + (exoPlayer.duration * exoPlayer.bufferedPercentage / 100),
                    totalDuration = exoPlayer.duration
                )
            }
            delay(30_000) // Log every 30 seconds (reduced to avoid GC pressure on Fire TV)
        }
    }
    // endregion

    // Keep screen on while playing to prevent Fire TV screensaver
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(channelId) {
        viewModel.loadChannel(channelId)
    }

    LaunchedEffect(channel) {
        channel?.let {
            val mediaItem = MediaItem.fromUri(it.streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            showOsd = true
        }
    }

    LaunchedEffect(showOsd, channel) {
        if (showOsd) {
            kotlinx.coroutines.delay(3000)
            showOsd = false
        }
    }

    // Lifecycle-aware player management: pause/stop when app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.playWhenReady = false
                }
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.stop()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
                else -> { }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                showOsd = true
                false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showOsd = !showOsd
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top overlay: channel info
        AnimatedVisibility(
            visible = showOsd,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    channel?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(RedLive, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "EN VIVO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = it.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                it.groupTitle?.let { group ->
                                    Text(
                                        text = group,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                    currentProgram?.let { program ->
                        Text(
                            text = program.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Bottom overlay: navigation controls
        AnimatedVisibility(
            visible = showOsd,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (viewModel.hasPrevious()) {
                        Text(
                            text = "◄ Anterior",
                            color = Blue400,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (viewModel.hasNext()) {
                        Text(
                            text = "Siguiente ►",
                            color = Blue400,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
