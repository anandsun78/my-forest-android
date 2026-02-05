package com.anandsundaram.myforest.data

interface FocusPreferences {
    fun getDurationMinutes(defaultValue: Float = 25f): Float
    fun setDurationMinutes(value: Float)
}
