package com.anandsundaram.myforest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anandsundaram.myforest.FocusSession
import com.anandsundaram.myforest.data.FocusPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class FocusViewModel(
    private val preferences: FocusPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(
        FocusUiState(durationMinutes = preferences.getDurationMinutes())
    )
    val state: StateFlow<FocusUiState> = _state.asStateFlow()

    private val _events = Channel<FocusEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onDurationChange(value: Float) {
        preferences.setDurationMinutes(value)
        _state.update { current -> current.copy(durationMinutes = value) }
    }

    fun onPlantClicked() {
        val durationMs = (_state.value.durationMinutes * 60_000).toLong()
        val updated = _state.value.copy(
            isTimerRunning = true,
            remainingTimeMs = durationMs,
            sessionDurationMs = durationMs,
            growth = 0f
        )
        _state.value = updated
        emitEvent(FocusEvent.StartService(durationMs))
    }

    fun onStopRequested() {
        emitEvent(FocusEvent.StopService)
        completeSession(isSuccess = false)
    }

    fun onTimerTick(remainingMs: Long) {
        val sessionDuration = _state.value.sessionDurationMs
        val progress = if (sessionDuration > 0) {
            1f - (remainingMs.toFloat() / sessionDuration)
        } else {
            0f
        }
        _state.update {
            it.copy(
                remainingTimeMs = remainingMs,
                growth = progress.coerceIn(0f, 1f)
            )
        }
    }

    fun onTimerFinished() {
        completeSession(isSuccess = true)
    }

    private fun completeSession(isSuccess: Boolean) {
        val current = _state.value
        val elapsedMs = (current.sessionDurationMs - current.remainingTimeMs).coerceAtLeast(0L)
        val actualMinutes = (elapsedMs / 60_000).toInt()
        val sessionMinutes = (current.sessionDurationMs / 60_000).toInt()

        val updatedHistory = current.history + FocusSession(
            date = Date(),
            durationMinutes = sessionMinutes,
            actualDurationMinutes = actualMinutes,
            isSuccess = isSuccess
        )

        _state.value = current.copy(
            isTimerRunning = false,
            remainingTimeMs = 0L,
            growth = 0f,
            sessionDurationMs = 0L,
            history = updatedHistory
        )
    }

    private fun emitEvent(event: FocusEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    sealed interface FocusEvent {
        data class StartService(val durationMs: Long) : FocusEvent
        data object StopService : FocusEvent
    }

    class Factory(private val preferences: FocusPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(preferences) as T
        }
    }
}
