package com.example.doineedto

import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.doineedto.admin.FocusDeviceAdminReceiver
import com.example.doineedto.data.AppPreferences
import com.example.doineedto.data.AppTargetSelection
import com.example.doineedto.data.AppUpdate
import com.example.doineedto.data.AppUpdateManager
import com.example.doineedto.data.DailyUnlockCount
import com.example.doineedto.data.LaunchableApp
import com.example.doineedto.data.PresetTargetCategory
import com.example.doineedto.data.ScheduleWindow
import com.example.doineedto.data.UnlockLogEntry
import com.example.doineedto.data.queryLaunchableApps
import com.example.doineedto.data.UpdateCheckResult
import com.example.doineedto.service.UnlockAccessibilityService
import com.example.doineedto.ui.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var preferences: AppPreferences
    private lateinit var updateManager: AppUpdateManager
    private var uiState by mutableStateOf(MainUiState())
    private var updateUiState by mutableStateOf(UpdateUiState())

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshUiState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)
        updateManager = AppUpdateManager(this)
        refreshUiState()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        currentFriction = uiState.friction,
                        onFrictionChanged = preferences::setFriction,
                        hardModeEnabled = uiState.hardModeEnabled,
                        onHardModeChanged = preferences::setHardModeEnabled,
                        hideLockOutcomes = uiState.hideLockOutcomes,
                        onHideLockOutcomesChanged = preferences::setHideLockOutcomes,
                        scheduleWindow = uiState.scheduleWindow,
                        onScheduleEnabledChanged = preferences::setScheduleEnabled,
                        onScheduleStartChanged = preferences::setScheduleStartMinutes,
                        onScheduleEndChanged = preferences::setScheduleEndMinutes,
                        isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                        onRequestDisableBatteryOptimization = { requestDisableBatteryOptimization() },
                        areNotificationsEnabled = uiState.areNotificationsEnabled,
                        onRequestNotificationPermission = { requestNotificationPermission() },
                        appVersionName = uiState.appVersionName,
                        isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                        hasCompletedOnboarding = uiState.hasCompletedOnboarding,
                        onCompleteOnboarding = {
                            preferences.markOnboardingCompleted()
                            refreshUiState()
                        },
                        shouldShowPermissionSetup = uiState.shouldShowPermissionSetup,
                        onOpenAccessibilitySettings = { openAccessibilitySettings() },
                        onOpenDeviceAdminSettings = { requestDeviceAdmin() },
                        onLockNow = { lockNow() },
                        onPreviewIntervention = {
                            startActivity(InterventionActivity.createIntent(this))
                        },
                        isDeviceAdminEnabled = uiState.isDeviceAdminEnabled,
                        refreshToken = uiState.refreshToken,
                        updateUiState = updateUiState,
                        onCheckForUpdate = { checkForUpdate() },
                        onInstallUpdate = { installUpdate() },
                        onOpenInstallSettings = { updateManager.openUnknownAppSourcesSettings() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUiState()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestDisableBatteryOptimization() {
        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }

        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        try {
            when {
                directIntent.resolveActivity(packageManager) != null -> startActivity(directIntent)
                fallbackIntent.resolveActivity(packageManager) != null -> startActivity(fallbackIntent)
                else -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            )
        }
    }

    private fun appVersionName(): String {
        val info = packageManager.getPackageInfo(packageName, 0)
        return info.versionName ?: "1.0"
    }

    private fun checkForUpdate() {
        updateUiState = updateUiState.copy(isChecking = true, message = null)

        lifecycleScope.launch {
            updateUiState = try {
                when (val result = withContext(Dispatchers.IO) { updateManager.checkForUpdate() }) {
                    is UpdateCheckResult.Available -> UpdateUiState(
                        availableUpdate = result.update,
                        message = getString(R.string.update_available_message, result.update.versionName),
                    )

                    UpdateCheckResult.UpToDate -> UpdateUiState(
                        message = getString(R.string.update_up_to_date),
                    )
                }
            } catch (error: Exception) {
                UpdateUiState(message = getString(R.string.update_check_failed, error.readableMessage()))
            }
        }
    }

    private fun installUpdate() {
        val update = updateUiState.availableUpdate ?: return

        if (!updateManager.canRequestPackageInstalls()) {
            updateUiState = updateUiState.copy(
                message = getString(R.string.update_install_permission_needed),
            )
            updateManager.openUnknownAppSourcesSettings()
            return
        }

        updateUiState = updateUiState.copy(isInstalling = true, message = getString(R.string.update_downloading))

        lifecycleScope.launch {
            try {
                val apkFile = withContext(Dispatchers.IO) {
                    updateManager.downloadUpdateApk(update)
                }
                updateManager.promptInstall(apkFile)
                updateUiState = updateUiState.copy(
                    isInstalling = false,
                    message = getString(R.string.update_install_prompt_opened),
                )
            } catch (error: Exception) {
                updateUiState = updateUiState.copy(
                    isInstalling = false,
                    message = getString(R.string.update_install_failed, error.readableMessage()),
                )
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains("$packageName/${packageName}.service.UnlockAccessibilityService")
    }

    private fun requestDeviceAdmin() {
        val componentName = ComponentName(this, FocusDeviceAdminReceiver::class.java)
        startActivity(
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_explanation)
                )
            }
        )
    }

    private fun isDeviceAdminEnabled(): Boolean {
        val manager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, FocusDeviceAdminReceiver::class.java)
        return manager.isAdminActive(componentName)
    }

    private fun lockNow() {
        if (UnlockAccessibilityService.tryLockScreen()) {
            return
        }

        val manager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, FocusDeviceAdminReceiver::class.java)
        if (manager.isAdminActive(componentName)) {
            manager.lockNow()
        }
    }

    private fun hasMissingSetupItems(): Boolean {
        return !isBatteryOptimizationIgnored() ||
            !areNotificationsEnabled() ||
            !isAccessibilityEnabled() ||
            !isDeviceAdminEnabled()
    }

    private fun refreshUiState() {
        if (!::preferences.isInitialized) return

        val hasCompletedOnboarding = preferences.hasCompletedOnboarding()
        uiState = MainUiState(
            friction = preferences.getFriction(),
            hardModeEnabled = preferences.isHardModeEnabled(),
            hideLockOutcomes = preferences.shouldHideLockOutcomes(),
            scheduleWindow = preferences.getScheduleWindow(),
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(),
            areNotificationsEnabled = areNotificationsEnabled(),
            appVersionName = appVersionName(),
            isAccessibilityEnabled = isAccessibilityEnabled(),
            hasCompletedOnboarding = hasCompletedOnboarding,
            shouldShowPermissionSetup = !hasCompletedOnboarding || hasMissingSetupItems(),
            isDeviceAdminEnabled = isDeviceAdminEnabled(),
            refreshToken = uiState.refreshToken + 1,
        )
    }
}

