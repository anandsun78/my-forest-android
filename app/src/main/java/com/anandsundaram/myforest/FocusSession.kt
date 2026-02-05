package com.anandsundaram.myforest

import java.util.Date

data class FocusSession(
    val date: Date,
    val durationMinutes: Int,
    val actualDurationMinutes: Int,
    val isSuccess: Boolean
)
