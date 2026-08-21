package opb.myniceapp.dint.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.data.AppTargetSelection
import opb.myniceapp.dint.data.PresetTargetCategory
import opb.myniceapp.dint.ui.common.SettingsGroup
import opb.myniceapp.dint.ui.common.SubPageHeader
import opb.myniceapp.dint.ui.main.MainUiState

@Composable
fun SettingsShortcutsScreen(
    uiState: MainUiState,
    onExcludedAppAdded: (String) -> Unit,
    onExcludedAppRemoved: (String) -> Unit,
    onAppTargetSelected: (PresetTargetCategory, AppTargetSelection) -> Unit,
    onAppTargetCleared: (PresetTargetCategory) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SubPageHeader(title = stringResource(R.string.settings_shortcuts_title), onBack = onBack)
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
    }
}