@Composable
private fun MainScreen(
    currentFriction: Int,
    onFrictionChanged: (Int) -> Unit,
    hardModeEnabled: Boolean,
    onHardModeChanged: (Boolean) -> Unit,
    hideLockOutcomes: Boolean,
    onHideLockOutcomesChanged: (Boolean) -> Unit,
    scheduleWindow: ScheduleWindow,
    onScheduleEnabledChanged: (Boolean) -> Unit,
    onScheduleStartChanged: (Int) -> Unit,
    onScheduleEndChanged: (Int) -> Unit,
    isBatteryOptimizationIgnored: Boolean,
    onRequestDisableBatteryOptimization: () -> Unit,
    areNotificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    appVersionName: String,
    isAccessibilityEnabled: Boolean,
    hasCompletedOnboarding: Boolean,
    onCompleteOnboarding: () -> Unit,
    shouldShowPermissionSetup: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDeviceAdminSettings: () -> Unit,
    onLockNow: () -> Unit,
    onPreviewIntervention: () -> Unit,
    isDeviceAdminEnabled: Boolean,
    refreshToken: Int,
    updateUiState: UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenInstallSettings: () -> Unit,
) {
    var onboardingCompleted by rememberSaveable(hasCompletedOnboarding) {
        androidx.compose.runtime.mutableStateOf(hasCompletedOnboarding)
    }
    var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    var friction by rememberSaveable(currentFriction) { mutableIntStateOf(currentFriction) }
    var hardMode by rememberSaveable(hardModeEnabled) { androidx.compose.runtime.mutableStateOf(hardModeEnabled) }
    var hiddenOutcomes by rememberSaveable(hideLockOutcomes) { androidx.compose.runtime.mutableStateOf(hideLockOutcomes) }
    var scheduleEnabled by rememberSaveable(scheduleWindow.isEnabled) {
        androidx.compose.runtime.mutableStateOf(scheduleWindow.isEnabled)
    }
    var scheduleStart by rememberSaveable(scheduleWindow.startMinutes) {
        mutableIntStateOf(scheduleWindow.startMinutes)
    }
    var scheduleEnd by rememberSaveable(scheduleWindow.endMinutes) {
        mutableIntStateOf(scheduleWindow.endMinutes)
    }
    val frictionLabel = remember(friction) { frictionDescription(friction) }
    val historyState = rememberHistoryState(refreshToken)

    if (!onboardingCompleted) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            when (onboardingStep) {
                0 -> OnboardingIntroScreen(
                    onContinue = { onboardingStep = 1 }
                )
                else -> OnboardingPermissionsScreen(
                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                    onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                    areNotificationsEnabled = areNotificationsEnabled,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    isDeviceAdminEnabled = isDeviceAdminEnabled,
                    onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                    onFinish = {
                        onCompleteOnboarding()
                        onboardingCompleted = true
                    },
                )
            }
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {},
                    label = { Text(stringResourceSafe(R.string.nav_home)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {},
                    label = { Text(stringResourceSafe(R.string.nav_more)) }
                )
            }
        }
    ) { innerPadding ->
        if (selectedTab == 0) {
            HomeTab(
                historyState = historyState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            )
        } else {
            MoreTab(
                appVersionName = appVersionName,
                friction = friction,
                frictionLabel = frictionLabel,
                onFrictionChanged = {
                    friction = it
                    onFrictionChanged(it)
                },
                scheduleEnabled = scheduleEnabled,
                scheduleStart = scheduleStart,
                scheduleEnd = scheduleEnd,
                onScheduleEnabledChanged = {
                    scheduleEnabled = it
                    onScheduleEnabledChanged(it)
                },
                onScheduleStartChanged = {
                    scheduleStart = it
                    onScheduleStartChanged(it)
                },
                onScheduleEndChanged = {
                    scheduleEnd = it
                    onScheduleEndChanged(it)
                },
                hardMode = hardMode,
                onHardModeChanged = {
                    hardMode = it
                    onHardModeChanged(it)
                },
                hiddenOutcomes = hiddenOutcomes,
                onHiddenOutcomesChanged = {
                    hiddenOutcomes = it
                    onHideLockOutcomesChanged(it)
                },
                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                areNotificationsEnabled = areNotificationsEnabled,
                onRequestNotificationPermission = onRequestNotificationPermission,
                isAccessibilityEnabled = isAccessibilityEnabled,
                shouldShowPermissionSetup = shouldShowPermissionSetup,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                isDeviceAdminEnabled = isDeviceAdminEnabled,
                onLockNow = onLockNow,
                onPreviewIntervention = onPreviewIntervention,
                refreshToken = refreshToken,
                updateUiState = updateUiState,
                onCheckForUpdate = onCheckForUpdate,
                onInstallUpdate = onInstallUpdate,
                onOpenInstallSettings = onOpenInstallSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            )
        }
    }
}

