package opb.myniceapp.dint.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.ui.common.SettingsGroup
import opb.myniceapp.dint.ui.common.SubPageHeader

@Composable
fun SettingsPermissionsScreen(
    isBatteryOptimizationIgnored: Boolean,
    onRequestDisableBatteryOptimization: () -> Unit,
    areNotificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    isAccessibilityEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    isDeviceAdminEnabled: Boolean,
    onOpenDeviceAdminSettings: () -> Unit,
    onLockNow: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SubPageHeader(title = stringResource(R.string.more_permissions), onBack = onBack)
        }
        item {
            SettingsGroup(
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
}
