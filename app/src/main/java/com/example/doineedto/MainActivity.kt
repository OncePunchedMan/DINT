package com.example.doineedto

import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.doineedto.admin.FocusDeviceAdminReceiver
import com.example.doineedto.service.UnlockAccessibilityService
import com.example.doineedto.ui.AppTheme
import com.example.doineedto.ui.main.MainScreen
import com.example.doineedto.ui.main.MainViewModel
import com.example.doineedto.ui.settings.ScheduleTimeField

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as DintApplication).container)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onRequestDisableBatteryOptimization = { requestDisableBatteryOptimization() },
                        onRequestNotificationPermission = { requestNotificationPermission() },
                        onOpenAccessibilitySettings = { openAccessibilitySettings() },
                        onOpenDeviceAdminSettings = { requestDeviceAdmin() },
                        onLockNow = { lockNow() },
                        onPreviewIntervention = {
                            startActivity(InterventionActivity.createIntent(this))
                        },
                        onRequestTimePicker = { field, currentMinutes -> requestTimePicker(field, currentMinutes) },
                        onOpenInstallSettings = { (application as DintApplication).container.updateManager.openUnknownAppSourcesSettings() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestDisableBatteryOptimization() {
        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }

        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        try {
            when {
                directIntent.resolveActivity(packageManager) != null -> startActivity(directIntent)
                fallbackIntent.resolveActivity(packageManager) != null -> startActivity(fallbackIntent)
                else -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            )
        }
    }

    private fun requestDeviceAdmin() {
        val componentName = ComponentName(this, FocusDeviceAdminReceiver::class.java)
        startActivity(
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_explanation)
                )
            }
        )
    }

    private fun lockNow() {
        if (UnlockAccessibilityService.tryLockScreen()) {
            return
        }

        val manager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, FocusDeviceAdminReceiver::class.java)
        if (manager.isAdminActive(componentName)) {
            manager.lockNow()
        }
    }

    private fun requestTimePicker(field: ScheduleTimeField, currentMinutes: Int) {
        val hour = currentMinutes / 60
        val minute = currentMinutes % 60
        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val total = selectedHour * 60 + selectedMinute
                when (field) {
                    ScheduleTimeField.START -> viewModel.onScheduleStartChanged(total)
                    ScheduleTimeField.END -> viewModel.onScheduleEndChanged(total)
                }
            },
            hour,
            minute,
            true
        ).show()
    }
}
