package opb.myniceapp.dint.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.data.AppTargetSelection
import opb.myniceapp.dint.data.PresetTargetCategory
import opb.myniceapp.dint.ui.common.ActionRow
import opb.myniceapp.dint.ui.common.SettingsGroup
import opb.myniceapp.dint.ui.main.MainUiState
import opb.myniceapp.dint.ui.main.UpdateUiState
import opb.myniceapp.dint.ui.updates.AppUpdateSection

@Composable
fun MoreTab(
    uiState: MainUiState,
    frictionLabel: String,
    onFrictionChanged: (Int) -> Unit,
    onScheduleEnabledChanged: (Boolean) -> Unit,
    onRequestTimePicker: (ScheduleTimeField, Int) -> Unit,
    onHardModeChanged: (Boolean) -> Unit,
    onHiddenOutcomesChanged: (Boolean) -> Unit,
    onRequestDisableBatteryOptimization: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDeviceAdminSettings: () -> Unit,
    onLockNow: () -> Unit,
    onPreviewIntervention: () -> Unit,
    updateUiState: UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onBackgroundUpdateCheckToggled: (Boolean) -> Unit,
    onExcludedAppAdded: (String) -> Unit,
    onExcludedAppRemoved: (String) -> Unit,
    onAppTargetSelected: (PresetTargetCategory, AppTargetSelection) -> Unit,
    onAppTargetCleared: (PresetTargetCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SettingsGroup(
                title = stringResource(R.string.more_settings),
                icon = Icons.Outlined.Tune,
                content = {
                    PauseStrengthRow(
                        friction = uiState.friction,
                        frictionLabel = frictionLabel,
                        onFrictionChanged = onFrictionChanged,
                    )
                    HorizontalDivider()
                    ScheduleSection(
                        isEnabled = uiState.scheduleWindow.isEnabled,
                        startMinutes = uiState.scheduleWindow.startMinutes,
                        endMinutes = uiState.scheduleWindow.endMinutes,
                        onEnabledChanged = onScheduleEnabledChanged,
                        onRequestTimePicker = onRequestTimePicker,
                    )
                    HorizontalDivider()
                    HardModeSection(
                        isEnabled = uiState.hardModeEnabled,
                        onEnabledChanged = onHardModeChanged
                    )
                    HorizontalDivider()
                    HiddenOutcomeSection(
                        isEnabled = uiState.hideLockOutcomes,
                        onEnabledChanged = onHiddenOutcomesChanged
                    )
                    HorizontalDivider()
                    ActionRow(
                        title = stringResource(R.string.preview_intervention),
                        subtitle = stringResource(R.string.preview_intervention_description),
                        buttonLabel = stringResource(R.string.preview_intervention),
                        onClick = onPreviewIntervention,
                        icon = Icons.Outlined.Visibility,
                    )
                }
            )
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.app_targets_title),
                icon = Icons.Outlined.Apps,
                content = {
                    AppLaunchTargetsSection(
                        selections = uiState.appTargetSelections,
                        launchableApps = uiState.launchableApps,
                        onSelect = onAppTargetSelected,
                        onClear = onAppTargetCleared,
                    )
                }
            )
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.excluded_apps_title),
                icon = Icons.Outlined.PlaylistRemove,
                content = {
                    ExcludedAppsSection(
                        excludedPackages = uiState.excludedPackages,
                        launchableApps = uiState.launchableApps,
                        onAdd = onExcludedAppAdded,
                        onRemove = onExcludedAppRemoved,
                    )
                }
            )
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.updates_title),
                icon = Icons.Outlined.SystemUpdate,
                content = {
                    AppUpdateSection(
                        state = updateUiState,
                        backgroundCheckEnabled = uiState.backgroundUpdateCheckEnabled,
                        onCheckForUpdate = onCheckForUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onOpenInstallSettings = onOpenInstallSettings,
                        onBackgroundCheckToggled = onBackgroundUpdateCheckToggled,
                    )
                }
            )
        }
        if (uiState.shouldShowPermissionSetup || uiState.isDeviceAdminEnabled) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.more_permissions),
                    icon = Icons.Outlined.Shield,
                    content = {
                        PermissionSetupSection(
                            isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                            onRequestDisableBatteryOptimization = onRequestDisableBatteryOptimization,
                            areNotificationsEnabled = uiState.areNotificationsEnabled,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            isDeviceAdminEnabled = uiState.isDeviceAdminEnabled,
                            onOpenDeviceAdminSettings = onOpenDeviceAdminSettings,
                            onLockNow = onLockNow,
                        )
                    }
                )
            }
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.about_title),
                icon = Icons.Outlined.Info,
                content = {
                    AboutCard(appVersionName = uiState.appVersionName)
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
                    stringResource(R.string.about_name),
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = { Text(stringResource(R.string.about_full_name)) },
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
            headlineContent = { Text(stringResource(R.string.about_author_label)) },
            supportingContent = { Text(stringResource(R.string.about_author_value)) }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.banking_warning_title)) },
            supportingContent = { Text(stringResource(R.string.banking_warning_body)) }
        )
    }
}
