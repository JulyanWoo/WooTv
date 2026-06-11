package com.wootv.app.presentation.screens.playlistlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wootv.app.domain.model.Playlist
import com.wootv.app.presentation.theme.*
import com.wootv.app.presentation.viewmodel.PlaylistViewModel

@Composable
fun PlaylistListScreen(
    onPlaylistClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mis Listas IPTV",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                Text(
                    text = "${playlists.size} lista${if (playlists.size != 1) "s" else ""} configurada${if (playlists.size != 1) "s" else ""}",
                    fontSize = 13.sp,
                    color = OnSurfaceVariantDark,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.colors(containerColor = SurfaceCard)
                ) {
                    Text("← Volver", fontSize = 14.sp)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.colors(containerColor = Blue500)
                ) {
                    Text("+ Agregar Lista", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (playlists.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📡", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tienes listas configuradas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Agrega una lista M3U para comenzar a ver canales",
                        fontSize = 14.sp,
                        color = OnSurfaceVariantDark
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.colors(containerColor = Blue500)
                    ) {
                        Text("+ Agregar Lista IPTV", fontSize = 15.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        onDelete = { viewModel.deletePlaylist(playlist) },
                        onRefresh = { viewModel.refreshPlaylist(playlist) }
                    )
                }
            }
        }
    }

    // Add Playlist Dialog
    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, url ->
                viewModel.addPlaylist(name, url, null)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceCard, RoundedCornerShape(12.dp)),
        colors = CardDefaults.colors(containerColor = SurfaceVariantDark),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playlist.url,
                        fontSize = 12.sp,
                        color = OnSurfaceVariantDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (playlist.lastRefreshed != null) {
                        val date = java.text.SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(playlist.lastRefreshed))
                        Text(
                            text = "✅ Actualizado: $date",
                            fontSize = 11.sp,
                            color = GreenOnline,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    } else {
                        Text(
                            text = "⏳ Cargando canales...",
                            fontSize = 11.sp,
                            color = Blue400,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.colors(containerColor = SurfaceCard)
                ) {
                    Text("🔄 Actualizar", fontSize = 12.sp)
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.colors(containerColor = ErrorRed.copy(alpha = 0.2f))
                ) {
                    Text("🗑 Eliminar", fontSize = 12.sp, color = ErrorRed)
                }
            }
        }
    }
}

@Composable
private fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceVariantDark,
        titleContentColor = OnSurfaceDark,
        textContentColor = OnSurfaceVariantDark,
        title = {
            androidx.compose.material3.Text(
                text = "Agregar Lista IPTV",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { androidx.compose.material3.Text("Nombre de la lista") },
                    placeholder = { androidx.compose.material3.Text("Ej: Colombia HD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = SurfaceCard,
                        focusedLabelColor = Blue400,
                        cursorColor = Blue400
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { androidx.compose.material3.Text("URL del archivo M3U") },
                    placeholder = { androidx.compose.material3.Text("https://example.com/playlist.m3u") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = SurfaceCard,
                        focusedLabelColor = Blue400,
                        cursorColor = Blue400
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.isNotBlank() && url.isNotBlank()) {
                                onAdd(name, url)
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onAdd(name, url)
                    }
                }
            ) {
                androidx.compose.material3.Text(
                    "Agregar",
                    color = Blue400,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(
                    "Cancelar",
                    color = OnSurfaceVariantDark
                )
            }
        }
    )
}
