package com.wootv.app.presentation.screens.channellist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wootv.app.domain.model.Channel
import com.wootv.app.presentation.theme.*
import com.wootv.app.presentation.viewmodel.ChannelListViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv

@UnstableApi
@Composable
fun ChannelListScreen(
    playlistId: Long,
    onChannelClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ChannelListViewModel = hiltViewModel()
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val channelIndexMap = remember(allChannels) {
        allChannels.mapIndexed { index, channel -> channel.id to (index + 1) }.toMap()
    }

    // Configure ExoPlayer with larger buffer for stable IPTV streaming on Fire TV
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

    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var activePlaybackChannel by remember { mutableStateOf<Channel?>(null) }

    // Add player listener to handle STATE_ENDED
    remember(exoPlayer) {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Fix: Replay when playback ends (STATE_ENDED = 4)
                if (playbackState == Player.STATE_ENDED) {
                    activePlaybackChannel?.let {
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            exoPlayer.seekTo(0)
                            exoPlayer.playWhenReady = true
                        }
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Fix: Retry playback on error only if screen is active/resumed
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            }
        }
    }.also { listener ->
        exoPlayer.addListener(listener)
    }

    LaunchedEffect(playlistId) {
        viewModel.loadChannels(playlistId)
    }

    // Auto-play first channel when channels load
    LaunchedEffect(channels) {
        if (channels.isNotEmpty() && focusedChannel == null) {
            focusedChannel = channels.first()
        }
    }

    LaunchedEffect(focusedChannel) {
        if (focusedChannel == null) {
            activePlaybackChannel = null
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(500)
        activePlaybackChannel = focusedChannel
    }

    LaunchedEffect(activePlaybackChannel) {
        activePlaybackChannel?.let { channel ->
            try {
                val mediaItem = MediaItem.fromUri(channel.streamUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } ?: run {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
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
                    if (activePlaybackChannel != null) {
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.colors(containerColor = SurfaceCard)
                ) {
                    Text("← Volver", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Canales",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${channels.size})",
                    fontSize = 14.sp,
                    color = OnSurfaceVariantDark
                )
            }
        }

        // Main content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Left: Channel List
            LazyColumn(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                    val isFocused = focusedChannel?.id == channel.id
                    val bgColor = if (isFocused) SurfaceCardHover else Color.Transparent
                    val logoUrl = channel.tvgLogo ?: channel.logoUrl
                    val originalIndex = channelIndexMap[channel.id] ?: (index + 1)

                    Card(
                        onClick = { onChannelClick(channel.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) focusedChannel = channel },
                        colors = CardDefaults.colors(containerColor = bgColor),
                        shape = CardDefaults.shape(shape = RoundedCornerShape(6.dp)),
                        border = CardDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(1.dp, FocusBorder),
                                shape = RoundedCornerShape(6.dp)
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Channel number as a compact boxed badge/pill
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isFocused) Blue500.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$originalIndex",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFocused) Blue400 else Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Channel logo
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceCard),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(logoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = OnSurfaceVariantDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isFocused) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(RedLive)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Channel info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                channel.groupTitle?.let { group ->
                                    Text(
                                        text = group,
                                        fontSize = 9.sp,
                                        color = if (isFocused) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.45f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Preview Player
            Box(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, SurfaceCard, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (activePlaybackChannel != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Channel info overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                Text(
                                    text = activePlaybackChannel?.name ?: "",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            activePlaybackChannel?.groupTitle?.let { group ->
                                Text(
                                    text = group,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "📺", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Selecciona un canal",
                            color = OnSurfaceVariantDark,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
