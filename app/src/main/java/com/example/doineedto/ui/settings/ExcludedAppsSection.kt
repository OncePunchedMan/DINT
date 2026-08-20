package com.example.doineedto.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.doineedto.R
import com.example.doineedto.data.LaunchableApp
import com.example.doineedto.ui.common.AppPickerDialog

@Composable
fun ExcludedAppsSection(
    excludedPackages: Set<String>,
    launchableApps: List<LaunchableApp>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val labelsByPackage = remember(launchableApps) {
        launchableApps.associate { it.packageName to it.label }
    }

    Column {
        ListItem(
            headlineContent = { Text(stringResource(R.string.excluded_apps_description)) },
            trailingContent = {
                Button(
                    onClick = { showPicker = true },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.add_excluded_app))
                }
            }
        )

        if (excludedPackages.isEmpty()) {
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.no_excluded_apps)) }
            )
        } else {
            excludedPackages.sorted().forEach { packageName ->
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(labelsByPackage[packageName] ?: packageName) },
                    trailingContent = {
                        TextButton(onClick = { onRemove(packageName) }) {
                            Text(stringResource(R.string.remove_excluded_app))
                        }
                    }
                )
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            title = stringResource(R.string.excluded_apps_title),
            apps = launchableApps,
            onDismiss = { showPicker = false },
            onSelect = { app ->
                onAdd(app.packageName)
                showPicker = false
            }
        )
    }
}
