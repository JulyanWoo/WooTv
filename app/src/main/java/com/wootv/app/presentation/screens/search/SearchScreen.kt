package com.wootv.app.presentation.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wootv.app.presentation.theme.*
import com.wootv.app.presentation.viewmodel.SearchViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun SearchScreen(
    onChannelClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    // Track the last clicked channel's ID to restore focus when returning (survives recomposition)
    val lastClickedChannelId = rememberSaveable { mutableStateOf<Long?>(null) }

    val textFieldFocusRequester = FocusRequester()
    val listState = rememberLazyListState()

    // Only auto-focus TextField on first entry (no query and not returning from player)
    LaunchedEffect(Unit) {
        if (query.isBlank() && lastClickedChannelId.value == null) {
            try {
                textFieldFocusRequester.requestFocus()
            } catch (_: Exception) { }
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
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.colors(containerColor = SurfaceCard)
            ) {
                Text("← Volver", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Buscar Canales",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceDark
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChanged(it) },
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .focusRequester(textFieldFocusRequester),
            placeholder = {
                androidx.compose.material3.Text(
                    "Escribe el nombre del canal...",
                    color = OnSurfaceVariantDark
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Blue500,
                unfocusedBorderColor = SurfaceCard,
                focusedTextColor = OnSurfaceDark,
                unfocusedTextColor = OnSurfaceDark,
                cursorColor = Blue400
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (query.isNotBlank()) {
            Text(
                text = "${results.size} resultado${if (results.size != 1) "s" else ""}",
                fontSize = 13.sp,
                color = OnSurfaceVariantDark,
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        if (results.isEmpty() && query.isNotBlank()) {
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🔎", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sin resultados",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceDark
                )
                Text(
                    text = "Intenta con otro término de búsqueda",
                    fontSize = 13.sp,
                    color = OnSurfaceVariantDark,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(results, key = { it.id }) { channel ->
                val itemFocusRequester = remember { FocusRequester() }

                LaunchedEffect(lastClickedChannelId.value) {
                    if (lastClickedChannelId.value == channel.id) {
                        try {
                            itemFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                        lastClickedChannelId.value = null
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Channel Play Card
                    Card(
                        onClick = {
                            lastClickedChannelId.value = channel.id
                            onChannelClick(channel.id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(itemFocusRequester),
                        colors = CardDefaults.colors(containerColor = SurfaceCard),
                        shape = CardDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        scale = CardDefaults.scale(focusedScale = 1.0f),
                        border = CardDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(1.dp, FocusBorder),
                                shape = RoundedCornerShape(10.dp)
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Channel logo
                            val logoUrl = channel.tvgLogo ?: channel.logoUrl
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceCardHover),
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = channel.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (channel.isFavorite) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "❤️",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                val group = channel.groupTitle ?: ""
                                if (group.isNotEmpty()) {
                                    Text(
                                        text = group,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "▶",
                                fontSize = 16.sp,
                                color = Blue400
                            )
                        }
                    }

                    // Favorite Toggle Card
                    Card(
                        onClick = { viewModel.toggleFavorite(channel.id, !channel.isFavorite) },
                        modifier = Modifier.size(48.dp),
                        colors = CardDefaults.colors(
                            containerColor = if (channel.isFavorite) RedLive else SurfaceCard
                        ),
                        shape = CardDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        scale = CardDefaults.scale(focusedScale = 1.0f),
                        border = CardDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(1.dp, FocusBorder),
                                shape = RoundedCornerShape(10.dp)
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (channel.isFavorite) "❤️" else "🤍",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
