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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight

@Composable
fun HomeScreen(
    onChannelClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val channels by homeViewModel.filteredChannels.collectAsStateWithLifecycle()
    val showOnlyFavorites by homeViewModel.showOnlyFavorites.collectAsStateWithLifecycle()
    val isLoading by homeViewModel.isLoading.collectAsStateWithLifecycle()
    val playlistNameMap by homeViewModel.playlistNameMap.collectAsStateWithLifecycle()
    val playlists by homeViewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylistId by homeViewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val selectedCategory by homeViewModel.selectedCategory.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var focusedChannel by remember { mutableStateOf<Channel?>(null) }
    var activePlaybackChannel by remember { mutableStateOf<Channel?>(null) }

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

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
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
        HomeScreenTopBar(
            playlists = playlists,
            selectedPlaylistId = selectedPlaylistId,
            showOnlyFavorites = showOnlyFavorites,
            onShowOnlyFavoritesToggle = { homeViewModel.setShowOnlyFavorites(it) },
            onPlaylistSelect = { homeViewModel.selectPlaylist(it.id) },
            onSearchClick = onSearchClick
        )

        // Main Content: Sidebar + Channel List + Preview
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
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
                onFocusChannel = { focusedChannel = it },
                onChannelClick = onChannelClick,
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight()
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Right: Preview Player + Info
            PreviewPanel(
                exoPlayer = exoPlayer,
                activeChannel = activePlaybackChannel,
                onToggleFavorite = { channelId, isFav -> homeViewModel.toggleFavorite(channelId, isFav) },
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun HomeScreenTopBar(
    playlists: List<com.wootv.app.domain.model.Playlist>,
    selectedPlaylistId: Long?,
    showOnlyFavorites: Boolean,
    onShowOnlyFavoritesToggle: (Boolean) -> Unit,
    onPlaylistSelect: (com.wootv.app.domain.model.Playlist) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPlaylistDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Logo
        Image(
            painter = painterResource(id = R.drawable.ic_top_logo),
            contentDescription = "WooTv Logo",
            modifier = Modifier.height(36.dp),
            contentScale = ContentScale.Fit
        )

        // Right: Horizontal row of Action Cards (Favorites, Playlists, Search)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Favorites Button
            Card(
                onClick = { onShowOnlyFavoritesToggle(!showOnlyFavorites) },
                colors = CardDefaults.colors(
                    containerColor = if (showOnlyFavorites) Blue500 else SurfaceCard,
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritos",
                        tint = if (showOnlyFavorites) Color.White else OnSurfaceVariantDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Favoritos",
                        fontSize = 13.sp,
                        color = if (showOnlyFavorites) Color.White else OnSurfaceVariantDark
                    )
                }
            }

            // Playlist Switcher Button
            Card(
                onClick = { showPlaylistDialog = true },
                colors = CardDefaults.colors(
                    containerColor = SurfaceCard,
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Listas",
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Listas",
                        fontSize = 13.sp,
                        color = OnSurfaceVariantDark
                    )
                }
            }

            // Search Button
            Card(
                onClick = onSearchClick,
                colors = CardDefaults.colors(
                    containerColor = SurfaceCard,
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Buscar",
                        fontSize = 13.sp,
                        color = OnSurfaceVariantDark
                    )
                }
            }
        }
    }

    // Modal Dialog for Playlist Switcher
    if (showPlaylistDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPlaylistDialog = false }
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
                        text = "Seleccionar Lista",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists) { playlist ->
                            val isSelected = playlist.id == selectedPlaylistId
                            Card(
                                onClick = {
                                    onPlaylistSelect(playlist)
                                    showPlaylistDialog = false
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
                                        text = playlist.name,
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
    Column(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .padding(vertical = 16.dp),
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
                    .size(64.dp),
                colors = CardDefaults.colors(
                    containerColor = if (isSelected) Blue500 else Color.Transparent,
                    focusedContainerColor = SurfaceCardHover,
                    pressedContainerColor = Blue600
                ),
                shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(1.dp, FocusBorder),
                        shape = RoundedCornerShape(12.dp)
                    )
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else OnSurfaceVariantDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else OnSurfaceVariantDark
                    )
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
    onFocusChannel: (Channel) -> Unit,
    onChannelClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Channel count header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Canales",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceDark
            )
            Text(
                text = "${channels.size}",
                fontSize = 13.sp,
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
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cargando canales...",
                        color = OnSurfaceVariantDark,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Por favor espera...",
                        color = Blue400,
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
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay canales",
                        color = OnSurfaceVariantDark,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona otra categoría o lista...",
                        color = Blue400,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                    val isFocused = focusedChannel?.id == channel.id
                    ChannelListItem(
                        channel = channel,
                        index = index + 1,
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

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus() },
        colors = CardDefaults.colors(containerColor = bgColor),
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel number
            Text(
                text = "$index",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isFocused) Blue400 else OnSurfaceVariantDark,
                modifier = Modifier.width(28.dp)
            )

            // Live indicator dot
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(RedLive)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Channel info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.name,
                        fontSize = 14.sp,
                        fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isFocused) OnSurfaceDark else OnSurfaceVariantDark,
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
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                val group = channel.groupTitle ?: ""
                if (group.isNotEmpty()) {
                    Text(
                        text = group,
                        fontSize = 11.sp,
                        color = OnSurfaceVariantDark.copy(alpha = 0.7f),
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
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(1.dp, SurfaceCard, RoundedCornerShape(12.dp)),
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
                            shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
                            border = CardDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(1.dp, FocusBorder),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFavorite) "Quitar de Favoritos" else "Agregar a Favoritos",
                                    fontSize = 12.sp,
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