@Composable
private fun OnboardingIntroScreen(
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            HeroCard(
                title = stringResourceSafe(R.string.onboarding_title),
                body = stringResourceSafe(R.string.onboarding_intro),
            )
            SettingsGroup(
                title = stringResourceSafe(R.string.onboarding_how_title),
                content = {
                    ListItem(
                        headlineContent = { Text(stringResourceSafe(R.string.onboarding_step_one_title)) },
                        supportingContent = { Text(stringResourceSafe(R.string.onboarding_step_one_body)) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResourceSafe(R.string.onboarding_step_two_title)) },
                        supportingContent = { Text(stringResourceSafe(R.string.onboarding_step_two_body)) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResourceSafe(R.string.onboarding_step_three_title)) },
                        supportingContent = { Text(stringResourceSafe(R.string.onboarding_step_three_body)) }
                    )
                }
            )
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResourceSafe(R.string.onboarding_continue))
        }
    }
}

@Composable
private fun OnboardingPermissionsScreen(
    isBatteryOptimizationIgnored: Boolean,
    onRequestDisableBatteryOptimization: () -> Unit,
    areNotificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    isAccessibilityEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    isDeviceAdminEnabled: Boolean,
    onOpenDeviceAdminSettings: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            HeroCard(
                title = stringResourceSafe(R.string.onboarding_permissions_title),
                body = stringResourceSafe(R.string.onboarding_permissions_body),
                compact = true,
            )
            SettingsGroup(
                title = stringResourceSafe(R.string.more_permissions),
                content = {
                    PermissionSetupSection(
                        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                        onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                        areNotificationsEnabled = areNotificationsEnabled,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        isDeviceAdminEnabled = isDeviceAdminEnabled,
                        onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                        onLockNow = {},
                        showLockAction = false,
                    )
                }
            )
        }
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResourceSafe(R.string.onboarding_finish))
        }
    }
}

