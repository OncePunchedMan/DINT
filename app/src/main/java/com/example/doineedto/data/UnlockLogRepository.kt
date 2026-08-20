package com.example.doineedto.data

import android.content.Context
import com.example.doineedto.data.db.UnlockLogDatabase
import com.example.doineedto.data.db.UnlockLogEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

// Not tied to any Activity/Service lifecycle, unlike lifecycleScope which gets cancelled
// right around finish()/onDestroy() -- exactly when InterventionActivity's terminal writes fire.
object RepositoryScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)

class UnlockLogRepository(context: Context) {
    private val dao = UnlockLogDatabase.getInstance(context).unlockLogDao()

    // A deliberate exception to "never block": a single local SQLite insert (sub-millisecond to
    // a few ms) fired once per unlock from the accessibility service, which must complete before
    // InterventionActivity looks up "the latest pending row" -- a fire-and-forget coroutine here
    // would race human reaction time on a genuinely narrow window.
    fun insertPendingBlocking() = runBlocking(Dispatchers.IO) {
        dao.insert(
            UnlockLogEntity(
                timestamp = System.currentTimeMillis(),
                reason = "",
                action = UnlockAction.PENDING.value,
            )
        )
    }

    suspend fun clearPendingLog() {
        dao.clearPending()
    }

    suspend fun completeLatestUnlock(reason: String, action: UnlockAction) {
        val cleanReason = reason.trim()
        val pending = dao.getLatestPending()

        if (pending != null) {
            dao.update(pending.copy(reason = cleanReason, action = action.value))
        } else {
            dao.insert(
                UnlockLogEntity(
                    timestamp = System.currentTimeMillis(),
                    reason = cleanReason,
                    action = action.value,
                )
            )
        }
    }

    suspend fun getRecent(limit: Int): List<UnlockLogEntry> = dao.getRecent(limit).map { it.toUnlockLogEntry() }

    suspend fun getPage(limit: Int, offset: Int): List<UnlockLogEntry> =
        dao.getPage(limit, offset).map { it.toUnlockLogEntry() }

    suspend fun count(): Int = dao.count()

    suspend fun countContinued(): Int = dao.countContinued()

    suspend fun countKeptLocked(): Int = dao.countKeptLocked()

    suspend fun getDailyUnlockCounts(limit: Int = 7): List<DailyUnlockCount> {
        val cutoff = System.currentTimeMillis() - DAILY_COUNT_WINDOW_MILLIS
        val formatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val grouped = LinkedHashMap<String, Int>()

        dao.getSince(cutoff)
            .sortedByDescending { it.timestamp }
            .forEach { entry ->
                val key = formatter.format(Date(entry.timestamp))
                grouped[key] = (grouped[key] ?: 0) + 1
            }

        return grouped.entries.take(limit).map { DailyUnlockCount(it.key, it.value) }
    }

    suspend fun shouldRejectRepeatedReason(reason: String, repetitionThreshold: Int = 3): Boolean {
        if (!ReasonValidator.isDistractionReason(reason)) return false

        val normalizedReason = ReasonValidator.normalizeReason(reason)
        if (normalizedReason.isBlank()) return false

        val cutoff = System.currentTimeMillis() - REPEATED_REASON_COOLDOWN_MILLIS
        return dao.getSince(cutoff).count { entry ->
            ReasonValidator.normalizeReason(entry.reason) == normalizedReason
        } >= repetitionThreshold
    }

    suspend fun countReasonUses(reason: String, sinceMillis: Long): Int {
        val normalized = ReasonValidator.normalizeReason(reason)
        if (normalized.isBlank()) return 0

        return dao.getSince(sinceMillis).count { ReasonValidator.normalizeReason(it.reason) == normalized }
    }

    fun todayStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    suspend fun migrateFromPreferencesIfNeeded(context: Context) {
        val preferences = AppPreferences(context)
        if (preferences.hasMigratedLogsToRoom()) return

        preferences.getUnlockLogsLegacy()
            .filterNot { it.action == UnlockAction.PENDING.value }
            .forEach { entry ->
                dao.insert(
                    UnlockLogEntity(
                        timestamp = entry.timestamp,
                        reason = entry.reason,
                        action = entry.action,
                    )
                )
            }

        preferences.clearLegacyUnlockLogs()
        preferences.markLogsMigratedToRoom()
    }

    private companion object {
        const val REPEATED_REASON_COOLDOWN_MILLIS = 3 * 60 * 60 * 1000L
        const val DAILY_COUNT_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000L
    }
}

private fun UnlockLogEntity.toUnlockLogEntry() = UnlockLogEntry(
    timestamp = timestamp,
    reason = reason,
    action = action,
)
