package opb.myniceapp.dint.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.ui.common.ActionRow

@Composable
fun HardModeSection(
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.hard_mode_title)) },
        supportingContent = {
            Text(
                if (isEnabled) {
                    stringResource(R.string.hard_mode_on)
                } else {
                    stringResource(R.string.hard_mode_description)
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
fun HiddenOutcomeSection(
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.hidden_outcomes_title)) },
        supportingContent = {
            Text(
                if (isEnabled) {
                    stringResource(R.string.hidden_outcomes_on)
                } else {
                    stringResource(R.string.hidden_outcomes_description)
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
fun PermissionSetupSection(
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
                title = stringResource(R.string.disable_battery_optimization),
                subtitle = stringResource(R.string.setup_battery_description),
                buttonLabel = stringResource(R.string.open_settings),
                onClick = onRequestDisableBatteryOptimization,
            )
            hasPreviousItem = true
        } else if (!showLockAction) {
            ActionRow(
                title = stringResource(R.string.disable_battery_optimization),
                subtitle = stringResource(R.string.battery_optimization_disabled),
                buttonLabel = stringResource(R.string.granted_label),
                onClick = {},
                enabled = false,
            )
            hasPreviousItem = true
        }
        if (!areNotificationsEnabled) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResource(R.string.enable_notifications),
                subtitle = stringResource(R.string.setup_notifications_description),
                buttonLabel = stringResource(R.string.open_settings),
                onClick = onRequestNotificationPermission,
            )
            hasPreviousItem = true
        } else if (!showLockAction) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResource(R.string.enable_notifications),
                subtitle = stringResource(R.string.notifications_enabled),
                buttonLabel = stringResource(R.string.granted_label),
                onClick = {},
                enabled = false,
            )
            hasPreviousItem = true
        }
        if (!isAccessibilityEnabled) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResource(R.string.enable_accessibility),
                subtitle = stringResource(R.string.enable_accessibility_description),
                buttonLabel = stringResource(R.string.open_settings),
                onClick = onOpenAccessibilitySettings,
            )
            hasPreviousItem = true
        } else if (!showLockAction) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResource(R.string.enable_accessibility),
                subtitle = stringResource(R.string.granted_accessibility_description),
                buttonLabel = stringResource(R.string.granted_label),
                onClick = {},
                enabled = false,
            )
            hasPreviousItem = true
        }
        if (!isDeviceAdminEnabled) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResource(R.string.enable_device_admin),
                subtitle = stringResource(R.string.enable_device_admin_description),
                buttonLabel = stringResource(R.string.open_settings),
                onClick = onOpenDeviceAdminSettings,
            )
            hasPreviousItem = true
        } else if (!showLockAction) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResource(R.string.enable_device_admin),
                subtitle = stringResource(R.string.device_admin_enabled),
                buttonLabel = stringResource(R.string.granted_label),
                onClick = {},
                enabled = false,
            )
            hasPreviousItem = true
        }
        if (isDeviceAdminEnabled && showLockAction) {
            if (hasPreviousItem) HorizontalDivider()
            ActionRow(
                title = stringResource(R.string.lock_now),
                subtitle = stringResource(R.string.lock_now_description),
                buttonLabel = stringResource(R.string.lock_now),
                onClick = onLockNow,
            )
        }
    }
}

@Composable
fun PauseStrengthRow(
    friction: Int,
    frictionLabel: String,
    onFrictionChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.intent_pause_title)) },
            supportingContent = { Text(frictionLabel) }
        )
        Slider(
            value = friction.toFloat(),
            onValueChange = { onFrictionChanged(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        )
    }
}

fun frictionDescription(friction: Int): String = when {
    friction < 25 -> "Light pause: a quick reflection and no waiting time."
    friction < 50 -> "Balanced pause: enough time to notice the urge. (4s)"
    friction < 75 -> "Strong pause: a longer wait before continuing. (8s)"
    else -> "Heavy pause: the phone should stay closed unless you really mean it. (12s)"
}
