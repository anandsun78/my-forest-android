package com.anandsundaram.myforest.ui

data class FocusUiState(
    val durationMinutes: Float = 50f,
    val isTimerRunning: Boolean = false,
    val remainingTimeMs: Long = 0L,
    val growth: Float = 0f,
    val sessionDurationMs: Long = 0L,
    val dailyStats: List<DailyFocusStat> = emptyList(),
    val totalMinutesAllTime: Int = 0,
    val focusedDays: Int = 0
)
