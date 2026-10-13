package it.gymlog.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.SessionTime
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.domain.activityLevels
import it.gymlog.app.domain.localDate
import it.gymlog.app.domain.weekStart
import it.gymlog.app.domain.weekStreak
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class StatsState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.MIN,
    val total: Int = 0,
    val thisMonth: Int = 0,
    val streak: Int = 0,
    val setsThisWeek: Int = 0,
    val levels: Map<LocalDate, Int> = emptyMap(),
    val recent: List<SessionTime> = emptyList(),
)

class StatsViewModel(
    private val workouts: WorkoutRepository,
    private val today: () -> LocalDate = LocalDate::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val day = MutableStateFlow(today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StatsState> = day.flatMapLatest { now ->
        val weekFrom = weekStart(now).atStartOfDay(zone).toInstant().toEpochMilli()
        combine(workouts.observeFinishedSessionTimes(), workouts.countWorkingSetsSince(weekFrom)) { sessions, setsWeek ->
            val dates = sessions.map { localDate(it.startedAt, zone) }
            StatsState(
                loading = false,
                today = now,
                total = sessions.size,
                thisMonth = dates.count { it.year == now.year && it.month == now.month },
                streak = weekStreak(dates.toSet(), now),
                setsThisWeek = setsWeek,
                levels = activityLevels(sessions, now, zone),
                recent = sessions.take(5),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsState())

    /** Da chiamare quando la schermata torna visibile: ricalcola con la data di oggi. */
    fun refreshDay() { day.value = today() }
}
