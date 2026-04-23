package com.example.doineedto

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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.doineedto.admin.FocusDeviceAdminReceiver
import com.example.doineedto.data.AppPreferences
import com.example.doineedto.data.ReasonValidator
import com.example.doineedto.data.UnlockAction
import com.example.doineedto.service.UnlockAccessibilityService
import com.example.doineedto.ui.AppTheme
import kotlin.math.roundToLong

class InterventionActivity : ComponentActivity() {
    private var timer: CountDownTimer? = null
    private var decisionMade = false
    private var hardModeEnabled = false
    private var shouldHideLockOutcomes = false
    private lateinit var preferences: AppPreferences
    private val screenOffReceiver =
        object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_SCREEN_OFF || decisionMade || isFinishing) return
                preferences.clearUnlockPending()
                preferences.clearPendingUnlockLog()
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
        preferences = AppPreferences(this)
        val waitMillis = preferences.waitDurationMillis()
        hardModeEnabled = preferences.isHardModeEnabled()
        shouldHideLockOutcomes = preferences.shouldHideLockOutcomes()
        setFinishOnTouchOutside(false)
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        setContent {
            var remainingMillis by androidx.compose.runtime.remember { mutableLongStateOf(waitMillis) }
            var canContinue by androidx.compose.runtime.remember { mutableStateOf(waitMillis == 0L) }
            var reason by remember { mutableStateOf("") }
            val isRepeatedDistractionReason = remember(reason) {
                preferences.shouldRejectRepeatedReason(reason)
            }

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
                        remainingMillis = remainingMillis,
                        canContinue = canContinue,
                        hardModeEnabled = hardModeEnabled,
                        hideLockOutcomes = shouldHideLockOutcomes,
                        reason = reason,
                        isRepeatedDistractionReason = isRepeatedDistractionReason,
                        onReasonChanged = { reason = it },
                        continueReasons = continueReasons,
                        keepLockedReasons = keepLockedReasons,
                        onCuratedReasonSelected = { selected ->
                            reason = selected
                        },
                        onKeepLockedReasonSelected = { selected ->
                            reason = selected
                            decisionMade = true
                            preferences.completeLatestUnlock(selected, UnlockAction.KEEP_LOCKED)
                            keepLocked()
                        },
                        onKeepLocked = {
                            decisionMade = true
                            preferences.completeLatestUnlock(reason, UnlockAction.KEEP_LOCKED)
                            keepLocked()
                        },
                        onContinue = {
                            decisionMade = true
                            preferences.completeLatestUnlock(reason, UnlockAction.CONTINUE)
                            finish()
                        },
                        onEmergencySkip = {
                            decisionMade = true
                            preferences.completeLatestUnlock(reason, UnlockAction.SKIPPED)
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

companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, InterventionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@androidx.compose.runtime.Composable
private fun InterventionScreen(
    remainingMillis: Long,
    canContinue: Boolean,
    hardModeEnabled: Boolean,
    hideLockOutcomes: Boolean,
    reason: String,
    isRepeatedDistractionReason: Boolean,
    onReasonChanged: (String) -> Unit,
    continueReasons: List<String>,
    keepLockedReasons: List<String>,
    onCuratedReasonSelected: (String) -> Unit,
    onKeepLockedReasonSelected: (String) -> Unit,
    onKeepLocked: () -> Unit,
    onContinue: () -> Unit,
    onEmergencySkip: () -> Unit,
) {
    val secondsLeft = (remainingMillis / 1000f).roundToLong()
    val allCuratedReasons = remember(continueReasons, keepLockedReasons) {
        (continueReasons + keepLockedReasons).toSet()
    }
    val isReasonValid = remember(reason, allCuratedReasons) {
        ReasonValidator.isReasonValid(reason, allCuratedReasons)
    }
    val canSubmit = canContinue && isReasonValid && !isRepeatedDistractionReason
    var showMoreContinueReasons by remember { mutableStateOf(false) }
    var showMoreHiddenReasons by remember { mutableStateOf(false) }
    BackHandler(enabled = hardModeEnabled) {}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PromptTitle(onEmergencySkip = onEmergencySkip)
            Text(
                text = "Choose a reason, edit it if needed, then decide whether this unlock is intentional.",
                style = MaterialTheme.typography.bodyLarge
            )
            if (hideLockOutcomes) {
                Text(
                    text = "Possible reasons",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    hiddenReasonOptions(showMoreHiddenReasons).forEach { option ->
                        SuggestionChip(
                            onClick = {
                                if (keepLockedReasons.contains(option)) {
                                    onKeepLockedReasonSelected(option)
                                } else {
                                    onCuratedReasonSelected(option)
                                }
                            },
                            label = { Text(option) }
                        )
                    }
                }
                if (!showMoreHiddenReasons) {
                    TextButton(onClick = { showMoreHiddenReasons = true }) {
                        Text("Show more")
                    }
                }
            } else {
                Text(
                    text = "Use device",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleContinueReasons(showMoreContinueReasons).forEach { option ->
                        SuggestionChip(
                            onClick = { onCuratedReasonSelected(option) },
                            label = { Text(option) }
                        )
                    }
                }
                if (!showMoreContinueReasons) {
                    TextButton(onClick = { showMoreContinueReasons = true }) {
                        Text("Show more")
                    }
                }
                Text(
                    text = "Keep device locked",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    keepLockedReasons.forEach { option ->
                        SuggestionChip(
                            onClick = { onKeepLockedReasonSelected(option) },
                            label = { Text(option) }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your reason") },
                placeholder = { Text("Message someone, check a map, take a photo...") },
                minLines = 2,
                isError = reason.isNotBlank() && (!isReasonValid || isRepeatedDistractionReason),
                supportingText = {
                    if (reason.isNotBlank() && isRepeatedDistractionReason) {
                        Text("That reason has come up too often lately. Pick a clearer purpose or keep the phone locked.")
                    } else if (reason.isNotBlank() && !isReasonValid) {
                        Text("Use a real reason like replying, directions, camera, music, work, or another concrete task.")
                    }
                }
            )
            OutlinedButton(
                onClick = onKeepLocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Keep it locked")
            }
            Button(
                onClick = onContinue,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        !canContinue -> "Continue in ${secondsLeft.coerceAtLeast(1)}s"
                        reason.trim().isBlank() -> "Enter a reason to continue"
                        isRepeatedDistractionReason -> "Pick a different reason"
                        !isReasonValid -> "Enter a clearer reason"
                        else -> "Use my phone"
                    }
                )
            }
            if (hardModeEnabled) {
                Text(
                    text = "Hard mode is on. You must interact with this reminder before using the device.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@androidx.compose.runtime.Composable
private fun PromptTitle(onEmergencySkip: () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = "Why are you opening your phone right ",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "now",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onEmergencySkip)
        )
        Text(
            text = "?",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

private fun visibleContinueReasons(showMore: Boolean): List<String> =
    if (showMore) continueReasons else continueReasons.take(6)

private fun hiddenReasonOptions(showMore: Boolean): List<String> =
    if (showMore) mergedReasons else mergedReasons.take(8)

private val continueReasons = listOf(
    "Reply to someone",
    "Check messages",
    "Call someone",
    "Look something up",
    "Use the camera",
    "Check directions",
    "Open Instagram",
    "Open TikTok",
    "Open YouTube",
    "Check Reddit",
    "Play music or a podcast",
    "Check my calendar",
    "Open a ticket or booking",
    "Check email",
    "Check my bank",
    "Read the news",
    "Read something",
    "Check the weather",
    "Use notes or tasks",
    "Shop for something",
    "Play a game",
    "Something else",
)

private val keepLockedReasons = listOf(
    "Just checking",
    "I opened it automatically",
    "I am bored",
    "I want to scroll",
    "I want to check Instagram",
    "I want to check TikTok",
    "I just want dopamine",
    "No real reason",
)

private val mergedReasons = listOf(
    "Reply to someone",
    "Check messages",
    "Just checking",
    "Look something up",
    "I opened it automatically",
    "Use the camera",
    "I am bored",
    "Check directions",
    "Open Instagram",
    "I want to check Instagram",
    "Open TikTok",
    "I want to check TikTok",
    "Open YouTube",
    "No real reason",
    "I want to scroll",
    "Play music or a podcast",
    "Check my calendar",
    "Open a ticket or booking",
    "Check email",
    "Check my bank",
    "Read the news",
    "Read something",
    "Check the weather",
    "Use notes or tasks",
    "Shop for something",
    "Play a game",
    "I just want dopamine",
    "Something else",
)