@Composable
private fun HomeTab(
    historyState: HistoryState,
    modifier: Modifier = Modifier,
) {
    var showAllHistory by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val visibleLogs = if (showAllHistory) historyState.logs else historyState.logs.take(5)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = stringResourceSafe(R.string.home_title),
                body = stringResourceSafe(R.string.main_intro),
            )
        }
        item {
            SettingsGroup(
                title = stringResourceSafe(R.string.home_why_title),
                content = {
                    ListItem(
                        headlineContent = { Text(stringResourceSafe(R.string.home_why_title)) },
                        supportingContent = { Text(stringResourceSafe(R.string.home_why_body)) }
                    )
                }
            )
        }
        item {
            SettingsGroup(
                title = stringResourceSafe(R.string.emergency_title),
                content = {
                    ListItem(
                        headlineContent = { Text(stringResourceSafe(R.string.emergency_title)) },
                        supportingContent = { Text(stringResourceSafe(R.string.emergency_body)) }
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
                text = stringResourceSafe(R.string.history_title),
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
                    Text(stringResourceSafe(R.string.show_more_history))
                }
            }
        }
        item {
            Text(
                text = stringResourceSafe(R.string.platform_note),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MoreTab(
    appVersionName: String,
    friction: Int,
    frictionLabel: String,
    onFrictionChanged: (Int) -> Unit,
    scheduleEnabled: Boolean,
    scheduleStart: Int,
    scheduleEnd: Int,
    onScheduleEnabledChanged: (Boolean) -> Unit,
    onScheduleStartChanged: (Int) -> Unit,
    onScheduleEndChanged: (Int) -> Unit,
    hardMode: Boolean,
    onHardModeChanged: (Boolean) -> Unit,
    hiddenOutcomes: Boolean,
    onHiddenOutcomesChanged: (Boolean) -> Unit,
    isBatteryOptimizationIgnored: Boolean,
    onRequestDisableBatteryOptimization: () -> Unit,
    areNotificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    isAccessibilityEnabled: Boolean,
    shouldShowPermissionSetup: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDeviceAdminSettings: () -> Unit,
    isDeviceAdminEnabled: Boolean,
    onLockNow: () -> Unit,
    onPreviewIntervention: () -> Unit,
    refreshToken: Int,
    updateUiState: UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SettingsGroup(
                title = stringResourceSafe(R.string.more_settings),
                content = {
                    PauseStrengthRow(
                        friction = friction,
                        frictionLabel = frictionLabel,
                        onFrictionChanged = onFrictionChanged,
                    )
                    HorizontalDivider()
                    ScheduleSection(
                        isEnabled = scheduleEnabled,
                        startMinutes = scheduleStart,
                        endMinutes = scheduleEnd,
                        onEnabledChanged = onScheduleEnabledChanged,
                        onStartChanged = onScheduleStartChanged,
                        onEndChanged = onScheduleEndChanged,
                    )
                    HorizontalDivider()
                    HardModeSection(
                        isEnabled = hardMode,
                        onEnabledChanged = onHardModeChanged
                    )
                    HorizontalDivider()
                    HiddenOutcomeSection(
                        isEnabled = hiddenOutcomes,
                        onEnabledChanged = onHiddenOutcomesChanged
                    )
                    HorizontalDivider()
                    ActionRow(
                        title = stringResourceSafe(R.string.preview_intervention),
                        subtitle = stringResourceSafe(R.string.preview_intervention_description),
                        buttonLabel = stringResourceSafe(R.string.preview_intervention),
                        onClick = onPreviewIntervention,
                    )
                }
            )
        }
        item {
            SettingsGroup(
                title = stringResourceSafe(R.string.app_targets_title),
                content = {
                    AppLaunchTargetsSection(refreshToken = refreshToken)
                }
            )
        }
        item {
            SettingsGroup(
                title = stringResourceSafe(R.string.updates_title),
                content = {
                    AppUpdateSection(
                        state = updateUiState,
                        onCheckForUpdate = onCheckForUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onOpenInstallSettings = onOpenInstallSettings,
                    )
                }
            )
        }
        if (shouldShowPermissionSetup || isDeviceAdminEnabled) {
            item {
                SettingsGroup(
                    title = stringResourceSafe(R.string.more_permissions),
                    content = {
                        PermissionSetupSection(
                            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                            onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                            areNotificationsEnabled = areNotificationsEnabled,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            isDeviceAdminEnabled = isDeviceAdminEnabled,
                            onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                            onLockNow = onLockNow,
                        )
                    }
                )
            }
        }
        item {
            SettingsGroup(
                title = stringResourceSafe(R.string.about_title),
                content = {
                    AboutCard(appVersionName = appVersionName)
                }
            )
        }
    }
}

@Composable
private fun AboutCard(appVersionName: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        ListItem(
            headlineContent = {
                Text(
                    stringResourceSafe(R.string.about_name),
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = { Text(stringResourceSafe(R.string.about_full_name)) },
            trailingContent = {
                Text(
                    "v$appVersionName",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResourceSafe(R.string.about_author_label)) },
            supportingContent = { Text(stringResourceSafe(R.string.about_author_value)) }
        )
    }
}

@Composable
private fun AppLaunchTargetsSection(
    refreshToken: Int,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { AppPreferences(context) }
    var chooserCategory by rememberSaveable { mutableStateOf<PresetTargetCategory?>(null) }
    var localRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    val launchableApps = remember(refreshToken, localRefreshToken) { queryLaunchableApps(context) }

    Column {
        ListItem(
            headlineContent = { Text(stringResourceSafe(R.string.app_targets_title)) },
            supportingContent = { Text(stringResourceSafe(R.string.app_targets_description)) }
        )

        PresetTargetCategory.entries.forEachIndexed { index, category ->
            if (index > 0) {
                HorizontalDivider()
            }

            val selection = preferences.getAppTargetSelection(category)

            AppTargetRow(
                category = category,
                selection = selection,
                onChoose = { chooserCategory = category },
                onClear = {
                    preferences.clearAppTargetSelection(category)
                    localRefreshToken += 1
                }
            )
        }
    }

    if (chooserCategory != null) {
        AppTargetChooserDialog(
            category = chooserCategory!!,
            apps = launchableApps,
            onDismiss = { chooserCategory = null },
            onSelect = { app ->
                preferences.setAppTargetSelection(
                    chooserCategory!!,
                    AppTargetSelection(
                        packageName = app.packageName,
                        label = app.label,
                    )
                )
                localRefreshToken += 1
                chooserCategory = null
            }
        )
    }
}

@Composable
private fun AppTargetRow(
    category: PresetTargetCategory,
    selection: AppTargetSelection?,
    onChoose: () -> Unit,
    onClear: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(category.title) },
        supportingContent = {
            Text(selection?.label ?: appTargetDescription(category))
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selection != null) {
                    TextButton(onClick = onClear) {
                        Text(stringResourceSafe(R.string.clear_app))
                    }
                }
                Button(
                    onClick = onChoose,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        stringResourceSafe(
                            if (selection == null) R.string.choose_app else R.string.change_app
                        )
                    )
                }
            }
        }
    )
}

