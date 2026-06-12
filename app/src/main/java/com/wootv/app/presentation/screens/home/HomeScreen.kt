package com.wootv.app.presentation.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wootv.app.R
import com.wootv.app.domain.model.Channel
import com.wootv.app.presentation.theme.*
import com.wootv.app.presentation.viewmodel.HomeViewModel
import com.wootv.app.presentation.viewmodel.MainCategory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import android.view.WindowManager
import com.wootv.app.presentation.util.DebugLogger
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally

@UnstableApi
@Composable
fun HomeScreen(
    onChannelClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val channels by homeViewModel.filteredChannels.collectAsStateWithLifecycle()
    val allChannels by homeViewModel.allChannels.collectAsStateWithLifecycle()
    val showOnlyFavorites by homeViewModel.showOnlyFavorites.collectAsStateWithLifecycle()
    val isLoading by homeViewModel.isLoading.collectAsStateWithLifecycle()
    val playlistNameMap by homeViewModel.playlistNameMap.collectAsStateWithLifecycle()
    val playlists by homeViewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylistId by homeViewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val selectedCategory by homeViewModel.selectedCategory.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val channelIndexMap = remember(allChannels) {
        allChannels.mapIndexed { index, channel -> channel.id to (index + 1) }.toMap()
    }

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

    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var activePlaybackChannel by remember { mutableStateOf<Channel?>(null) }

    // region debug-point hp1-player-listener
    // Add player listener for debugging stream auto-pause issue
    remember(exoPlayer) {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                activePlaybackChannel?.let { channel ->
                    DebugLogger.logPlayerState(
                        channelName = channel.name,
                        isPlaying = exoPlayer.isPlaying,
                        playbackState = playbackState,
                        bufferedPercentage = exoPlayer.bufferedPercentage
                    )

                    // Fix: Replay when playback ends (STATE_ENDED = 4)
                    // This handles IPTV streams that end unexpectedly
                    if (playbackState == Player.STATE_ENDED) {
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            DebugLogger.logEvent("playbackEnded", mapOf(
                                "channel" to channel.name,
                                "action" to "replaying"
                            ))
                            exoPlayer.seekTo(0)
                            exoPlayer.playWhenReady = true
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                activePlaybackChannel?.let { channel ->
                    DebugLogger.logEvent("isPlayingChanged", mapOf(
                        "isPlaying" to isPlaying,
                        "channel" to channel.name,
                        "position" to exoPlayer.currentPosition
                    ))
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                activePlaybackChannel?.let { channel ->
                    DebugLogger.logPlayerError(
                        channelName = channel.name,
                        errorMessage = error.message ?: "Unknown error",
                        errorStack = error.stackTrace?.joinToString("\n") ?: "No stack trace"
                    )

                    // Fix: Retry playback on error only if screen is active/resumed
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        DebugLogger.logEvent("playbackError", mapOf(
                            "channel" to channel.name,
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
    LaunchedEffect(activePlaybackChannel) {
        while (activePlaybackChannel != null) {
            activePlaybackChannel?.let { channel ->
                DebugLogger.logBufferStatus(
                    channelName = channel.name,
                    bufferedPosition = exoPlayer.bufferedPosition,
                    bufferedDuration = exoPlayer.bufferedPosition + (exoPlayer.duration * exoPlayer.bufferedPercentage / 100),
                    totalDuration = exoPlayer.duration
                )
            }
            delay(30_000) // Log every 30 seconds (reduced to avoid GC pressure on Fire TV)
        }
    }
    // endregion

    // Keep screen on while a channel is actively being previewed
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(activePlaybackChannel) {
        if (activePlaybackChannel != null) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-play first channel when channels load or change category/playlist
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            focusedChannel = channels.first()
        } else {
            focusedChannel = null
        }
    }

    // Debounce focus-based preview
    LaunchedEffect(focusedChannel) {
        if (focusedChannel == null) {
            activePlaybackChannel = null
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(400)
        activePlaybackChannel = focusedChannel
    }

    // Play active channel
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
        val availableGroups by homeViewModel.availableGroups.collectAsStateWithLifecycle()
        val selectedGroup by homeViewModel.selectedGroup.collectAsStateWithLifecycle()

        // Top Bar
        HomeScreenTopBar(
            availableGroups = availableGroups,
            selectedGroup = selectedGroup,
            showOnlyFavorites = showOnlyFavorites,
            onShowOnlyFavoritesToggle = { homeViewModel.setShowOnlyFavorites(it) },
            onGroupSelect = { homeViewModel.selectGroup(it) },
            onSearchClick = onSearchClick
        )

        // Main Content: Sidebar + Channel List + Preview
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            // Left Sidebar
            CategorySidebar(
                selectedCategory = selectedCategory,
                onCategorySelect = { homeViewModel.selectCategory(it) }
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Channel List
            ChannelListPanel(
                channels = channels,
                focusedChannel = focusedChannel,
                isLoading = isLoading,
                playlistNameMap = playlistNameMap,
                channelIndexMap = channelIndexMap,
                onFocusChannel = { focusedChannel = it },
                onChannelClick = onChannelClick,
                modifier = Modifier
                    .weight(0.26f)
                    .fillMaxHeight()
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Right: Preview Player + Info
            PreviewPanel(
                exoPlayer = exoPlayer,
                activeChannel = activePlaybackChannel,
                onToggleFavorite = { channelId, isFav -> homeViewModel.toggleFavorite(channelId, isFav) },
                modifier = Modifier
                    .weight(0.74f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun HomeScreenTopBar(
    availableGroups: List<String>,
    selectedGroup: String?,
    showOnlyFavorites: Boolean,
    onShowOnlyFavoritesToggle: (Boolean) -> Unit,
    onGroupSelect: (String?) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showGroupDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Logo (reduced size 35%)
        Image(
            painter = painterResource(id = R.drawable.ic_top_logo),
            contentDescription = "WooTv Logo",
            modifier = Modifier.height(23.dp),
            contentScale = ContentScale.Fit
        )

        // Right: Horizontal row of Action Cards (Favorites, Categories, Search) - low profile TV size
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Favorites Button
            Card(
                onClick = { onShowOnlyFavoritesToggle(!showOnlyFavorites) },
                colors = CardDefaults.colors(
                    containerColor = if (showOnlyFavorites) Blue500.copy(alpha = 0.6f) else Color.Transparent,
                    focusedContainerColor = SurfaceCard
                ),
                shape = CardDefaults.shape(shape = RoundedCornerShape(6.dp)),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(1.dp, FocusBorder),
                        shape = RoundedCornerShape(6.dp)
                    ),
                    border = Border.None
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritos",
                        tint = if (showOnlyFavorites) Color.White else OnSurfaceVariantDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Favoritos",
                        fontSize = 10.5.sp,
                        color = if (showOnlyFavorites) Color.White else OnSurfaceVariantDark
                    )
                }
            }

            // Categories Selector Button
            Card(
                onClick = { showGroupDialog = true },
                colors = CardDefaults.colors(
                    containerColor = if (selectedGroup != null) Blue500.copy(alpha = 0.6f) else Color.Transparent,
                    focusedContainerColor = SurfaceCard
                ),
                shape = CardDefaults.shape(shape = RoundedCornerShape(6.dp)),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(1.dp, FocusBorder),
                        shape = RoundedCornerShape(6.dp)
                    ),
                    border = Border.None
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Categorías",
                        tint = if (selectedGroup != null) Color.White else OnSurfaceVariantDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedGroup ?: "Categorías",
                        fontSize = 10.5.sp,
                        color = if (selectedGroup != null) Color.White else OnSurfaceVariantDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
            }

            // Search Button
            Card(
                onClick = onSearchClick,
                colors = CardDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = SurfaceCard
                ),
                shape = CardDefaults.shape(shape = RoundedCornerShape(6.dp)),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(1.dp, FocusBorder),
                        shape = RoundedCornerShape(6.dp)
                    ),
                    border = Border.None
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Buscar",
                        fontSize = 10.5.sp,
                        color = OnSurfaceVariantDark
                    )
                }
            }
        }
    }

    // Modal Dialog for Category Switcher
    if (showGroupDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showGroupDialog = false }
        ) {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .wrapContentHeight()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Seleccionar Categoría",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.height(300.dp), // fixed height for category scrollability
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "Todas las categorías" option at top
                        item {
                            val isAllSelected = selectedGroup == null
                            Card(
                                onClick = {
                                    onGroupSelect(null)
                                    showGroupDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.colors(
                                    containerColor = if (isAllSelected) Blue500 else SurfaceCardHover,
                                    focusedContainerColor = SurfaceCardHover
                                ),
                                shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                border = CardDefaults.border(
                                    focusedBorder = Border(
                                        border = BorderStroke(1.dp, FocusBorder),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Todas las categorías",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isAllSelected) Color.White else OnSurfaceVariantDark
                                    )
                                    if (isAllSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Fixed Categories
                        val fixedCategories = listOf("Noticias", "Deportes", "Películas", "Infantil")
                        items(fixedCategories) { category ->
                            val isSelected = category == selectedGroup
                            Card(
                                onClick = {
                                    onGroupSelect(category)
                                    showGroupDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.colors(
                                    containerColor = if (isSelected) Blue500 else SurfaceCardHover,
                                    focusedContainerColor = SurfaceCardHover
                                ),
                                shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                border = CardDefaults.border(
                                    focusedBorder = Border(
                                        border = BorderStroke(1.dp, FocusBorder),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else OnSurfaceVariantDark
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Custom M3U parsed groups
                        items(availableGroups) { group ->
                            val isSelected = group == selectedGroup
                            Card(
                                onClick = {
                                    onGroupSelect(group)
                                    showGroupDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.colors(
                                    containerColor = if (isSelected) Blue500 else SurfaceCardHover,
                                    focusedContainerColor = SurfaceCardHover
                                ),
                                shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                border = CardDefaults.border(
                                    focusedBorder = Border(
                                        border = BorderStroke(1.dp, FocusBorder),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else OnSurfaceVariantDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
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

@Composable
private fun CategorySidebar(
    selectedCategory: MainCategory,
    onCategorySelect: (MainCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSidebarFocused by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarFocused) 180.dp else 64.dp,
        animationSpec = tween(durationMillis = 250),
        label = "SidebarWidth"
    )

    Column(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .padding(vertical = 16.dp)
            .onFocusChanged { isSidebarFocused = it.hasFocus },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val categories = listOf(
            Triple(MainCategory.TV, Icons.Default.Tv, "TV"),
            Triple(MainCategory.MOVIES, Icons.Default.Movie, "Cine"),
            Triple(MainCategory.SERIES, Icons.Default.VideoLibrary, "Series"),
            Triple(MainCategory.ANIME, Icons.Default.AutoAwesome, "Anime")
        )

        categories.forEach { (category, icon, label) ->
            val isSelected = selectedCategory == category
            
            Card(
                onClick = { onCategorySelect(category) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.colors(
                    containerColor = if (isSelected) Blue500 else Color.Transparent,
                    focusedContainerColor = SurfaceCardHover,
                    pressedContainerColor = Blue600
                ),
                shape = CardDefaults.shape(shape = RoundedCornerShape(10.dp)),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(1.dp, FocusBorder),
                        shape = RoundedCornerShape(10.dp)
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isSidebarFocused) Arrangement.Start else Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else OnSurfaceVariantDark,
                        modifier = Modifier.size(22.dp)
                    )
                    
                    AnimatedVisibility(
                        visible = isSidebarFocused,
                        enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else OnSurfaceVariantDark,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelListPanel(
    channels: List<Channel>,
    focusedChannel: Channel?,
    isLoading: Boolean,
    playlistNameMap: Map<Long, String>,
    channelIndexMap: Map<Long, Int>,
    onFocusChannel: (Channel) -> Unit,
    onChannelClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Channel count header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Canales",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceDark
            )
            Text(
                text = "${channels.size}",
                fontSize = 11.sp,
                color = OnSurfaceVariantDark
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Blue400,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cargando canales...",
                        color = OnSurfaceVariantDark,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (channels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.TvOff,
                        contentDescription = null,
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No hay canales",
                        color = OnSurfaceVariantDark,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                    val isFocused = focusedChannel?.id == channel.id
                    val originalIndex = channelIndexMap[channel.id] ?: (index + 1)
                    ChannelListItem(
                        channel = channel,
                        index = originalIndex,
                        isFocused = isFocused,
                        playlistNameMap = playlistNameMap,
                        onFocus = { onFocusChannel(channel) },
                        onClick = { onChannelClick(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelListItem(
    channel: Channel,
    index: Int,
    isFocused: Boolean,
    playlistNameMap: Map<Long, String>,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    val bgColor = if (isFocused) SurfaceCardHover else Color.Transparent
    val logoUrl = channel.tvgLogo ?: channel.logoUrl

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus() },
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
                    text = "$index",
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

            // Live indicator dot
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.name,
                        fontSize = 12.sp,
                        fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isFocused) Color.White else Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (channel.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = RedLive,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                val group = channel.groupTitle ?: ""
                if (group.isNotEmpty()) {
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

@Composable
private fun PreviewPanel(
    exoPlayer: ExoPlayer,
    activeChannel: Channel?,
    onToggleFavorite: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Video Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(0.5.dp, SurfaceCard.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (activeChannel != null) {
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

                // Channel name overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Live badge
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
                                text = activeChannel.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val group = activeChannel.groupTitle ?: ""
                        if (group.isNotEmpty()) {
                            Text(
                                text = group,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        val isFavorite = activeChannel.isFavorite
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            onClick = { onToggleFavorite(activeChannel.id, !isFavorite) },
                            colors = CardDefaults.colors(
                                containerColor = if (isFavorite) RedLive else SurfaceCard,
                                focusedContainerColor = SurfaceCardHover
                            ),
                            shape = CardDefaults.shape(shape = RoundedCornerShape(6.dp)),
                            border = CardDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(1.dp, FocusBorder),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFavorite) "Quitar de Favoritos" else "Agregar a Favoritos",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                // Placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Selecciona un canal",
                        color = OnSurfaceVariantDark,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Usa las flechas para navegar",
                        color = OnSurfaceVariantDark.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
