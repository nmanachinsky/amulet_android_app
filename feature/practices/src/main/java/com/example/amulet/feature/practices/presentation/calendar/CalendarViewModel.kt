package com.example.amulet.feature.practices.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amulet.shared.domain.practices.usecase.GetScheduledSessionsForDateRangeUseCase
import com.example.amulet.shared.domain.practices.usecase.GetPracticeByIdUseCase
import com.example.amulet.shared.domain.practices.usecase.GetScheduleByPracticeIdUseCase
import com.example.amulet.shared.domain.practices.usecase.UpsertPracticeScheduleUseCase
import com.example.amulet.shared.domain.practices.usecase.GetPracticesStreamUseCase
import com.example.amulet.shared.domain.practices.usecase.SkipScheduledSessionUseCase
import com.example.amulet.shared.domain.practices.model.PracticeSchedule
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.isoDayNumber
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getScheduledSessionsForDateRangeUseCase: GetScheduledSessionsForDateRangeUseCase,
    private val getPracticeByIdUseCase: GetPracticeByIdUseCase,
    private val getScheduleByPracticeIdUseCase: GetScheduleByPracticeIdUseCase,
    private val upsertPracticeScheduleUseCase: UpsertPracticeScheduleUseCase,
    private val getPracticesStreamUseCase: GetPracticesStreamUseCase,
    private val skipScheduledSessionUseCase: SkipScheduledSessionUseCase,
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()
    @OptIn(kotlin.time.ExperimentalTime::class)
    private val today = kotlin.time.Clock.System.now().toLocalDateTime(timeZone).date

    private val _state = MutableStateFlow(
        CalendarState(
            selectedDate = today,
            currentMonth = YearMonth(today.year, today.month)
        )
    )
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val _effects = Channel<CalendarEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var plannerExistingScheduleId: String? = null
    private var plannerExistingScheduleCreatedAt: Long? = null
    private var plannerExistingScheduleCourseId: String? = null
    private var plannerExistingSchedulePlanId: String? = null

    init {
        loadSessions()
        observePracticesForPlanner()
    }

    private fun observePracticesForPlanner() {
        viewModelScope.launch {
            getPracticesStreamUseCase(PracticeFilter())
                .collect { practices ->
                    _state.update { it.copy(plannerAvailablePractices = practices) }
                }
        }
    }

    private fun openPlannerGlobal() {
        // Открываем шит без выбранной практики, пользователь выберет её из списка
        plannerExistingScheduleId = null
        plannerExistingScheduleCreatedAt = null
        plannerExistingScheduleCourseId = null
        plannerExistingSchedulePlanId = null
        _state.update {
            it.copy(
                plannerPracticeId = null,
                plannerPracticeTitle = "",
                plannerSelectedDays = setOf(1, 2, 3, 4, 5),
                plannerTimeOfDay = "09:00",
                plannerReminderEnabled = true,
                isPlannerOpen = true,
                isPlannerSaving = false
            )
        }
    }

    private fun openPlanner(practiceId: String) {
        viewModelScope.launch {
            // Сброс локального состояния id расписания
            plannerExistingScheduleId = null
            plannerExistingScheduleCreatedAt = null
            plannerExistingScheduleCourseId = null
            plannerExistingSchedulePlanId = null

            // Подгружаем заголовок практики
            val practice = getPracticeByIdUseCase(practiceId).firstOrNull()

            // Подгружаем существующее расписание, если есть
            val schedule = getScheduleByPracticeIdUseCase(practiceId as PracticeId).firstOrNull()

            plannerExistingScheduleId = schedule?.id
            plannerExistingScheduleCreatedAt = schedule?.createdAt
            plannerExistingScheduleCourseId = schedule?.courseId
            plannerExistingSchedulePlanId = schedule?.planId

            _state.update { state ->
                state.copy(
                    plannerPracticeId = practiceId,
                    plannerPracticeTitle = practice?.title ?: "",
                    plannerSelectedDays = schedule?.daysOfWeek?.toSet() ?: setOf(1, 2, 3, 4, 5),
                    plannerTimeOfDay = schedule?.timeOfDay ?: "09:00",
                    plannerReminderEnabled = schedule?.reminderEnabled ?: true,
                    isPlannerOpen = true,
                    isPlannerSaving = false
                )
            }
        }
    }

    private fun closePlanner() {
        _state.update { it.copy(isPlannerOpen = false, isPlannerSaving = false) }
    }

    private fun plannerToggleDay(day: Int) {
        _state.update { s ->
            val current = s.plannerSelectedDays
            val next = if (day in current) current - day else current + day
            s.copy(plannerSelectedDays = next)
        }
    }

    private fun plannerChangeTime(time: String) {
        _state.update { it.copy(plannerTimeOfDay = time) }
    }

    private fun plannerSetReminder(enabled: Boolean) {
        _state.update { it.copy(plannerReminderEnabled = enabled) }
    }

    private fun plannerSave() {
        val snapshot = _state.value
        val practiceId = snapshot.plannerPracticeId ?: return
        if (snapshot.plannerSelectedDays.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isPlannerSaving = true) }

            val schedule = PracticeSchedule(
                id = plannerExistingScheduleId ?: java.util.UUID.randomUUID().toString(),
                userId = "", // userId берётся на уровне репозитория
                practiceId = practiceId,
                courseId = plannerExistingScheduleCourseId,
                daysOfWeek = snapshot.plannerSelectedDays.toList().sorted(),
                timeOfDay = snapshot.plannerTimeOfDay,
                reminderEnabled = snapshot.plannerReminderEnabled,
                createdAt = plannerExistingScheduleCreatedAt ?: System.currentTimeMillis(),
                planId = plannerExistingSchedulePlanId,
                updatedAt = System.currentTimeMillis()
            )

            val result = upsertPracticeScheduleUseCase(schedule)
            val error = result.component2()
            if (error != null) {
                _state.update { it.copy(isPlannerSaving = false) }
            } else {
                _state.update { it.copy(isPlannerSaving = false, isPlannerOpen = false) }
                // Обновляем сессии, чтобы отобразить изменения в расписании
                loadSessions()
            }
        }
    }

    fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.ChangeViewMode -> changeViewMode(intent.mode)
            is CalendarIntent.SelectDate -> selectDate(intent.date)
            is CalendarIntent.ChangeMonth -> changeMonth(intent.offset)
            is CalendarIntent.OpenSession -> openSession(intent.sessionId)
            is CalendarIntent.StartSession -> startSession(intent.sessionId)
            is CalendarIntent.RescheduleSession -> rescheduleSession(intent.sessionId, intent.newTime)
            is CalendarIntent.CancelSession -> cancelSession(intent.sessionId)
            is CalendarIntent.OpenPlanner -> openPlanner(intent.practiceId)
            CalendarIntent.OpenPlannerGlobal -> openPlannerGlobal()
            is CalendarIntent.PlannerSelectPractice -> openPlanner(intent.practiceId)
            CalendarIntent.ClosePlanner -> closePlanner()
            is CalendarIntent.PlannerToggleDay -> plannerToggleDay(intent.day)
            is CalendarIntent.PlannerChangeTime -> plannerChangeTime(intent.time)
            is CalendarIntent.PlannerSetReminderEnabled -> plannerSetReminder(intent.enabled)
            CalendarIntent.PlannerSave -> plannerSave()
            CalendarIntent.NavigateBack -> navigateBack()
            CalendarIntent.Refresh -> loadSessions()
        }
    }

    private fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
    }

    private fun changeViewMode(mode: com.example.amulet.feature.practices.presentation.calendar.ScheduleViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }

    private fun changeMonth(offset: Int) {
        _state.update {
            val newMonth = it.currentMonth.plus(offset.toLong(), DateTimeUnit.MONTH)
            it.copy(currentMonth = newMonth)
        }
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val month = _state.value.currentMonth
                // Рассчитываем диапазон, который покрывает весь календарный грид.
                // 1) Находим первый и последний день месяца
                val startOfMonth = LocalDate(month.year, month.month, 1)
                val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

                // 2) Сдвигаем начало к понедельнику (или первому дню недели для грида)
                val startDayOfWeek = startOfMonth.dayOfWeek.isoDayNumber // Пн = 1, Вс = 7
                val daysToSubtract = (startDayOfWeek - 1).coerceAtLeast(0)
                val gridStart = startOfMonth.minus(daysToSubtract.toLong(), DateTimeUnit.DAY)

                // 3) Сдвигаем конец к воскресенью
                val endDayOfWeek = endOfMonth.dayOfWeek.isoDayNumber
                val daysToAdd = (7 - endDayOfWeek).coerceAtLeast(0)
                val gridEnd = endOfMonth.plus(daysToAdd.toLong(), DateTimeUnit.DAY)

                getScheduledSessionsForDateRangeUseCase(gridStart, gridEnd)
                    .collect { sessions ->
                        _state.update { it.copy(sessions = sessions, isLoading = false) }
                    }
            } catch (e: Exception) {
                // Handle error
                _state.update { it.copy(isLoading = false) } // Set error state if needed
            }
        }
    }

    private fun openSession(sessionId: String) {
        val session = _state.value.sessions.find { it.id == sessionId }
        val courseId = session?.courseId
        if (courseId != null) {
            _effects.trySend(CalendarEffect.NavigateToCourse(courseId))
        } else {
            session?.practiceId?.let { practiceId ->
                _effects.trySend(CalendarEffect.NavigateToPractice(practiceId))
            }
        }
    }

    private fun startSession(sessionId: String) {
        // Больше не стартуем сессию автоматически из расписания.
        // Пользователь попадает на экран сессии и запускает практику вручную.
        openSession(sessionId)
    }

    private fun rescheduleSession(sessionId: String, newTime: Long) {
        // По требованию UX: "перенести" просто открывает планировщик для этой практики,
        // чтобы пользователь перенастроил расписание целиком.
        val session = _state.value.sessions.find { it.id == sessionId } ?: return
        openPlanner(session.practiceId)
    }

    private fun cancelSession(sessionId: String) {
        viewModelScope.launch {
            val session = _state.value.sessions.find { it.id == sessionId } ?: return@launch

            val result = skipScheduledSessionUseCase(session)
            val error = result.component2()
            if (error == null) {
                // Перечитываем сессии, чтобы отражать пропуск
                loadSessions()
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effects.send(CalendarEffect.NavigateBack)
        }
    }
}
