package com.anandsundaram.myforest.data

interface FocusPreferences {
    fun getDurationMinutes(defaultValue: Float = 50f): Float
    fun setDurationMinutes(value: Float)
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(isCompleted: Boolean)
}
