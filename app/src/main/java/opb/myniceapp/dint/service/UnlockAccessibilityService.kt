package opb.myniceapp.dint.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import opb.myniceapp.dint.InterventionActivity
import opb.myniceapp.dint.data.AppPreferences
import opb.myniceapp.dint.data.RepositoryScope
import opb.myniceapp.dint.data.UnlockLogRepository
import kotlinx.coroutines.launch

class UnlockAccessibilityService : AccessibilityService() {
    private lateinit var preferences: AppPreferences
    private lateinit var repository: UnlockLogRepository
    private var screenReceiver: BroadcastReceiver? = null
    private var homePackageName: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pendingCheck: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        preferences = AppPreferences(this)
        repository = UnlockLogRepository(this)
        homePackageName = resolveHomePackageName()
        RepositoryScope.launch { repository.migrateFromPreferencesIfNeeded(this@UnlockAccessibilityService) }
        registerScreenReceiver()
    }

    private fun resolveHomePackageName(): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentPackage = event?.packageName?.toString() ?: return
        if (currentPackage == packageName) return
        // Systemui fires plenty of window-state events (status bar/notification-shade updates)
        // that don't represent a real foreground change. Filter these out *before* touching
        // pendingCheck, so they can't cancel an already-scheduled check for a real target app.
        if (currentPackage == "com.android.systemui") return
        // Unlocking almost always resurfaces the home launcher first, which would otherwise
        // consume the one-shot unlock-pending flag before the user opens the app they meant to.
        if (currentPackage == homePackageName) return

        // Fingerprint/biometric unlock can surface a brief intermediate system window (package
        // name varies by OEM) before the real target app appears. Defer evaluation so a newer
        // window-change event can supersede a stale one instead of evaluating every transient
        // window as it arrives.
        pendingCheck?.let(handler::removeCallbacks)
        val check = Runnable { evaluateForegroundPackage(currentPackage) }
        pendingCheck = check
        handler.postDelayed(check, SETTLE_DELAY_MILLIS)
    }

    private fun evaluateForegroundPackage(currentPackage: String) {
        if (preferences.isExcludedPackage(currentPackage)) {
            // Landing on an excluded app (e.g. a push-approval authenticator opened via
            // notification) consumes this unlock, so nothing later in the approval flow
            // can retroactively trigger the intervention off the same unlock event.
            preferences.clearUnlockPending()
            return
        }
        if (!preferences.hasUnlockPending()) return
        if (!preferences.isInterventionAllowedNow()) {
            preferences.clearUnlockPending()
            return
        }
        if (preferences.shouldCooldownIntervention()) return

        preferences.clearUnlockPending()
        preferences.markInterventionShown()
        repository.insertPendingBlocking()
        startActivity(InterventionActivity.createIntent(this))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        handler.removeCallbacksAndMessages(null)
        screenReceiver?.let(::unregisterReceiver)
        screenReceiver = null
        super.onDestroy()
    }

    private fun registerScreenReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> preferences.markUnlockPending()
                    Intent.ACTION_SCREEN_OFF -> {
                        preferences.clearUnlockPending()
                        RepositoryScope.launch { repository.clearPendingLog() }
                    }
                }
            }
        }
        registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )
        screenReceiver = receiver
    }

    companion object {
        @Volatile
        private var activeInstance: UnlockAccessibilityService? = null
        private const val SETTLE_DELAY_MILLIS = 350L

        fun tryLockScreen(): Boolean {
            val service = activeInstance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
