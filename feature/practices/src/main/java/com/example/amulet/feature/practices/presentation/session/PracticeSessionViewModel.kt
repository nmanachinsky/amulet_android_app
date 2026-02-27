package com.example.amulet.feature.practices.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amulet.core.foreground.PracticeForegroundLauncher
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.devices.model.DeviceSessionStatus
import com.example.amulet.shared.domain.devices.usecase.ObserveDeviceSessionStatusUseCase
import com.example.amulet.shared.domain.practices.PracticeSessionManager
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.patterns.usecase.GetPatternByIdUseCase
import com.example.amulet.shared.domain.practices.usecase.GetPracticeByIdUseCase
import com.example.amulet.shared.domain.practices.usecase.GetUserPreferencesStreamUseCase
import com.example.amulet.shared.domain.practices.usecase.UpdateSessionFeedbackUseCase
import com.example.amulet.shared.domain.practices.usecase.UpdateSessionMoodBeforeUseCase
import com.example.amulet.shared.domain.practices.usecase.UpdateUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PracticeSessionViewModel @Inject constructor(
    private val practiceSessionManager: PracticeSessionManager,
    private val practiceForegroundLauncher: PracticeForegroundLauncher,
    private val getPracticeByIdUseCase: GetPracticeByIdUseCase,
    private val observeDeviceSessionStatusUseCase: ObserveDeviceSessionStatusUseCase,
    private val getUserPreferencesStreamUseCase: GetUserPreferencesStreamUseCase,
    private val updateUserPreferencesUseCase: UpdateUserPreferencesUseCase,
    private val updateSessionFeedbackUseCase: UpdateSessionFeedbackUseCase,
    private val updateSessionMoodBeforeUseCase: UpdateSessionMoodBeforeUseCase,
    private val getPatternByIdUseCase: GetPatternByIdUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PracticeSessionState())
    val state: StateFlow<PracticeSessionState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PracticeSessionEffect>()
    val effects = _effects.asSharedFlow()

    init {
        observeSession()
    }

    fun setPracticeIdIfEmpty(id: String) {
        if (_state.value.practiceId == null) {
            _state.update { it.copy(practiceId = id) }

            viewModelScope.launch {
                val practice = getPracticeByIdUseCase(id).filterNotNull().firstOrNull()
                if (practice != null) {
                    _state.update { current ->
                        if (current.practice != null) {
                            current
                        } else {
                            current.copy(
                                practice = practice,
                                title = practice.title,
                            )
                        }
                    }

                    val patternId = practice.patternId
                    if (patternId != null) {
                        val pattern = getPatternByIdUseCase(patternId).filterNotNull().firstOrNull()
                        if (pattern != null) {
                            _state.update { current ->
                                if (current.patternName != null) current
                                else current.copy(patternName = pattern.title)
                            }
                        }
                    }
                }
            }
        }
    }

    fun handleIntent(intent: PracticeSessionIntent) {
        Logger.d("handleIntent: $intent", tag = TAG)
        when (intent) {
            PracticeSessionIntent.Start -> start()
            is PracticeSessionIntent.Stop -> stop(intent.completed)
            is PracticeSessionIntent.ChangeBrightness -> changeBrightness(intent.level)
            is PracticeSessionIntent.ChangeAudioMode -> changeAudioMode(intent.mode)
            is PracticeSessionIntent.Rate -> rateSession(intent.rating, intent.note)
            is PracticeSessionIntent.SelectMoodBefore -> selectMoodBefore(intent.mood)
            is PracticeSessionIntent.SelectMoodAfter -> selectMoodAfter(intent.mood)
            is PracticeSessionIntent.ChangeFeedbackNote -> changeFeedbackNote(intent.note)
            PracticeSessionIntent.NavigateBack -> emitEffect(PracticeSessionEffect.NavigateBack)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSession() {
        viewModelScope.launch {
            val sessionAndProgressFlow = combine(
                practiceSessionManager.activeSession,
                practiceSessionManager.progress,
            ) { session, progress ->
                Pair(session, progress)
            }

            val practiceFlow = sessionAndProgressFlow.flatMapLatest { pair ->
                val session = pair.first
                session?.let { getPracticeByIdUseCase(it.practiceId) }
                    ?: kotlinx.coroutines.flow.flowOf(null)
            }

            val patternFlow = practiceFlow.flatMapLatest { practice ->
                val patternId = practice?.patternId
                if (patternId != null) {
                    getPatternByIdUseCase(patternId)
                } else {
                    kotlinx.coroutines.flow.flowOf(null)
                }
            }

            val deviceSessionFlow = observeDeviceSessionStatusUseCase()
            val userPrefsFlow = getUserPreferencesStreamUseCase()

            combine(
                sessionAndProgressFlow,
                practiceFlow,
                deviceSessionFlow,
                userPrefsFlow,
                patternFlow,
            ) { sessionAndProgress, practice, deviceSession, prefs, pattern ->
                val (session, progress) = sessionAndProgress
                val deviceStatus: DeviceSessionStatus = deviceSession
                val isBleConnected = deviceStatus.connection is com.example.amulet.shared.domain.devices.model.BleConnectionState.Connected
                val previousState = _state.value
                val previousAudioMode = previousState.audioMode
                val previousSession = previousState.session
                val displaySession = session
                    ?: previousSession?.takeIf { it.status == com.example.amulet.shared.domain.practices.model.PracticeSessionStatus.COMPLETED }
                val effectivePractice = practice ?: previousState.practice
                val effectiveTitle = practice?.title ?: previousState.title
                val effectiveType = practice?.type?.name ?: previousState.type
                val effectiveTotalDurationSec =
                    progress?.totalSec ?: practice?.durationSec ?: previousState.totalDurationSec

                _state.update {
                    it.copy(
                        isLoading = false,
                        session = displaySession,
                        progress = progress,
                        currentStepIndex = progress?.currentStepIndex,
                        practice = effectivePractice,
                        title = effectiveTitle,
                        type = effectiveType,
                        totalDurationSec = effectiveTotalDurationSec,
                        brightnessLevel = prefs.defaultBrightness,
                        vibrationLevel = prefs.defaultIntensity,
                        audioMode = previousAudioMode ?: prefs.defaultAudioMode ?: session?.audioMode,
                        connectionState = deviceStatus.connection,
                        batteryLevel = if (isBleConnected) deviceStatus.liveStatus?.batteryLevel else null,
                        isCharging = if (isBleConnected) deviceStatus.liveStatus?.isCharging ?: false else false,
                        isDeviceOnline = deviceStatus.liveStatus?.isOnline ?: false,
                        patternName = pattern?.title ?: previousState.patternName,
                    )
                }
            }.collect { }
        }
    }

    private fun start() {
        val current = _state.value.session
        Logger.d("start: requested, currentSession=$current", tag = TAG)
        if (current?.status == com.example.amulet.shared.domain.practices.model.PracticeSessionStatus.ACTIVE) {
            Logger.d("start: session already ACTIVE, ignoring", tag = TAG)
            return
        }

        val id = _state.value.practiceId ?: run {
            Logger.d("start: practiceId is null, abort", tag = TAG)
            return
        }

        // Инверсия контроля: запускаем foreground-сервис МГНОВЕННО,
        // чтобы защитить процесс от убийства ОС при переходе в фон.
        // Тяжелая логика (BLE, паттерны) стартует уже под защитой сервиса.
        practiceForegroundLauncher.ensureServiceStarted()

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isPatternPreloading = true) }
            val currentState = _state.value
            Logger.d(
                "start: calling startSession practiceId=$id intensity=${currentState.vibrationLevel} brightness=${currentState.brightnessLevel}",
                tag = TAG
            )
            val result = practiceSessionManager.startSession(
                practiceId = id,
                initialIntensity = currentState.vibrationLevel,
                initialBrightness = currentState.brightnessLevel,
            )
            val error = result.component2()
            if (error != null) {
                Logger.d("start: startSession failed error=$error", tag = TAG)
                _state.update { it.copy(isLoading = false, isPatternPreloading = false, error = error) }
                emitEffect(PracticeSessionEffect.ShowError(error))
            } else {
                val session = result.component1()
                Logger.d("start: startSession success session=$session", tag = TAG)
                _state.update { it.copy(isLoading = false, isPatternPreloading = false, session = session, error = null) }

                val moodBefore = _state.value.moodBefore
                if (session != null && moodBefore != null) {
                    launch {
                        updateSessionMoodBeforeUseCase(session.id, moodBefore)
                    }
                }
            }
        }
    }

    private fun stop(completed: Boolean) {
        Logger.d("stop: requested completed=$completed", tag = TAG)
        viewModelScope.launch {
            val result = practiceSessionManager.stopSession(completed)
            val error = result.component2()
            if (error != null && error != AppError.NotFound) {
                Logger.d("stop: stopSession failed error=$error", tag = TAG)
                emitEffect(PracticeSessionEffect.ShowError(error))
            } else {
                if (error == AppError.NotFound) {
                    Logger.d("stop: stopSession returned NotFound (already stopped), treating as success", tag = TAG)
                }
                Logger.d("stop: stopSession success completed=$completed", tag = TAG)
                if (!completed) {
                    _state.update { it.copy(session = null, progress = null, currentStepIndex = null) }
                    emitEffect(PracticeSessionEffect.NavigateBack)
                } else {
                    // При успешном завершении остаёмся на экране,
                    // чтобы показать финальный блок с вопросом о настроении.
                    val session = result.component1()
                    if (session != null) {
                        _state.update { it.copy(session = session) }
                    }
                }
            }
        }
    }

    private fun changeBrightness(level: Double) {
        _state.update { it.copy(brightnessLevel = level) }
        viewModelScope.launch {
            val current = getUserPreferencesStreamUseCase().firstOrNull()
            val updated = current?.copy(defaultBrightness = level)
            if (updated != null) {
                updateUserPreferencesUseCase(updated)
            }
        }
    }

    private fun changeAudioMode(mode: com.example.amulet.shared.domain.practices.model.PracticeAudioMode) {
        _state.update { it.copy(audioMode = mode) }
        viewModelScope.launch {
            val current = getUserPreferencesStreamUseCase().firstOrNull()
            val updated = current?.copy(defaultAudioMode = mode)
            if (updated != null) {
                updateUserPreferencesUseCase(updated)
            }
        }
    }

    private fun selectMoodBefore(mood: MoodKind) {
        _state.update { it.copy(moodBefore = mood) }
    }

    private fun selectMoodAfter(mood: MoodKind) {
        _state.update { it.copy(moodAfter = mood) }
    }

    private fun changeFeedbackNote(note: String) {
        _state.update { it.copy(pendingNote = note) }
    }

    private fun rateSession(rating: Int?, note: String?) {
        val currentSessionId = _state.value.session?.id ?: return
        val currentNote = note ?: _state.value.pendingNote
        val existingMoodAfter = _state.value.moodAfter
        val moodAfter = existingMoodAfter ?: when (rating) {
            1, 2 -> MoodKind.NERVOUS
            3 -> MoodKind.NEUTRAL
            4, 5 -> MoodKind.RELAX
            else -> null
        }
        _state.update { it.copy(pendingRating = rating, pendingNote = currentNote, moodAfter = moodAfter) }
        viewModelScope.launch {
            val result = updateSessionFeedbackUseCase(
                sessionId = currentSessionId,
                rating = rating,
                moodAfter = moodAfter,
                note = currentNote,
            )
            val error = result.component2()
            if (error != null) {
                emitEffect(PracticeSessionEffect.ShowError(error))
            } else {
                emitEffect(PracticeSessionEffect.NavigateBack)
            }
        }
    }

    private fun emitEffect(effect: PracticeSessionEffect) {
        Logger.d("emitEffect: effect=$effect", tag = TAG)
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    companion object {
        private const val TAG = "PracticeSessionViewModel"
    }
}
