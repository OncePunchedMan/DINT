package com.example.doineedto.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UnlockLogDao {
    @Insert
    suspend fun insert(entry: UnlockLogEntity): Long

    @Update
    suspend fun update(entry: UnlockLogEntity)

    @Query("SELECT * FROM unlock_logs WHERE action = 'pending' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPending(): UnlockLogEntity?

    @Query("DELETE FROM unlock_logs WHERE action = 'pending'")
    suspend fun clearPending()

    @Query("SELECT * FROM unlock_logs WHERE action != 'pending' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<UnlockLogEntity>

    @Query("SELECT * FROM unlock_logs WHERE action != 'pending' ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<UnlockLogEntity>

    @Query("SELECT * FROM unlock_logs WHERE action != 'pending' AND timestamp >= :sinceMillis ORDER BY timestamp DESC")
    suspend fun getSince(sinceMillis: Long): List<UnlockLogEntity>

    @Query("SELECT COUNT(*) FROM unlock_logs WHERE action != 'pending'")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM unlock_logs WHERE action = 'continue'")
    suspend fun countContinued(): Int

    @Query("SELECT COUNT(*) FROM unlock_logs WHERE action = 'keep_locked'")
    suspend fun countKeptLocked(): Int
}
