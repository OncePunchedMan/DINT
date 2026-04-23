package com.example.doineedto.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.roundToLong

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // TODO: Add a future setting to back up history and settings via protobuf export/import.

    fun getFriction(): Int = prefs.getInt(KEY_FRICTION, 40)

    fun setFriction(value: Int) {
        prefs.edit().putInt(KEY_FRICTION, value.coerceIn(0, 100)).apply()
    }

    fun isHardModeEnabled(): Boolean = prefs.getBoolean(KEY_HARD_MODE_ENABLED, false)

    fun setHardModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HARD_MODE_ENABLED, enabled).apply()
    }

    fun hasSeenInitialSetup(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_INITIAL_SETUP, false)

    fun markInitialSetupSeen() {
        prefs.edit().putBoolean(KEY_HAS_SEEN_INITIAL_SETUP, true).apply()
    }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)

    fun markOnboardingCompleted() {
        prefs.edit()
            .putBoolean(KEY_HAS_COMPLETED_ONBOARDING, true)
            .putBoolean(KEY_HAS_SEEN_INITIAL_SETUP, true)
            .apply()
    }

    fun shouldHideLockOutcomes(): Boolean = prefs.getBoolean(KEY_HIDE_LOCK_OUTCOMES, false)

    fun setHideLockOutcomes(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_LOCK_OUTCOMES, enabled).apply()
    }

    fun isScheduleEnabled(): Boolean = prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)

    fun setScheduleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply()
    }

    fun getScheduleStartMinutes(): Int = prefs.getInt(KEY_SCHEDULE_START_MINUTES, 8 * 60)

    fun setScheduleStartMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SCHEDULE_START_MINUTES, minutes.normalizedMinutes()).apply()
    }

    fun getScheduleEndMinutes(): Int = prefs.getInt(KEY_SCHEDULE_END_MINUTES, 22 * 60)

    fun setScheduleEndMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SCHEDULE_END_MINUTES, minutes.normalizedMinutes()).apply()
    }

    fun getScheduleWindow(): ScheduleWindow =
        ScheduleWindow(
            isEnabled = isScheduleEnabled(),
            startMinutes = getScheduleStartMinutes(),
            endMinutes = getScheduleEndMinutes(),
        )

    fun isInterventionAllowedNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val schedule = getScheduleWindow()
        if (!schedule.isEnabled) return true

        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        return if (schedule.startMinutes == schedule.endMinutes) {
            true
        } else if (schedule.startMinutes < schedule.endMinutes) {
            currentMinutes in schedule.startMinutes until schedule.endMinutes
        } else {
            currentMinutes >= schedule.startMinutes || currentMinutes < schedule.endMinutes
        }
    }

    fun markUnlockPending() {
        prefs.edit().putLong(KEY_PENDING_UNLOCK_AT, System.currentTimeMillis()).apply()
    }

    fun hasUnlockPending(windowMillis: Long = 10_000L): Boolean {
        val timestamp = prefs.getLong(KEY_PENDING_UNLOCK_AT, 0L)
        return timestamp > 0L && (System.currentTimeMillis() - timestamp) <= windowMillis
    }

    fun clearUnlockPending() {
        prefs.edit().putLong(KEY_PENDING_UNLOCK_AT, 0L).apply()
    }

    fun shouldCooldownIntervention(cooldownMillis: Long = 5_000L): Boolean {
        val lastInterventionAt = prefs.getLong(KEY_LAST_INTERVENTION_AT, 0L)
        return (System.currentTimeMillis() - lastInterventionAt) < cooldownMillis
    }

    fun markInterventionShown() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_INTERVENTION_AT, now).apply()
        appendUnlockLog(
            UnlockLogEntry(
                timestamp = now,
                reason = "",
                action = UnlockAction.PENDING.value,
            )
        )
    }

    fun waitDurationMillis(): Long {
        val friction = getFriction()
        return (friction / 100f * MAX_WAIT_MILLIS).roundToLong()
    }

    fun shouldRejectRepeatedReason(reason: String, repetitionThreshold: Int = 3): Boolean {
        if (!ReasonValidator.isDistractionReason(reason)) return false

        val normalizedReason = ReasonValidator.normalizeReason(reason)
        if (normalizedReason.isBlank()) return false

        val cutoff = System.currentTimeMillis() - REPEATED_REASON_COOLDOWN_MILLIS
        val recentMatches = getUnlockLogs()
            .asSequence()
            .filter { it.action != UnlockAction.PENDING.value }
            .filter { it.timestamp >= cutoff }
            .count { entry ->
                ReasonValidator.normalizeReason(entry.reason) == normalizedReason
            }

        return recentMatches >= repetitionThreshold
    }

    fun completeLatestUnlock(reason: String, action: UnlockAction) {
        val logs = getUnlockLogs().toMutableList()
        val index = logs.indexOfLast { it.action == UnlockAction.PENDING.value }
        val cleanReason = reason.trim()

        if (index >= 0) {
            logs[index] = logs[index].copy(
                reason = cleanReason,
                action = action.value,
            )
        } else {
            logs += UnlockLogEntry(
                timestamp = System.currentTimeMillis(),
                reason = cleanReason,
                action = action.value,
            )
        }

        saveUnlockLogs(logs)
    }

    fun getUnlockLogs(): List<UnlockLogEntry> {
        val raw = prefs.getString(KEY_UNLOCK_LOGS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    UnlockLogEntry(
                        timestamp = item.optLong("timestamp"),
                        reason = item.optString("reason"),
                        action = item.optString("action"),
                    )
                )
            }
        }.sortedByDescending { it.timestamp }
    }

    fun getDailyUnlockCounts(limit: Int = 7): List<DailyUnlockCount> {
        val formatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val grouped = LinkedHashMap<String, Int>()

        getUnlockLogs()
            .sortedByDescending { it.timestamp }
            .forEach { entry ->
                val key = formatter.format(Date(entry.timestamp))
                grouped[key] = (grouped[key] ?: 0) + 1
            }

        return grouped.entries.take(limit).map { DailyUnlockCount(it.key, it.value) }
    }

    fun totalUnlocks(): Int = getUnlockLogs().size

    fun continuedUnlocks(): Int = getUnlockLogs().count { it.action == UnlockAction.CONTINUE.value }

    fun keptLockedUnlocks(): Int = getUnlockLogs().count { it.action == UnlockAction.KEEP_LOCKED.value }

    private fun appendUnlockLog(entry: UnlockLogEntry) {
        val updated = (getUnlockLogs() + entry).sortedByDescending { it.timestamp }.take(MAX_LOG_ENTRIES)
        saveUnlockLogs(updated)
    }

    private fun saveUnlockLogs(entries: List<UnlockLogEntry>) {
        val array = JSONArray()
        entries
            .sortedByDescending { it.timestamp }
            .take(MAX_LOG_ENTRIES)
            .forEach { entry ->
                array.put(
                    JSONObject().apply {
                        put("timestamp", entry.timestamp)
                        put("reason", entry.reason)
                        put("action", entry.action)
                    }
                )
            }

        prefs.edit().putString(KEY_UNLOCK_LOGS, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "do_i_need_to"
        private const val KEY_FRICTION = "friction"
        private const val KEY_PENDING_UNLOCK_AT = "pending_unlock_at"
        private const val KEY_LAST_INTERVENTION_AT = "last_intervention_at"
        private const val KEY_UNLOCK_LOGS = "unlock_logs"
        private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        private const val KEY_SCHEDULE_START_MINUTES = "schedule_start_minutes"
        private const val KEY_SCHEDULE_END_MINUTES = "schedule_end_minutes"
        private const val KEY_HARD_MODE_ENABLED = "hard_mode_enabled"
        private const val KEY_HIDE_LOCK_OUTCOMES = "hide_lock_outcomes"
        private const val KEY_HAS_SEEN_INITIAL_SETUP = "has_seen_initial_setup"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
        private const val MAX_WAIT_MILLIS = 12_000L
        private const val MAX_LOG_ENTRIES = 200
        private const val REPEATED_REASON_COOLDOWN_MILLIS = 3 * 60 * 60 * 1000L
    }
}

data class ScheduleWindow(
    val isEnabled: Boolean,
    val startMinutes: Int,
    val endMinutes: Int,
)

data class UnlockLogEntry(
    val timestamp: Long,
    val reason: String,
    val action: String,
)

data class DailyUnlockCount(
    val label: String,
    val count: Int,
)

enum class UnlockAction(val value: String) {
    PENDING("pending"),
    CONTINUE("continue"),
    KEEP_LOCKED("keep_locked"),
    SKIPPED("skipped"),
}

private fun Int.normalizedMinutes(): Int = ((this % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY

private const val MINUTES_PER_DAY = 24 * 60
