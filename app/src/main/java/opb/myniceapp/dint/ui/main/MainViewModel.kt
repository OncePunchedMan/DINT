package opb.myniceapp.dint.ui.main

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import opb.myniceapp.dint.AppContainer
import opb.myniceapp.dint.R
import opb.myniceapp.dint.admin.FocusDeviceAdminReceiver
import opb.myniceapp.dint.data.AppPreferences
import opb.myniceapp.dint.data.AppTargetSelection
import opb.myniceapp.dint.data.AppUpdate
import opb.myniceapp.dint.data.AppUpdateManager
import opb.myniceapp.dint.data.DailyUnlockCount
import opb.myniceapp.dint.data.LaunchableApp
import opb.myniceapp.dint.data.PresetTargetCategory
import opb.myniceapp.dint.data.ScheduleWindow
import opb.myniceapp.dint.data.UnlockLogEntry
import opb.myniceapp.dint.data.UnlockLogRepository
import opb.myniceapp.dint.data.UpdateCheckResult
import opb.myniceapp.dint.data.queryLaunchableApps
import opb.myniceapp.dint.work.UpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val preferences: AppPreferences,
    private val unlockLogRepository: UnlockLogRepository,
    private val updateManager: AppUpdateManager,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _historyState = MutableStateFlow(HistoryUiState.EMPTY)
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    init {
        if (preferences.isBackgroundUpdateCheckEnabled()) {
            UpdateScheduler.schedule(appContext)
        } else {
            UpdateScheduler.cancel(appContext)
        }
        viewModelScope.launch(Dispatchers.IO) {
            unlockLogRepository.migrateFromPreferencesIfNeeded(appContext)
        }
        refreshSystemStatus()
        refreshHistory()
    }

    fun onResume() {
        refreshSystemStatus()
        refreshHistory()
    }

    fun onFrictionChanged(value: Int) {
        preferences.setFriction(value)
        _uiState.value = _uiState.value.copy(friction = value)
    }

    fun onHardModeChanged(enabled: Boolean) {
        preferences.setHardModeEnabled(enabled)
        _uiState.value = _uiState.value.copy(hardModeEnabled = enabled)
    }

    fun onHideLockOutcomesChanged(enabled: Boolean) {
        preferences.setHideLockOutcomes(enabled)
        _uiState.value = _uiState.value.copy(hideLockOutcomes = enabled)
    }

    fun onScheduleEnabledChanged(enabled: Boolean) {
        preferences.setScheduleEnabled(enabled)
        _uiState.value = _uiState.value.copy(scheduleWindow = preferences.getScheduleWindow())
    }

    fun onScheduleStartChanged(minutes: Int) {
        preferences.setScheduleStartMinutes(minutes)
        _uiState.value = _uiState.value.copy(scheduleWindow = preferences.getScheduleWindow())
    }

    fun onScheduleEndChanged(minutes: Int) {
        preferences.setScheduleEndMinutes(minutes)
        _uiState.value = _uiState.value.copy(scheduleWindow = preferences.getScheduleWindow())
    }

    fun onExcludedAppAdded(packageName: String) {
        preferences.addExcludedPackage(packageName)
        _uiState.value = _uiState.value.copy(excludedPackages = preferences.getExcludedPackages())
    }

    fun onExcludedAppRemoved(packageName: String) {
        preferences.removeExcludedPackage(packageName)
        _uiState.value = _uiState.value.copy(excludedPackages = preferences.getExcludedPackages())
    }

    fun onAppTargetSelected(category: PresetTargetCategory, selection: AppTargetSelection) {
        preferences.setAppTargetSelection(category, selection)
        _uiState.value = _uiState.value.copy(
            appTargetSelections = _uiState.value.appTargetSelections + (category to selection)
        )
    }

    fun onAppTargetCleared(category: PresetTargetCategory) {
        preferences.clearAppTargetSelection(category)
        _uiState.value = _uiState.value.copy(
            appTargetSelections = _uiState.value.appTargetSelections + (category to null)
        )
    }

    fun onBackgroundUpdateCheckToggled(enabled: Boolean) {
        preferences.setBackgroundUpdateCheckEnabled(enabled)
        if (enabled) {
            UpdateScheduler.schedule(appContext)
        } else {
            UpdateScheduler.cancel(appContext)
        }
        _uiState.value = _uiState.value.copy(backgroundUpdateCheckEnabled = enabled)
    }

    fun onOnboardingCompleted() {
        preferences.markOnboardingCompleted()
        refreshSystemStatus()
    }

    fun checkForUpdate() {
        _updateState.value = _updateState.value.copy(isChecking = true, message = null)

        viewModelScope.launch {
            _updateState.value = try {
                when (val result = withContext(Dispatchers.IO) { updateManager.checkForUpdate() }) {
                    is UpdateCheckResult.Available -> UpdateUiState(
                        availableUpdate = result.update,
                        message = appContext.getString(R.string.update_available_message, result.update.versionName),
                    )

                    UpdateCheckResult.UpToDate -> UpdateUiState(
                        message = appContext.getString(R.string.update_up_to_date),
                    )
                }
            } catch (error: Exception) {
                UpdateUiState(message = appContext.getString(R.string.update_check_failed, error.readableMessage()))
            }
        }
    }

    fun installUpdate() {
        val update = _updateState.value.availableUpdate ?: return

        if (!updateManager.canRequestPackageInstalls()) {
            _updateState.value = _updateState.value.copy(
                message = appContext.getString(R.string.update_install_permission_needed),
            )
            updateManager.openUnknownAppSourcesSettings()
            return
        }

        _updateState.value = _updateState.value.copy(
            isInstalling = true,
            message = appContext.getString(R.string.update_downloading),
        )

        viewModelScope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) { updateManager.downloadUpdateApk(update) }
                updateManager.promptInstall(apkFile)
                _updateState.value = _updateState.value.copy(
                    isInstalling = false,
                    message = appContext.getString(R.string.update_install_prompt_opened),
                )
            } catch (error: Exception) {
                _updateState.value = _updateState.value.copy(
                    isInstalling = false,
                    message = appContext.getString(R.string.update_install_failed, error.readableMessage()),
                )
            }
        }
    }

    private fun refreshSystemStatus() {
        val hasCompletedOnboarding = preferences.hasCompletedOnboarding()
        _uiState.value = _uiState.value.copy(
            friction = safeRead(defaultValue = 40) { preferences.getFriction() },
            hardModeEnabled = safeRead(defaultValue = false) { preferences.isHardModeEnabled() },
            hideLockOutcomes = safeRead(defaultValue = false) { preferences.shouldHideLockOutcomes() },
            scheduleWindow = safeRead(
                defaultValue = ScheduleWindow(
                    isEnabled = false,
                    startMinutes = 8 * 60,
                    endMinutes = 22 * 60,
                )
            ) { preferences.getScheduleWindow() },
            isBatteryOptimizationIgnored = safeRead(defaultValue = false) { isBatteryOptimizationIgnored(appContext) },
            areNotificationsEnabled = safeRead(defaultValue = false) { areNotificationsEnabled(appContext) },
            isAccessibilityEnabled = safeRead(defaultValue = false) { isAccessibilityEnabled(appContext) },
            isDeviceAdminEnabled = safeRead(defaultValue = false) { isDeviceAdminEnabled(appContext) },
            appVersionName = safeRead(defaultValue = "1.0") { appVersionName(appContext) },
            hasCompletedOnboarding = hasCompletedOnboarding,
            shouldShowPermissionSetup = !hasCompletedOnboarding || safeRead(defaultValue = true) { hasMissingSetupItems(appContext) },
            excludedPackages = safeRead(defaultValue = emptySet()) { preferences.getExcludedPackages() },
            appTargetSelections = safeRead(defaultValue = emptyMap()) {
                PresetTargetCategory.entries.associateWith { preferences.getAppTargetSelection(it) }
            },
            launchableApps = safeRead(defaultValue = emptyList()) { queryLaunchableApps(appContext) },
            backgroundUpdateCheckEnabled = safeRead(defaultValue = true) { preferences.isBackgroundUpdateCheckEnabled() },
        )
    }

    private fun refreshHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val next = HistoryUiState(
                totalUnlocks = unlockLogRepository.count(),
                continuedUnlocks = unlockLogRepository.countContinued(),
                keptLockedUnlocks = unlockLogRepository.countKeptLocked(),
                dailyCounts = unlockLogRepository.getDailyUnlockCounts(),
                logs = unlockLogRepository.getRecent(20),
            )
            _historyState.value = next
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel(
                    preferences = container.preferences,
                    unlockLogRepository = container.unlockLogRepository,
                    updateManager = container.updateManager,
                    appContext = container.appContext,
                )
            }
        }
    }
}

