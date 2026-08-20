package opb.myniceapp.dint.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.data.AppTargetSelection
import opb.myniceapp.dint.data.LaunchableApp
import opb.myniceapp.dint.data.PresetTargetCategory
import opb.myniceapp.dint.ui.common.AppPickerDialog

@Composable
fun AppLaunchTargetsSection(
    selections: Map<PresetTargetCategory, AppTargetSelection?>,
    launchableApps: List<LaunchableApp>,
    onSelect: (PresetTargetCategory, AppTargetSelection) -> Unit,
    onClear: (PresetTargetCategory) -> Unit,
) {
    var chooserCategory by rememberSaveable { mutableStateOf<PresetTargetCategory?>(null) }

    Column {
        ListItem(
            headlineContent = { Text(stringResource(R.string.app_targets_title)) },
            supportingContent = { Text(stringResource(R.string.app_targets_description)) }
        )

        PresetTargetCategory.entries.forEachIndexed { index, category ->
            if (index > 0) {
                HorizontalDivider()
            }

            AppTargetRow(
                category = category,
                selection = selections[category],
                onChoose = { chooserCategory = category },
                onClear = { onClear(category) }
            )
        }
    }

    if (chooserCategory != null) {
        AppTargetChooserDialog(
            category = chooserCategory!!,
            apps = launchableApps,
            onDismiss = { chooserCategory = null },
            onSelect = { app ->
                onSelect(
                    chooserCategory!!,
                    AppTargetSelection(packageName = app.packageName, label = app.label),
                )
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
                        Text(stringResource(R.string.clear_app))
                    }
                }
                Button(
                    onClick = onChoose,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        stringResource(
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
    AppPickerDialog(
        title = "${stringResource(R.string.choose_app_dialog_title)}: ${category.title}",
        apps = apps,
        onDismiss = onDismiss,
        onSelect = onSelect,
    )
}

@Composable
private fun appTargetDescription(category: PresetTargetCategory): String = when (category) {
    PresetTargetCategory.MEMES -> stringResource(R.string.app_target_memes_description)
    PresetTargetCategory.SOCIAL -> stringResource(R.string.app_target_social_description)
    PresetTargetCategory.NEWS -> stringResource(R.string.app_target_news_description)
    PresetTargetCategory.VIDEO -> stringResource(R.string.app_target_video_description)
    PresetTargetCategory.MUSIC -> stringResource(R.string.app_target_music_description)
    PresetTargetCategory.SHOPPING -> stringResource(R.string.app_target_shopping_description)
    PresetTargetCategory.GAMES -> stringResource(R.string.app_target_games_description)
}
