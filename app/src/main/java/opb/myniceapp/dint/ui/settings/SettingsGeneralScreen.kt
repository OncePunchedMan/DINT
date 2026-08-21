package opb.myniceapp.dint.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import opb.myniceapp.dint.R
import opb.myniceapp.dint.ui.common.ActionRow
import opb.myniceapp.dint.ui.common.SettingsGroup
import opb.myniceapp.dint.ui.common.SubPageHeader

@Composable
fun SettingsUiScreen(
    friction: Int,
    frictionLabel: String,
    onFrictionChanged: (Int) -> Unit,
    scheduleEnabled: Boolean,
    scheduleStartMinutes: Int,
    scheduleEndMinutes: Int,
    onScheduleEnabledChanged: (Boolean) -> Unit,
    onRequestTimePicker: (ScheduleTimeField, Int) -> Unit,
    hardModeEnabled: Boolean,
    onHardModeChanged: (Boolean) -> Unit,
    hideLockOutcomes: Boolean,
    onHiddenOutcomesChanged: (Boolean) -> Unit,
    onPreviewIntervention: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SubPageHeader(title = stringResource(R.string.more_settings), onBack = onBack)
        }
        item {
            SettingsGroup(
                content = {
                    PauseStrengthRow(
                        friction = friction,
                        frictionLabel = frictionLabel,
                        onFrictionChanged = onFrictionChanged,
                    )
                    HorizontalDivider()
                    ScheduleSection(
                        isEnabled = scheduleEnabled,
                        startMinutes = scheduleStartMinutes,
                        endMinutes = scheduleEndMinutes,
                        onEnabledChanged = onScheduleEnabledChanged,
                        onRequestTimePicker = onRequestTimePicker,
                    )
                    HorizontalDivider()
                    HardModeSection(
                        isEnabled = hardModeEnabled,
                        onEnabledChanged = onHardModeChanged
                    )
                    HorizontalDivider()
                    HiddenOutcomeSection(
                        isEnabled = hideLockOutcomes,
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
    }
}
