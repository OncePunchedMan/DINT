package com.example.doineedto.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.doineedto.R
import com.example.doineedto.data.UnlockLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryCard(log: UnlockLogEntry) {
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatter.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = actionLabel(log.action),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = if (log.action == "skipped") "" else if (log.reason.isBlank()) stringResource(R.string.no_reason_entered) else log.reason,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun actionLabel(action: String): String = when (action) {
    "continue" -> stringResource(R.string.action_label_continue)
    "keep_locked" -> stringResource(R.string.action_label_keep_locked)
    "skipped" -> stringResource(R.string.action_label_skipped)
    else -> stringResource(R.string.action_label_pending)
}
