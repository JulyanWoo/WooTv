package com.wootv.app.presentation.screens.epg

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wootv.app.domain.model.EpgProgram
import com.wootv.app.presentation.viewmodel.EpgViewModel

@Composable
fun EpgScreen(
    channelId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EpgViewModel = hiltViewModel()
) {
    val programs by viewModel.getPrograms(channelId.toString()).collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            text = "Guía de Programación",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(programs) { program ->
                EpgProgramCard(program = program)
            }
        }
    }
}

@Composable
private fun EpgProgramCard(program: EpgProgram) {
    val dateFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val timeRange = "${dateFormat.format(java.util.Date(program.startTime))} - ${dateFormat.format(java.util.Date(program.endTime))}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.colors(
            containerColor = if (program.isCurrent)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = { }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = program.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = timeRange,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

