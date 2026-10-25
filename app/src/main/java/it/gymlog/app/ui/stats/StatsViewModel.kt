package it.gymlog.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.SessionTime
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.data.settings.Settings
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.domain.ExerciseProgress
import it.gymlog.app.domain.PersonalRecord
import it.gymlog.app.domain.activityLevels
import it.gymlog.app.domain.exerciseProgress
import it.gymlog.app.domain.latestRecords
import it.gymlog.app.domain.localDate
import it.gymlog.app.domain.weekStart
import it.gymlog.app.domain.weekStreak
import it.gymlog.app.domain.weeklyMuscleSets
import it.gymlog.app.domain.weeklyVolumeSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    val progress: List<ExerciseProgress> = emptyList(),
    val weeklyVolume: List<Pair<LocalDate, Double>> = emptyList(),
    val muscleSets: List<Pair<Muscle, Int>> = emptyList(),
    val records: List<PersonalRecord> = emptyList(),
)

class StatsViewModel(
    private val workouts: WorkoutRepository,
    private val settings: SettingsRepository? = null,
    private val today: () -> LocalDate = LocalDate::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val day = MutableStateFlow(today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StatsState> = day.flatMapLatest { now ->
        val weekFrom = weekStart(now).atStartOfDay(zone).toInstant().toEpochMilli()
        val settingsFlow = settings?.settings ?: flowOf(Settings())
        combine(
            workouts.observeFinishedSessionTimes(),
            workouts.countWorkingSetsSince(weekFrom),
            workouts.observeStatSets(),
            settingsFlow,
        ) { sessions, setsWeek, sets, current ->
            val dates = sessions.map { localDate(it.startedAt, zone) }
            val formula = current.oneRmFormula
            StatsState(
                loading = false,
                today = now,
                total = sessions.size,
                thisMonth = dates.count { it.year == now.year && it.month == now.month },
                streak = weekStreak(dates.toSet(), now),
                setsThisWeek = setsWeek,
                levels = activityLevels(sessions, now, zone),
                recent = sessions.take(5),
                progress = exerciseProgress(sets, formula, zone),
                weeklyVolume = weeklyVolumeSeries(sets, now, zone),
                muscleSets = weeklyMuscleSets(sets, now, zone),
                records = latestRecords(sets, formula, zone),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsState())

    /** Da chiamare quando la schermata torna visibile: ricalcola con la data di oggi. */
    fun refreshDay() { day.value = today() }
}