@Composable
private fun AppTargetChooserDialog(
    category: PresetTargetCategory,
    apps: List<LaunchableApp>,
    onDismiss: () -> Unit,
    onSelect: (LaunchableApp) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = searchQuery.trim()
    val filteredApps = remember(apps, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            apps
        } else {
            val query = normalizedQuery.lowercase()
            apps.filter { app ->
                app.label.lowercase().contains(query) || app.packageName.lowercase().contains(query)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stringResourceSafe(R.string.choose_app_dialog_title)}: ${category.title}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResourceSafe(R.string.search_apps)) },
                )

                if (filteredApps.isEmpty()) {
                    Text(
                        text = stringResourceSafe(R.string.no_apps_match_search),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    filteredApps.forEach { app ->
                        TextButton(
                            onClick = { onSelect(app) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = app.label,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResourceSafe(R.string.dismiss))
            }
        }
    )
}

@Composable
private fun appTargetDescription(category: PresetTargetCategory): String = when (category) {
    PresetTargetCategory.MEMES -> stringResourceSafe(R.string.app_target_memes_description)
    PresetTargetCategory.SOCIAL -> stringResourceSafe(R.string.app_target_social_description)
    PresetTargetCategory.NEWS -> stringResourceSafe(R.string.app_target_news_description)
    PresetTargetCategory.VIDEO -> stringResourceSafe(R.string.app_target_video_description)
    PresetTargetCategory.MUSIC -> stringResourceSafe(R.string.app_target_music_description)
    PresetTargetCategory.SHOPPING -> stringResourceSafe(R.string.app_target_shopping_description)
    PresetTargetCategory.GAMES -> stringResourceSafe(R.string.app_target_games_description)
}

