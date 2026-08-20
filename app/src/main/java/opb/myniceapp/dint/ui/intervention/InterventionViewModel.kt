package opb.myniceapp.dint.ui.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import opb.myniceapp.dint.AppContainer
import opb.myniceapp.dint.data.AppPreferences
import opb.myniceapp.dint.data.RepositoryScope
import opb.myniceapp.dint.data.UnlockAction
import opb.myniceapp.dint.data.UnlockLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InterventionViewModel(
    preferences: AppPreferences,
    private val repository: UnlockLogRepository,
) : ViewModel() {

    val hardModeEnabled: Boolean = preferences.isHardModeEnabled()
    val hideLockOutcomes: Boolean = preferences.shouldHideLockOutcomes()
    val waitMillis: Long = preferences.waitDurationMillis()

    private val _reason = MutableStateFlow("")
    val reason: StateFlow<String> = _reason.asStateFlow()

    private val _isRepeatedDistractionReason = MutableStateFlow(false)
    val isRepeatedDistractionReason: StateFlow<Boolean> = _isRepeatedDistractionReason.asStateFlow()

    private var repeatedCheckJob: Job? = null

    fun onReasonChanged(value: String) {
        _reason.value = value
        repeatedCheckJob?.cancel()
        repeatedCheckJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.shouldRejectRepeatedReason(value) }
            _isRepeatedDistractionReason.value = result
        }
    }

    // Fire-and-forget via RepositoryScope, not viewModelScope: this write must survive the
    // Activity finishing (Continue/Keep locked/Emergency skip all call finish() right after),
    // and viewModelScope is cancelled around the same moment for the same reason lifecycleScope
    // was rejected in InterventionActivity historically -- see UnlockLogRepository.RepositoryScope.
    fun completeUnlock(reason: String, action: UnlockAction) {
        RepositoryScope.launch { repository.completeLatestUnlock(reason, action) }
    }

    fun clearPendingLog() {
        RepositoryScope.launch { repository.clearPendingLog() }
    }

    suspend fun reasonUsageCounts(reason: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val today = repository.countReasonUses(reason, repository.todayStartMillis())
        val lastHour = repository.countReasonUses(reason, System.currentTimeMillis() - 3_600_000L)
        today to lastHour
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                InterventionViewModel(
                    preferences = container.preferences,
                    repository = container.unlockLogRepository,
                )
            }
        }
    }
}
