package opb.myniceapp.dint.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        preferences = AppPreferences(this)
        repository = UnlockLogRepository(this)
        RepositoryScope.launch { repository.migrateFromPreferencesIfNeeded(this@UnlockAccessibilityService) }
        registerScreenReceiver()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentPackage = event?.packageName?.toString() ?: return
        if (currentPackage == packageName) return
        if (currentPackage == "com.android.systemui") return
        if (preferences.isExcludedPackage(currentPackage)) return
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

        fun tryLockScreen(): Boolean {
            val service = activeInstance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