@Composable
private fun AppUpdateSection(
    state: UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenInstallSettings: () -> Unit,
) {
    Column {
        ListItem(
            headlineContent = { Text(stringResourceSafe(R.string.updates_title)) },
            supportingContent = {
                Text(state.message ?: stringResourceSafe(R.string.updates_description))
            },
            trailingContent = {
                Button(
                    onClick = onCheckForUpdate,
                    enabled = !state.isChecking && !state.isInstalling,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        stringResourceSafe(
                            if (state.isChecking) R.string.update_checking else R.string.check_for_update
                        )
                    )
                }
            }
        )

        state.availableUpdate?.let { update ->
            HorizontalDivider()
            ListItem(
                headlineContent = {
                    Text(
                        stringResourceSafe(R.string.update_available_title, update.versionName),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                supportingContent = {
                    Text(stringResourceSafe(R.string.update_available_asset, update.apkName))
                },
                trailingContent = {
                    Button(
                        onClick = onInstallUpdate,
                        enabled = !state.isChecking && !state.isInstalling,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            stringResourceSafe(
                                if (state.isInstalling) R.string.update_downloading_short else R.string.install_update
                            )
                        )
                    }
                }
            )
            HorizontalDivider()
            ActionRow(
                title = stringResourceSafe(R.string.update_install_permission_title),
                subtitle = stringResourceSafe(R.string.update_install_permission_description),
                buttonLabel = stringResourceSafe(R.string.open_settings),
                onClick = onOpenInstallSettings,
            )
        }
    }
}

