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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

@Composable
fun SearchScreen(
    onChannelClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

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
                .fillMaxWidth(),
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
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(results, key = { it.id }) { channel ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Channel Play Card
                    Card(
                        onClick = { onChannelClick(channel.id) },
                        modifier = Modifier.weight(1f),
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
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = channel.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurfaceDark,
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
                                        color = OnSurfaceVariantDark,
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
