package com.wootv.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wootv.app.domain.model.EpgProgram

@Composable
fun EpgTimeline(
    programs: List<EpgProgram>,
    currentTime: Long,
    onProgramClick: (EpgProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

    LazyRow(modifier = modifier) {
        items(programs) { program ->
            Card(
                onClick = { onProgramClick(program) },
                modifier = Modifier.padding(horizontal = 4.dp),
                colors = CardDefaults.colors(
                    containerColor = if (program.startTime <= currentTime && program.endTime > currentTime)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2
                    )
                    Text(
                        text = dateFormat.format(java.util.Date(program.startTime)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