@Composable
private fun HardModeSection(
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResourceSafe(R.string.hard_mode_title)) },
        supportingContent = {
            Text(
                if (isEnabled) {
                    stringResourceSafe(R.string.hard_mode_on)
                } else {
                    stringResourceSafe(R.string.hard_mode_description)
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
}

@Composable
private fun HiddenOutcomeSection(
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResourceSafe(R.string.hidden_outcomes_title)) },
        supportingContent = {
            Text(
                if (isEnabled) {
                    stringResourceSafe(R.string.hidden_outcomes_on)
                } else {
                    stringResourceSafe(R.string.hidden_outcomes_description)
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
}

@Composable
private fun PermissionSetupSection(
    isBatteryOptimizationIgnored: Boolean,
    onRequestDisableBatteryOptimization: () -> Unit,
    areNotificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    isAccessibilityEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    isDeviceAdminEnabled: Boolean,
    onOpenDeviceAdminSettings: () -> Unit,
    onLockNow: () -> Unit,
    showLockAction: Boolean = true,
) {
    Column {
        var hasPreviousItem = false

        if (!isBatteryOptimizationIgnored) {
            ActionRow(
                title = stringResourceSafe(R.string.disable_battery_optimization),
                subtitle = stringResourceSafe(R.string.setup_battery_description),
                buttonLabel = stringResourceSafe(R.string.open_settings),
                onClick = onRequestDisableBatteryOptimization,
            )
            hasPreviousItem = true
        } else if (!showLockAction) {
            ActionRow(
                title = stringResourceSafe(R.string.disable_battery_optimization),
                subtitle = stringResourceSafe(R.string.battery_optimization_disabled),
                buttonLabel = stringResourceSafe(R.string.granted_label),
                onClick = {},
                enabled = false,
            )
            hasPreviousItem = true
        }
        if (!areNotificationsEnabled) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResourceSafe(R.string.enable_notifications),
                subtitle = stringResourceSafe(R.string.setup_notifications_description),
                buttonLabel = stringResourceSafe(R.string.open_settings),
                onClick = onRequestNotificationPermission,
            )
            hasPreviousItem = true
        } else if (!showLockAction) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResourceSafe(R.string.enable_notifications),
                subtitle = stringResourceSafe(R.string.notifications_enabled),
                buttonLabel = stringResourceSafe(R.string.granted_label),
                onClick = {},
                enabled = false,
            )
            hasPreviousItem = true
        }
        if (!isAccessibilityEnabled) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResourceSafe(R.string.enable_accessibility),
                subtitle = stringResourceSafe(R.string.enable_accessibility_description),
                buttonLabel = stringResourceSafe(R.string.open_settings),
                onClick = onOpenAccessibilitySettings,
            )
            hasPreviousItem = true
        } else if (!showLockAction) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResourceSafe(R.string.enable_accessibility),
                subtitle = stringResourceSafe(R.string.granted_accessibility_description),
                buttonLabel = stringResourceSafe(R.string.granted_label),
                onClick = {},
                enabled = false,
            )
            hasPreviousItem = true
        }
        if (!isDeviceAdminEnabled) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResourceSafe(R.string.enable_device_admin),
                subtitle = stringResourceSafe(R.string.enable_device_admin_description),
                buttonLabel = stringResourceSafe(R.string.open_settings),
                onClick = onOpenDeviceAdminSettings,
            )
            hasPreviousItem = true
        } else if (!showLockAction) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResourceSafe(R.string.enable_device_admin),
                subtitle = stringResourceSafe(R.string.device_admin_enabled),
                buttonLabel = stringResourceSafe(R.string.granted_label),
                onClick = {},
                enabled = false,
            )
            hasPreviousItem = true
        }
        if (isDeviceAdminEnabled && showLockAction) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResourceSafe(R.string.lock_now),
                subtitle = stringResourceSafe(R.string.lock_now_description),
                buttonLabel = stringResourceSafe(R.string.lock_now),
                onClick = onLockNow,
            )
        }
    }
}

