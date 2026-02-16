package com.anandsundaram.myforest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anandsundaram.myforest.data.FocusPreferences
import com.anandsundaram.myforest.data.FocusRepository
import com.anandsundaram.myforest.data.FocusSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

class FocusViewModel(
    private val preferences: FocusPreferences,
    private val repository: FocusRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        FocusUiState(durationMinutes = preferences.getDurationMinutes())
    )
    val state: StateFlow<FocusUiState> = _state.asStateFlow()

    private val _events = Channel<FocusEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            repository.observeSessions().collectLatest { sessions ->
                val dailyStats = buildDailyStats(sessions)
                val totalMinutes = sessions.sumOf { it.actualDurationMinutes }
                val focusedDays = dailyStats.count { it.totalMinutes > 0 }

                _state.update { current ->
                    current.copy(
                        dailyStats = dailyStats,
                        totalMinutesAllTime = totalMinutes,
                        focusedDays = focusedDays
                    )
                }
            }
        }
    }

    fun onDurationChange(value: Float) {
        val snapped = (value / STEP_MINUTES).roundToInt() * STEP_MINUTES
        val clamped = snapped.coerceIn(MIN_MINUTES, MAX_MINUTES)
        preferences.setDurationMinutes(clamped)
        _state.update { current -> current.copy(durationMinutes = clamped) }
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
        val sessionMinutes = (current.sessionDurationMs / 60_000).toInt()
        val actualMinutes = if (isSuccess) {
            sessionMinutes
        } else {
            (elapsedMs / 60_000).toInt().coerceAtMost(sessionMinutes)
        }

        val session = FocusSessionEntity(
            startTimestamp = System.currentTimeMillis(),
            durationMinutes = sessionMinutes,
            actualDurationMinutes = actualMinutes,
            isSuccess = isSuccess
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSession(session)
        }

        _state.value = current.copy(
            isTimerRunning = false,
            remainingTimeMs = 0L,
            growth = 0f,
            sessionDurationMs = 0L
        )
    }

    private fun emitEvent(event: FocusEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    private fun buildDailyStats(sessions: List<FocusSessionEntity>): List<DailyFocusStat> {
        val zoneId = ZoneId.systemDefault()
        val byDay = sessions.groupBy { session ->
            Instant.ofEpochMilli(session.startTimestamp)
                .atZone(zoneId)
                .toLocalDate()
        }

        return byDay.entries
            .map { (date, daySessions) ->
                val total = daySessions.sumOf { it.actualDurationMinutes }
                val successMinutes = daySessions.filter { it.isSuccess }
                    .sumOf { it.actualDurationMinutes }
                DailyFocusStat(
                    date = date,
                    totalMinutes = total,
                    sessions = daySessions.size,
                    successMinutes = successMinutes
                )
            }
            .sortedBy { it.date }
    }

    sealed interface FocusEvent {
        data class StartService(val durationMs: Long) : FocusEvent
        data object StopService : FocusEvent
    }

    private companion object {
        private const val MIN_MINUTES = 5f
        private const val MAX_MINUTES = 120f
        private const val STEP_MINUTES = 5f
    }

    class Factory(
        private val preferences: FocusPreferences,
        private val repository: FocusRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(preferences, repository) as T
        }
    }
}
