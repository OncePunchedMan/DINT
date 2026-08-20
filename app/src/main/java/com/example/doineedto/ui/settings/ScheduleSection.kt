package com.example.doineedto.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.doineedto.R
import java.util.Locale

enum class ScheduleTimeField { START, END }

@Composable
fun ScheduleSection(
    isEnabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    onEnabledChanged: (Boolean) -> Unit,
    onRequestTimePicker: (field: ScheduleTimeField, currentMinutes: Int) -> Unit,
) {
    Column {
        ListItem(
            headlineContent = { Text(stringResource(R.string.schedule_title)) },
            supportingContent = {
                Text(
                    if (isEnabled) {
                        stringResource(R.string.schedule_active_range, formatMinutes(startMinutes), formatMinutes(endMinutes))
                    } else {
                        stringResource(R.string.schedule_description)
                    }
                )
            },
            trailingContent = {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChanged
                )
            }
        )
        if (isEnabled) {
            TimeAdjustRow(
                label = stringResource(R.string.schedule_time_label_start),
                minutes = startMinutes,
                onRequestTimePicker = { onRequestTimePicker(ScheduleTimeField.START, startMinutes) }
            )
            TimeAdjustRow(
                label = stringResource(R.string.schedule_time_label_end),
                minutes = endMinutes,
                onRequestTimePicker = { onRequestTimePicker(ScheduleTimeField.END, endMinutes) }
            )
        }
    }
}

@Composable
private fun TimeAdjustRow(
    label: String,
    minutes: Int,
    onRequestTimePicker: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label: ${formatMinutes(minutes)}",
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRequestTimePicker) {
            Text(stringResource(R.string.set_time))
        }
    }
}

fun formatMinutes(minutes: Int): String {
    val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hour = normalized / 60
    val minute = normalized % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}
