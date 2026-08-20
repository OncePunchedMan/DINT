package com.example.doineedto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.doineedto.data.UnlockLogEntry
import com.example.doineedto.data.UnlockLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_SIZE = 50

@Composable
internal fun FullHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { UnlockLogRepository(context) }
    var entries by remember { mutableStateOf(listOf<UnlockLogEntry>()) }
    var offset by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun loadNextPage() {
        isLoading = true
        val page = withContext(Dispatchers.IO) { repository.getPage(PAGE_SIZE, offset) }
        entries = entries + page
        offset += page.size
        hasMore = page.size == PAGE_SIZE
        isLoading = false
    }

    LaunchedEffect(repository) { loadNextPage() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.full_history_title),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry ->
                HistoryCard(log = entry)
            }
            if (hasMore) {
                item {
                    Button(
                        onClick = { coroutineScope.launch { loadNextPage() } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    ) {
                        Text(stringResource(R.string.load_more_history))
                    }
                }
            }
        }
    }
}
