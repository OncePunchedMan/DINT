package com.example.doineedto.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_logs")
data class UnlockLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val reason: String,
    val action: String,
)
