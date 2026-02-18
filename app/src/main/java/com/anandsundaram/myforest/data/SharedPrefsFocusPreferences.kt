package com.anandsundaram.myforest.data

import android.content.SharedPreferences

class SharedPrefsFocusPreferences(
    private val sharedPreferences: SharedPreferences
) : FocusPreferences {

    override fun getDurationMinutes(defaultValue: Float): Float {
        val stored = sharedPreferences.all[KEY_DURATION_MINUTES]
        return when (stored) {
            is Float -> stored
            is Int -> stored.toFloat()
            is Long -> stored.toFloat()
            is Double -> stored.toFloat()
            else -> defaultValue
        }
    }

    override fun setDurationMinutes(value: Float) {
        sharedPreferences.edit().putFloat(KEY_DURATION_MINUTES, value).apply()
    }

    override fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    override fun setOnboardingCompleted(isCompleted: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, isCompleted).apply()
    }

    private companion object {
        private const val KEY_DURATION_MINUTES = "durationMinutes"
        private const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"
    }
}
