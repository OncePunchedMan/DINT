package opb.myniceapp.dint

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import opb.myniceapp.dint.admin.FocusDeviceAdminReceiver
import opb.myniceapp.dint.data.AppPreferences
import opb.myniceapp.dint.data.UnlockAction
import opb.myniceapp.dint.data.launchIntentForReason
import opb.myniceapp.dint.service.UnlockAccessibilityService
import opb.myniceapp.dint.ui.AppTheme
import opb.myniceapp.dint.ui.intervention.InterventionScreen
import opb.myniceapp.dint.ui.intervention.InterventionViewModel

class InterventionActivity : ComponentActivity() {
    private val viewModel: InterventionViewModel by viewModels {
        InterventionViewModel.factory((application as DintApplication).container)
    }
    private var timer: CountDownTimer? = null
    private var decisionMade = false
    private var hardModeEnabled = false
    private val preferences: AppPreferences by lazy { (application as DintApplication).container.preferences }
    private val screenOffReceiver =
        object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_SCREEN_OFF || decisionMade || isFinishing) return
                preferences.clearUnlockPending()
                viewModel.clearPendingLog()
                finish()
            }
        }
    private val relaunchHandler = Handler(Looper.getMainLooper())
    private val relaunchRunnable = Runnable {
        if (hardModeEnabled && !decisionMade && !isFinishing) {
            startActivity(createIntent(this))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        val waitMillis = viewModel.waitMillis
        hardModeEnabled = viewModel.hardModeEnabled
        setFinishOnTouchOutside(false)
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        setContent {
            var remainingMillis by remember { mutableLongStateOf(waitMillis) }
            var canContinue by remember { mutableStateOf(waitMillis == 0L) }

            DisposableEffect(waitMillis) {
                timer?.cancel()
                if (waitMillis == 0L) {
                    canContinue = true
                } else {
                    timer = object : CountDownTimer(waitMillis, 250L) {
                        override fun onTick(millisUntilFinished: Long) {
                            remainingMillis = millisUntilFinished
                        }

                        override fun onFinish() {
                            remainingMillis = 0L
                            canContinue = true
                        }
                    }
                    timer?.start()
                }

                onDispose {
                    timer?.cancel()
                    timer = null
                }
            }

            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InterventionScreen(
                        viewModel = viewModel,
                        remainingMillis = remainingMillis,
                        canContinue = canContinue,
                        onKeepLockedReasonSelected = { selected ->
                            viewModel.onReasonChanged(selected)
                            decisionMade = true
                            viewModel.completeUnlock(selected, UnlockAction.KEEP_LOCKED)
                            keepLocked()
                        },
                        onKeepLocked = {
                            decisionMade = true
                            viewModel.completeUnlock(viewModel.reason.value, UnlockAction.KEEP_LOCKED)
                            keepLocked()
                        },
                        onContinue = {
                            decisionMade = true
                            val reason = viewModel.reason.value
                            viewModel.completeUnlock(reason, UnlockAction.CONTINUE)
                            openAppForReasonIfPossible(reason, preferences)
                            finish()
                        },
                        onEmergencySkip = {
                            decisionMade = true
                            viewModel.completeUnlock(viewModel.reason.value, UnlockAction.SKIPPED)
                            finish()
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        relaunchHandler.removeCallbacks(relaunchRunnable)
        runCatching { unregisterReceiver(screenOffReceiver) }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        scheduleRelaunchIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        scheduleRelaunchIfNeeded()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        scheduleRelaunchIfNeeded()
    }

    private fun scheduleRelaunchIfNeeded() {
        relaunchHandler.removeCallbacks(relaunchRunnable)
        if (hardModeEnabled && !decisionMade && !isFinishing) {
            relaunchHandler.postDelayed(relaunchRunnable, 150L)
        }
    }

    private fun dismissToHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    private fun keepLocked() {
        if (UnlockAccessibilityService.tryLockScreen()) {
            finish()
            return
        }

        val manager = getSystemService<DevicePolicyManager>()
        val admin = ComponentName(this, FocusDeviceAdminReceiver::class.java)
        if (manager != null && manager.isAdminActive(admin)) {
            manager.lockNow()
            finish()
        } else {
            dismissToHome()
        }
    }

    private fun openAppForReasonIfPossible(reason: String, preferences: AppPreferences) {
        val launchIntent = launchIntentForReason(
            context = this,
            reason = reason,
            appTargetSelection = preferences::getAppTargetSelection,
        ) ?: return

        startActivity(launchIntent)
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, InterventionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