@Composable
private fun PauseStrengthRow(
    friction: Int,
    frictionLabel: String,
    onFrictionChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        ListItem(
            headlineContent = { Text(stringResourceSafe(R.string.intent_pause_title)) },
            supportingContent = { Text(frictionLabel) }
        )
        Slider(
            value = friction.toFloat(),
            onValueChange = { onFrictionChanged(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    buttonLabel: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            ) {
                Text(buttonLabel)
            }
        }
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    body: String,
    compact: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 20.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            if (body.isNotBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun rememberHistoryState(refreshToken: Int): HistoryState {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { AppPreferences(context) }
    return remember(preferences, refreshToken) {
        HistoryState(
            totalUnlocks = preferences.totalUnlocks(),
            continuedUnlocks = preferences.continuedUnlocks(),
            keptLockedUnlocks = preferences.keptLockedUnlocks(),
            dailyCounts = preferences.getDailyUnlockCounts(),
            logs = preferences.getUnlockLogs().take(20),
        )
    }
}

@Composable
private fun ScheduleSection(
    isEnabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    onEnabledChanged: (Boolean) -> Unit,
    onStartChanged: (Int) -> Unit,
    onEndChanged: (Int) -> Unit,
) {
    Column {
        ListItem(
            headlineContent = { Text(stringResourceSafe(R.string.schedule_title)) },
            supportingContent = {
                Text(
                    if (isEnabled) {
                        "Active from ${formatMinutes(startMinutes)} to ${formatMinutes(endMinutes)}"
                    } else {
                        stringResourceSafe(R.string.schedule_description)
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
                label = "Start",
                minutes = startMinutes,
                onChange = onStartChanged
            )
            TimeAdjustRow(
                label = "End",
                minutes = endMinutes,
                onChange = onEndChanged
            )
        }
    }
}

@Composable
private fun TimeAdjustRow(
    label: String,
    minutes: Int,
    onChange: (Int) -> Unit,
) {
    val context = LocalContext.current

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
        Button(
            onClick = {
                val hour = minutes / 60
                val minute = minutes % 60
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        onChange(selectedHour * 60 + selectedMinute)
                    },
                    hour,
                    minute,
                    true
                ).show()
            }
        ) {
            Text("Set time")
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
    var showAllDays by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val visibleDailyCounts = if (showAllDays) dailyCounts else dailyCounts.take(3)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResourceSafe(R.string.stats_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Unlocks", totalUnlocks.toString(), Modifier.weight(1f))
            StatCard("Continued", continuedUnlocks.toString(), Modifier.weight(1f))
            StatCard("Stopped", keptLockedUnlocks.toString(), Modifier.weight(1f))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        Text(stringResourceSafe(R.string.show_more_days))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

@Composable
private fun HistoryCard(log: UnlockLogEntry) {
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
                text = if (log.action == "skipped") "" else if (log.reason.isBlank()) "No reason entered yet." else log.reason,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun actionLabel(action: String): String = when (action) {
    "continue" -> "Continued"
    "keep_locked" -> "Stopped"
    "skipped" -> "Skipped"
    else -> "Pending"
}

private data class HistoryState(
    val totalUnlocks: Int,
    val continuedUnlocks: Int,
    val keptLockedUnlocks: Int,
    val dailyCounts: List<DailyUnlockCount>,
    val logs: List<UnlockLogEntry>,
)

private data class MainUiState(
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
    val appVersionName: String = "1.0",
    val isAccessibilityEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val shouldShowPermissionSetup: Boolean = true,
    val isDeviceAdminEnabled: Boolean = false,
    val refreshToken: Int = 0,
)

private data class UpdateUiState(
    val isChecking: Boolean = false,
    val isInstalling: Boolean = false,
    val availableUpdate: AppUpdate? = null,
    val message: String? = null,
)

private fun frictionDescription(friction: Int): String = when {
    friction < 25 -> "Light pause: a quick reflection and a short wait."
    friction < 50 -> "Balanced pause: enough time to notice the urge."
    friction < 75 -> "Strong pause: a longer wait before continuing."
    else -> "Heavy pause: the phone should stay closed unless you really mean it."
}

private fun formatMinutes(minutes: Int): String {
    val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hour = normalized / 60
    val minute = normalized % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}

@Composable
private fun stringResourceSafe(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
private fun stringResourceSafe(id: Int, vararg formatArgs: Any): String =
    androidx.compose.ui.res.stringResource(id, *formatArgs)

private fun Throwable.readableMessage(): String = localizedMessage ?: javaClass.simpleName
