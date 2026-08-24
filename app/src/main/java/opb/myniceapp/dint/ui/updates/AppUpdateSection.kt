package opb.myniceapp.dint.ui.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.ui.main.UpdateUiState

@Composable
fun AppUpdateSection(
    state: UpdateUiState,
    backgroundCheckEnabled: Boolean,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onBackgroundCheckToggled: (Boolean) -> Unit,
) {
    Column {
        ListItem(
            headlineContent = { Text(stringResource(R.string.updates_title)) },
            supportingContent = {
                Text(state.message ?: stringResource(R.string.updates_description))
            },
            trailingContent = {
                Button(
                    onClick = onCheckForUpdate,
                    enabled = !state.isChecking && !state.isInstalling,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        stringResource(
                            if (state.isChecking) R.string.update_checking else R.string.check_for_update
                        )
                    )
                }
            }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.background_update_check_title)) },
            supportingContent = { Text(stringResource(R.string.background_update_check_description)) },
            trailingContent = {
                Switch(
                    checked = backgroundCheckEnabled,
                    onCheckedChange = onBackgroundCheckToggled,
                )
            }
        )

        state.availableUpdate?.let { update ->
            HorizontalDivider()
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(R.string.update_available_title, update.versionName),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                supportingContent = {
                    Text(stringResource(R.string.update_available_asset, update.apkName))
                },
                trailingContent = {
                    Button(
                        onClick = onInstallUpdate,
                        enabled = !state.isChecking && !state.isInstalling,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            stringResource(
                                if (state.isInstalling) R.string.update_downloading_short else R.string.install_update
                            )
                        )
                    }
                }
            )
        }
    }
}
