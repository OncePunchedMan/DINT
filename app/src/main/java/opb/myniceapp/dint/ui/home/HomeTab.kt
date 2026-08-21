package opb.myniceapp.dint.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.data.DailyUnlockCount
import opb.myniceapp.dint.ui.common.HeroCard
import opb.myniceapp.dint.ui.common.SettingsGroup
import opb.myniceapp.dint.ui.main.HistoryUiState

@Composable
fun HomeTab(
    historyState: HistoryUiState,
    onViewFullHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAllHistory by rememberSaveable { mutableStateOf(false) }
    val visibleLogs = if (showAllHistory) historyState.logs else historyState.logs.take(5)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = stringResource(R.string.home_title),
                body = stringResource(R.string.main_intro),
            )
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.home_why_title),
                icon = Icons.Outlined.Lightbulb,
                content = {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.home_why_title)) },
                        supportingContent = { Text(stringResource(R.string.home_why_body)) }
                    )
                }
            )
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.emergency_title),
                icon = Icons.Outlined.HealthAndSafety,
                content = {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.emergency_title)) },
                        supportingContent = { Text(stringResource(R.string.emergency_body)) }
                    )
                }
            )
        }
        item {
            StatsSection(
                totalUnlocks = historyState.totalUnlocks,
                continuedUnlocks = historyState.continuedUnlocks,
                keptLockedUnlocks = historyState.keptLockedUnlocks,
                dailyCounts = historyState.dailyCounts,
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(visibleLogs) { log ->
            HistoryCard(log = log)
        }
        if (historyState.logs.size > 5 && !showAllHistory) {
            item {
                Button(
                    onClick = { showAllHistory = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(stringResource(R.string.show_more_history))
                }
            }
        }
        item {
            TextButton(
                onClick = onViewFullHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.view_full_history))
            }
        }
        item {
            Text(
                text = stringResource(R.string.platform_note),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StatsSection(
    totalUnlocks: Int,
    continuedUnlocks: Int,
    keptLockedUnlocks: Int,
    dailyCounts: List<DailyUnlockCount>,
) {
    var showAllDays by rememberSaveable { mutableStateOf(false) }
    val visibleDailyCounts = if (showAllDays) dailyCounts else dailyCounts.take(3)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Outlined.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_label_unlocks), totalUnlocks.toString(), Icons.Outlined.LockOpen, Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_label_continued), continuedUnlocks.toString(), Icons.Outlined.CheckCircle, Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_label_stopped), keptLockedUnlocks.toString(), Icons.Outlined.Lock, Modifier.weight(1f))
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleDailyCounts.forEach { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(day.label, style = MaterialTheme.typography.bodyMedium)
                        Text("${day.count}", fontWeight = FontWeight.SemiBold)
                    }
                }
                if (dailyCounts.size > 3 && !showAllDays) {
                    Button(
                        onClick = { showAllDays = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.show_more_days))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
