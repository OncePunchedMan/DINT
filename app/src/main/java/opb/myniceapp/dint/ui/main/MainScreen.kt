package opb.myniceapp.dint.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import opb.myniceapp.dint.R
import opb.myniceapp.dint.ui.history.FullHistoryScreen
import opb.myniceapp.dint.ui.home.HomeTab
import opb.myniceapp.dint.ui.onboarding.OnboardingIntroScreen
import opb.myniceapp.dint.ui.onboarding.OnboardingPermissionsScreen
import opb.myniceapp.dint.ui.settings.MoreTab
import opb.myniceapp.dint.ui.settings.ScheduleTimeField
import opb.myniceapp.dint.ui.settings.SettingsPermissionsScreen
import opb.myniceapp.dint.ui.settings.SettingsShortcutsScreen
import opb.myniceapp.dint.ui.settings.SettingsUiScreen
import opb.myniceapp.dint.ui.settings.frictionDescription

private enum class MainSubScreen { None, FullHistory, SettingsUi, SettingsShortcuts, SettingsPermissions }

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestDisableBatteryOptimization: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDeviceAdminSettings: () -> Unit,
    onLockNow: () -> Unit,
    onPreviewIntervention: () -> Unit,
    onRequestTimePicker: (ScheduleTimeField, Int) -> Unit,
    onOpenInstallSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()

    var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
    var activeSubScreen by rememberSaveable { mutableStateOf(MainSubScreen.None) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val frictionLabel = remember(uiState.friction) { frictionDescription(uiState.friction) }

    if (!uiState.hasCompletedOnboarding) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            OnboardingStepIndicator(
                currentStep = onboardingStep,
                totalSteps = 2,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            AnimatedContent(
                targetState = onboardingStep,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(300)) { width -> direction * width } togetherWith
                        slideOutHorizontally(tween(300)) { width -> -direction * width })
                },
                label = "onboarding_step",
            ) { step ->
                when (step) {
                    0 -> OnboardingIntroScreen(
                        onContinue = { onboardingStep = 1 }
                    )
                    else -> OnboardingPermissionsScreen(
                        isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                        onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                        areNotificationsEnabled = uiState.areNotificationsEnabled,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        isDeviceAdminEnabled = uiState.isDeviceAdminEnabled,
                        onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                        onFinish = { viewModel.onOnboardingCompleted() },
                    )
                }
            }
        }
        return
    }

    BackHandler(enabled = activeSubScreen != MainSubScreen.None || selectedTab != 0) {
        if (activeSubScreen != MainSubScreen.None) {
            activeSubScreen = MainSubScreen.None
        } else {
            selectedTab = 0
        }
    }

    when (activeSubScreen) {
        MainSubScreen.FullHistory -> Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            FullHistoryScreen(onBack = { activeSubScreen = MainSubScreen.None })
        }
        MainSubScreen.SettingsUi -> Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            SettingsUiScreen(
                friction = uiState.friction,
                frictionLabel = frictionLabel,
                onFrictionChanged = viewModel::onFrictionChanged,
                scheduleEnabled = uiState.scheduleWindow.isEnabled,
                scheduleStartMinutes = uiState.scheduleWindow.startMinutes,
                scheduleEndMinutes = uiState.scheduleWindow.endMinutes,
                onScheduleEnabledChanged = viewModel::onScheduleEnabledChanged,
                onRequestTimePicker = onRequestTimePicker,
                hardModeEnabled = uiState.hardModeEnabled,
                onHardModeChanged = viewModel::onHardModeChanged,
                hideLockOutcomes = uiState.hideLockOutcomes,
                onHiddenOutcomesChanged = viewModel::onHideLockOutcomesChanged,
                onPreviewIntervention = onPreviewIntervention,
                onBack = { activeSubScreen = MainSubScreen.None },
            )
        }
        MainSubScreen.SettingsShortcuts -> Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            SettingsShortcutsScreen(
                uiState = uiState,
                onExcludedAppAdded = viewModel::onExcludedAppAdded,
                onExcludedAppRemoved = viewModel::onExcludedAppRemoved,
                onAppTargetSelected = viewModel::onAppTargetSelected,
                onAppTargetCleared = viewModel::onAppTargetCleared,
                onBack = { activeSubScreen = MainSubScreen.None },
            )
        }
        MainSubScreen.SettingsPermissions -> Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            SettingsPermissionsScreen(
                isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                areNotificationsEnabled = uiState.areNotificationsEnabled,
                onRequestNotificationPermission = onRequestNotificationPermission,
                isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                isDeviceAdminEnabled = uiState.isDeviceAdminEnabled,
                onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                isInstallPermissionGranted = uiState.canRequestPackageInstalls,
                onOpenInstallSettings = onOpenInstallSettings,
                onLockNow = onLockNow,
                onBack = { activeSubScreen = MainSubScreen.None },
            )
        }
        MainSubScreen.None -> Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    tonalElevation = 10.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.nav_home)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.nav_more)) }
                    )
                }
            }
        ) { innerPadding ->
            Crossfade(targetState = selectedTab, label = "main_tab") { tab ->
                if (tab == 0) {
                    HomeTab(
                        historyState = historyState,
                        onViewFullHistory = { activeSubScreen = MainSubScreen.FullHistory },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp)
                    )
                } else {
                    MoreTab(
                        appVersionName = uiState.appVersionName,
                        shouldShowPermissionSetup = uiState.shouldShowPermissionSetup,
                        updateUiState = updateState,
                        backgroundUpdateCheckEnabled = uiState.backgroundUpdateCheckEnabled,
                        onCheckForUpdate = viewModel::checkForUpdate,
                        onInstallUpdate = viewModel::installUpdate,
                        onBackgroundUpdateCheckToggled = viewModel::onBackgroundUpdateCheckToggled,
                        onNavigateToUi = { activeSubScreen = MainSubScreen.SettingsUi },
                        onNavigateToShortcuts = { activeSubScreen = MainSubScreen.SettingsShortcuts },
                        onNavigateToPermissions = { activeSubScreen = MainSubScreen.SettingsPermissions },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { step ->
            val active = step == currentStep
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (active) 24.dp else 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}
