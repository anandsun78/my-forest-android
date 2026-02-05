package com.anandsundaram.myforest.ui

import com.anandsundaram.myforest.FocusSession

data class FocusUiState(
    val durationMinutes: Float = 25f,
    val isTimerRunning: Boolean = false,
    val remainingTimeMs: Long = 0L,
    val growth: Float = 0f,
    val sessionDurationMs: Long = 0L,
    val history: List<FocusSession> = emptyList()
)
