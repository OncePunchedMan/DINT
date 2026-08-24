package opb.myniceapp.dint.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.ui.common.SettingsGroup
import opb.myniceapp.dint.ui.main.UpdateUiState
import opb.myniceapp.dint.ui.updates.AppUpdateSection

@Composable
fun MoreTab(
    appVersionName: String,
    shouldShowPermissionSetup: Boolean,
    updateUiState: UpdateUiState,
    backgroundUpdateCheckEnabled: Boolean,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onBackgroundUpdateCheckToggled: (Boolean) -> Unit,
    onNavigateToUi: () -> Unit,
    onNavigateToShortcuts: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SettingsNavEntry(
                title = stringResource(R.string.more_settings),
                subtitle = stringResource(R.string.settings_ui_subtitle),
                icon = Icons.Outlined.Tune,
                onClick = onNavigateToUi,
            )
        }
        item {
            SettingsNavEntry(
                title = stringResource(R.string.settings_shortcuts_title),
                subtitle = stringResource(R.string.settings_shortcuts_subtitle),
                icon = Icons.Outlined.Apps,
                onClick = onNavigateToShortcuts,
            )
        }
        item {
            SettingsNavEntry(
                title = stringResource(R.string.more_permissions),
                subtitle = stringResource(
                    if (shouldShowPermissionSetup) R.string.permissions_status_needed
                    else R.string.permissions_status_ok
                ),
                icon = Icons.Outlined.Shield,
                onClick = onNavigateToPermissions,
            )
        }
        item {
            BankingDisclaimerCard()
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.updates_title),
                icon = Icons.Outlined.SystemUpdate,
                content = {
                    AppUpdateSection(
                        state = updateUiState,
                        backgroundCheckEnabled = backgroundUpdateCheckEnabled,
                        onCheckForUpdate = onCheckForUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onBackgroundCheckToggled = onBackgroundUpdateCheckToggled,
                    )
                }
            )
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.about_title),
                icon = Icons.Outlined.Info,
                content = {
                    AboutCard(appVersionName = appVersionName)
                }
            )
        }
    }
}

@Composable
private fun SettingsNavEntry(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    SettingsGroup(
        title = title,
        icon = icon,
        content = {
            ListItem(
                headlineContent = { Text(subtitle) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable(onClick = onClick),
            )
        }
    )
}

@Composable
private fun BankingDisclaimerCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.banking_warning_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.banking_warning_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
//        HorizontalDivider()
//        ListItem(
//            headlineContent = { Text(stringResource(R.string.about_author_label)) },
//            supportingContent = { Text(stringResource(R.string.about_author_value)) }
//        )
    }
}