private inline fun <T> safeRead(defaultValue: T, block: () -> T): T =
    try {
        block()
    } catch (_: RuntimeException) {
        defaultValue
    }

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun areNotificationsEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.contains("${context.packageName}/${context.packageName}.service.UnlockAccessibilityService")
}

private fun isDeviceAdminEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val componentName = ComponentName(context, FocusDeviceAdminReceiver::class.java)
    return manager.isAdminActive(componentName)
}

private fun appVersionName(context: Context): String {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return info.versionName ?: "1.0"
}

private fun hasMissingSetupItems(context: Context): Boolean {
    return !isBatteryOptimizationIgnored(context) ||
        !areNotificationsEnabled(context) ||
        !isAccessibilityEnabled(context) ||
        !isDeviceAdminEnabled(context)
}

private fun Throwable.readableMessage(): String = localizedMessage ?: javaClass.simpleName

data class MainUiState(
    val friction: Int = 40,
    val hardModeEnabled: Boolean = false,
    val hideLockOutcomes: Boolean = false,
    val scheduleWindow: ScheduleWindow = ScheduleWindow(
        isEnabled = false,
        startMinutes = 8 * 60,
        endMinutes = 22 * 60,
    ),
    val isBatteryOptimizationIgnored: Boolean = false,
    val areNotificationsEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isDeviceAdminEnabled: Boolean = false,
    val appVersionName: String = "1.0",
    val hasCompletedOnboarding: Boolean = false,
    val shouldShowPermissionSetup: Boolean = true,
    val excludedPackages: Set<String> = emptySet(),
    val appTargetSelections: Map<PresetTargetCategory, AppTargetSelection?> = emptyMap(),
    val launchableApps: List<LaunchableApp> = emptyList(),
    val backgroundUpdateCheckEnabled: Boolean = true,
)

data class UpdateUiState(
    val isChecking: Boolean = false,
    val isInstalling: Boolean = false,
    val availableUpdate: AppUpdate? = null,
    val message: String? = null,
)

data class HistoryUiState(
    val totalUnlocks: Int,
    val continuedUnlocks: Int,
    val keptLockedUnlocks: Int,
    val dailyCounts: List<DailyUnlockCount>,
    val logs: List<UnlockLogEntry>,
) {
    companion object {
        val EMPTY = HistoryUiState(
            totalUnlocks = 0,
            continuedUnlocks = 0,
            keptLockedUnlocks = 0,
            dailyCounts = emptyList(),
            logs = emptyList(),
        )
    }
}
