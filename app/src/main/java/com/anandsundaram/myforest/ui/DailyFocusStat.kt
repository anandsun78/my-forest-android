package com.anandsundaram.myforest.ui

import java.time.LocalDate

data class DailyFocusStat(
    val date: LocalDate,
    val totalMinutes: Int,
    val sessions: Int,
    val successMinutes: Int
)
