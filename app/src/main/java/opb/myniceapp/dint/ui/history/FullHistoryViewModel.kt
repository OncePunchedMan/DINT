package opb.myniceapp.dint.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import opb.myniceapp.dint.AppContainer
import opb.myniceapp.dint.data.UnlockLogEntry
import opb.myniceapp.dint.data.UnlockLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_SIZE = 50

class FullHistoryViewModel(private val repository: UnlockLogRepository) : ViewModel() {
    private val _entries = MutableStateFlow<List<UnlockLogEntry>>(emptyList())
    val entries: StateFlow<List<UnlockLogEntry>> = _entries.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var offset = 0

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        if (_isLoading.value || !_hasMore.value) return

        _isLoading.value = true
        viewModelScope.launch {
            val page = withContext(Dispatchers.IO) { repository.getPage(PAGE_SIZE, offset) }
            _entries.value = _entries.value + page
            offset += page.size
            _hasMore.value = page.size == PAGE_SIZE
            _isLoading.value = false
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { FullHistoryViewModel(container.unlockLogRepository) }
        }
    }
}
