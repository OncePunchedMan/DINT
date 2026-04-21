package com.example.doineedto.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.doineedto.InterventionActivity
import com.example.doineedto.data.AppPreferences

class UnlockAccessibilityService : AccessibilityService() {
    private lateinit var preferences: AppPreferences
    private var screenReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        preferences = AppPreferences(this)
        registerScreenReceiver()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentPackage = event?.packageName?.toString() ?: return
        if (currentPackage == packageName) return
        if (currentPackage == "com.android.systemui") return
        if (!preferences.hasUnlockPending()) return
        if (!preferences.isInterventionAllowedNow()) {
            preferences.clearUnlockPending()
            return
        }
        if (preferences.shouldCooldownIntervention()) return

        preferences.clearUnlockPending()
        preferences.markInterventionShown()
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
                    Intent.ACTION_SCREEN_OFF -> preferences.clearUnlockPending()
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
