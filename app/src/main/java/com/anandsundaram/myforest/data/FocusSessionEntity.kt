package com.anandsundaram.myforest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startTimestamp: Long,
    val durationMinutes: Int,
    val actualDurationMinutes: Int,
    val isSuccess: Boolean
)
