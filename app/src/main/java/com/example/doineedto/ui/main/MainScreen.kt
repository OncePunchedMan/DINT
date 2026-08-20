package com.example.doineedto.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doineedto.R
import com.example.doineedto.ui.history.FullHistoryScreen
import com.example.doineedto.ui.home.HomeTab
import com.example.doineedto.ui.onboarding.OnboardingIntroScreen
import com.example.doineedto.ui.onboarding.OnboardingPermissionsScreen
import com.example.doineedto.ui.settings.MoreTab
import com.example.doineedto.ui.settings.ScheduleTimeField
import com.example.doineedto.ui.settings.frictionDescription

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
    var showFullHistory by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val frictionLabel = remember(uiState.friction) { frictionDescription(uiState.friction) }

    if (!uiState.hasCompletedOnboarding) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            when (onboardingStep) {
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
        return
    }

    if (showFullHistory) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            FullHistoryScreen(onBack = { showFullHistory = false })
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
                    label = { Text(stringResource(R.string.nav_home)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {},
                    label = { Text(stringResource(R.string.nav_more)) }
                )
            }
        }
    ) { innerPadding ->
        if (selectedTab == 0) {
            HomeTab(
                historyState = historyState,
                onViewFullHistory = { showFullHistory = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            )
        } else {
            MoreTab(
                uiState = uiState,
                frictionLabel = frictionLabel,
                onFrictionChanged = viewModel::onFrictionChanged,
                onScheduleEnabledChanged = viewModel::onScheduleEnabledChanged,
                onRequestTimePicker = onRequestTimePicker,
                onHardModeChanged = viewModel::onHardModeChanged,
                onHiddenOutcomesChanged = viewModel::onHideLockOutcomesChanged,
                onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                onLockNow = onLockNow,
                onPreviewIntervention = onPreviewIntervention,
                updateUiState = updateState,
                onCheckForUpdate = viewModel::checkForUpdate,
                onInstallUpdate = viewModel::installUpdate,
                onOpenInstallSettings = onOpenInstallSettings,
                onBackgroundUpdateCheckToggled = viewModel::onBackgroundUpdateCheckToggled,
                onExcludedAppAdded = viewModel::onExcludedAppAdded,
                onExcludedAppRemoved = viewModel::onExcludedAppRemoved,
                onAppTargetSelected = viewModel::onAppTargetSelected,
                onAppTargetCleared = viewModel::onAppTargetCleared,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            )
        }
    }
}
