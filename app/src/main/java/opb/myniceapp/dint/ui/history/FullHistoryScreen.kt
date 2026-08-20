package opb.myniceapp.dint.ui.history

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import opb.myniceapp.dint.DintApplication
import opb.myniceapp.dint.R
import opb.myniceapp.dint.ui.home.HistoryCard

@Composable
fun FullHistoryScreen(onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as DintApplication).container
    val viewModel: FullHistoryViewModel = viewModel(factory = FullHistoryViewModel.factory(container))

    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

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
                        onClick = { viewModel.loadNextPage() },
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
