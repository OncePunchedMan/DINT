package opb.myniceapp.dint.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import java.util.Calendar
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

    fun isBackgroundUpdateCheckEnabled(): Boolean =
        prefs.getBoolean(KEY_BACKGROUND_UPDATE_CHECK_ENABLED, true)

    fun setBackgroundUpdateCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_UPDATE_CHECK_ENABLED, enabled).apply()
    }

    fun getLastNotifiedUpdateVersion(): String? = prefs.getString(KEY_LAST_NOTIFIED_UPDATE_VERSION, null)

    fun setLastNotifiedUpdateVersion(versionName: String) {
        prefs.edit().putString(KEY_LAST_NOTIFIED_UPDATE_VERSION, versionName).apply()
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
        prefs.edit().putLong(KEY_LAST_INTERVENTION_AT, System.currentTimeMillis()).apply()
    }

    fun waitDurationMillis(): Long {
        val friction = getFriction()
        return (friction / 100f * MAX_WAIT_MILLIS).roundToLong()
    }

    fun hasMigratedLogsToRoom(): Boolean = prefs.getBoolean(KEY_HAS_MIGRATED_LOGS_TO_ROOM, false)

    fun markLogsMigratedToRoom() {
        prefs.edit().putBoolean(KEY_HAS_MIGRATED_LOGS_TO_ROOM, true).apply()
    }

    // Legacy SharedPreferences-backed unlock log storage, kept only so UnlockLogRepository can
    // migrate existing entries into Room once. Not for any other use -- see getUnlockLogs()/etc.
    // that used to live here, now backed by Room via UnlockLogRepository.
    fun getUnlockLogsLegacy(): List<UnlockLogEntry> {
        val array = readJsonArrayPreference(KEY_UNLOCK_LOGS)
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

    fun clearLegacyUnlockLogs() {
        prefs.edit().remove(KEY_UNLOCK_LOGS).apply()
    }

    fun getExcludedPackages(): Set<String> {
        val array = readJsonArrayPreference(KEY_EXCLUDED_PACKAGES)
        return buildSet {
            for (index in 0 until array.length()) {
                array.optString(index)?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    fun addExcludedPackage(packageName: String) {
        val updated = getExcludedPackages() + packageName
        saveExcludedPackages(updated)
    }

    fun removeExcludedPackage(packageName: String) {
        val updated = getExcludedPackages() - packageName
        saveExcludedPackages(updated)
    }

    fun isExcludedPackage(packageName: String): Boolean =
        packageName in DEFAULT_EXCLUDED_PACKAGES || packageName in getExcludedPackages()

    private fun saveExcludedPackages(packages: Set<String>) {
        val array = JSONArray()
        packages.forEach { array.put(it) }
        prefs.edit().putString(KEY_EXCLUDED_PACKAGES, array.toString()).apply()
    }

    fun getAppTargetSelection(category: PresetTargetCategory): AppTargetSelection? {
        val packageName = readStringPreference(appTargetPackageKey(category)) ?: return null
        val label = readStringPreference(appTargetLabelKey(category)) ?: return null
        return AppTargetSelection(packageName = packageName, label = label)
    }

    // TODO: When backup/import is added, preserve restored preset app mappings even if the app is
    // missing on this phone, and surface a warning indicator so the user can see the mapping needs attention.

    fun setAppTargetSelection(category: PresetTargetCategory, selection: AppTargetSelection) {
        prefs.edit()
            .putString(appTargetPackageKey(category), selection.packageName)
            .putString(appTargetLabelKey(category), selection.label)
            .apply()
    }

    fun clearAppTargetSelection(category: PresetTargetCategory) {
        prefs.edit()
            .remove(appTargetPackageKey(category))
            .remove(appTargetLabelKey(category))
            .apply()
    }

    private fun readJsonArrayPreference(key: String): JSONArray {
        val raw = readStringPreference(key) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: JSONException) {
            prefs.edit().remove(key).apply()
            JSONArray()
        }
    }

    private fun readStringPreference(key: String): String? {
        return try {
            prefs.getString(key, null)
        } catch (_: ClassCastException) {
            prefs.edit().remove(key).apply()
            null
        }
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
        private const val KEY_BACKGROUND_UPDATE_CHECK_ENABLED = "background_update_check_enabled"
        private const val KEY_LAST_NOTIFIED_UPDATE_VERSION = "last_notified_update_version"
        private const val KEY_EXCLUDED_PACKAGES = "excluded_packages"
        private const val KEY_HIDE_LOCK_OUTCOMES = "hide_lock_outcomes"
        private const val KEY_HAS_SEEN_INITIAL_SETUP = "has_seen_initial_setup"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
        private const val KEY_HAS_MIGRATED_LOGS_TO_ROOM = "has_migrated_logs_to_room"
        private const val MAX_WAIT_MILLIS = 12_000L

        private fun appTargetPackageKey(category: PresetTargetCategory): String =
            "app_target_${category.key}_package"

        private fun appTargetLabelKey(category: PresetTargetCategory): String =
            "app_target_${category.key}_label"
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
