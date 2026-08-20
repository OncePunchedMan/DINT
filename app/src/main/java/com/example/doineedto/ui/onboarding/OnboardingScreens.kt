package com.example.doineedto.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.doineedto.R
import com.example.doineedto.ui.common.HeroCard
import com.example.doineedto.ui.common.SettingsGroup
import com.example.doineedto.ui.settings.PermissionSetupSection

@Composable
fun OnboardingIntroScreen(
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            HeroCard(
                title = stringResource(R.string.onboarding_title),
                body = stringResource(R.string.onboarding_intro),
            )
            SettingsGroup(
                title = stringResource(R.string.onboarding_how_title),
                content = {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.onboarding_step_one_title)) },
                        supportingContent = { Text(stringResource(R.string.onboarding_step_one_body)) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.onboarding_step_two_title)) },
                        supportingContent = { Text(stringResource(R.string.onboarding_step_two_body)) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.onboarding_step_three_title)) },
                        supportingContent = { Text(stringResource(R.string.onboarding_step_three_body)) }
                    )
                }
            )
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
    }
}

@Composable
fun OnboardingPermissionsScreen(
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
                title = stringResource(R.string.onboarding_permissions_title),
                body = stringResource(R.string.onboarding_permissions_body),
                compact = true,
            )
            SettingsGroup(
                title = stringResource(R.string.more_permissions),
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
            Text(stringResource(R.string.onboarding_finish))
        }
    }
}
