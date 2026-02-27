package com.example.amulet.feature.hugs.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amulet.shared.domain.hugs.BlockPairUseCase
import com.example.amulet.shared.domain.hugs.DeletePairUseCase
import com.example.amulet.shared.domain.hugs.ObservePairsUseCase
import com.example.amulet.shared.domain.hugs.SetHugsDndEnabledUseCase
import com.example.amulet.shared.domain.hugs.SyncPairsUseCase
import com.example.amulet.shared.domain.hugs.UnblockPairUseCase
import com.example.amulet.shared.domain.hugs.UpdatePairMemberSettingsUseCase
import com.example.amulet.shared.domain.hugs.model.PairId
import com.example.amulet.shared.domain.hugs.model.PairMemberSettings
import com.example.amulet.shared.domain.hugs.model.PairStatus
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.user.usecase.ObserveCurrentUserUseCase
import com.example.amulet.shared.domain.practices.usecase.GetUserPreferencesStreamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HugsSettingsViewModel @Inject constructor(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val observePairsUseCase: ObservePairsUseCase,
    private val getUserPreferencesStreamUseCase: GetUserPreferencesStreamUseCase,
    private val setHugsDndEnabledUseCase: SetHugsDndEnabledUseCase,
    private val updatePairMemberSettingsUseCase: UpdatePairMemberSettingsUseCase,
    private val blockPairUseCase: BlockPairUseCase,
    private val syncPairsUseCase: SyncPairsUseCase,
    private val unblockPairUseCase: UnblockPairUseCase,
    private val deletePairUseCase: DeletePairUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HugsSettingsState())
    val state: StateFlow<HugsSettingsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<HugsSettingsEffect>()
    val effects = _effects.asSharedFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val userFlow = observeCurrentUserUseCase()
            val pairsFlow = observePairsUseCase()
            val prefsFlow = getUserPreferencesStreamUseCase()

            combine(userFlow, pairsFlow, prefsFlow) { user, pairs, prefs ->
                val activePair = pairs.firstOrNull { it.status == PairStatus.ACTIVE } ?: pairs.firstOrNull()
                val memberSettings = activePair
                    ?.members
                    ?.firstOrNull { it.userId == user?.id }
                    ?.settings
                Triple(user, activePair, Pair(prefs.hugsDndEnabled, memberSettings))
            }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = Triple(null, null, Pair(false, null))
                )
                .collect { (user, pair, extra) ->
                    val (globalDnd, memberSettings) = extra
                    _state.update { current ->
                        val shouldApplyMemberSettings = current.isLoading

                        current.copy(
                            isLoading = false,
                            currentUser = user,
                            activePair = pair,
                            globalDndEnabled = globalDnd,
                            isMuted = if (shouldApplyMemberSettings) {
                                memberSettings?.muted ?: false
                            } else {
                                current.isMuted
                            },
                            quietHoursStartMinutes = if (shouldApplyMemberSettings) {
                                memberSettings?.quietHoursStartMinutes
                            } else {
                                current.quietHoursStartMinutes
                            },
                            quietHoursEndMinutes = if (shouldApplyMemberSettings) {
                                memberSettings?.quietHoursEndMinutes
                            } else {
                                current.quietHoursEndMinutes
                            },
                            quietHoursStartText = if (shouldApplyMemberSettings) {
                                memberSettings?.quietHoursStartMinutes?.let { minutesToText(it) } ?: ""
                            } else {
                                current.quietHoursStartText
                            },
                            quietHoursEndText = if (shouldApplyMemberSettings) {
                                memberSettings?.quietHoursEndMinutes?.let { minutesToText(it) } ?: ""
                            } else {
                                current.quietHoursEndText
                            },
                            maxHugsPerHourText = if (shouldApplyMemberSettings) {
                                memberSettings?.maxHugsPerHour?.toString() ?: ""
                            } else {
                                current.maxHugsPerHourText
                            },
                        )
                    }
                }
        }
    }

    private fun deletePair() {
        val current = _state.value
        val pair = current.activePair ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = deletePairUseCase(pair.id)
            val error = result.component2()
            if (error != null) {
                _state.update { it.copy(isSaving = false, error = error) }
                _effects.emit(HugsSettingsEffect.ShowError(error))
            } else {
                _state.update { it.copy(isSaving = false) }
                _effects.emit(HugsSettingsEffect.Close)
            }
        }
    }

    fun onIntent(intent: HugsSettingsIntent) {
        when (intent) {
            HugsSettingsIntent.Refresh -> observeData()
            is HugsSettingsIntent.ToggleGlobalDnd -> toggleGlobalDnd(intent.enabled)
            is HugsSettingsIntent.ChangeQuietStartText -> {
                _state.update { it.copy(quietHoursStartText = intent.value) }
            }
            is HugsSettingsIntent.ChangeQuietEndText -> {
                _state.update { it.copy(quietHoursEndText = intent.value) }
            }
            is HugsSettingsIntent.ChangeMaxHugsPerHour -> {
                _state.update { it.copy(maxHugsPerHourText = intent.value.filter { ch -> ch.isDigit() }) }
            }
            is HugsSettingsIntent.ToggleMuted -> {
                _state.update { it.copy(isMuted = intent.enabled) }
            }
            HugsSettingsIntent.SavePairSettings -> savePairSettings()
            HugsSettingsIntent.DisconnectPair -> disconnectPair()
            HugsSettingsIntent.UnblockPair -> unblockPair()
            HugsSettingsIntent.DeletePair -> deletePair()
        }
    }

    private fun toggleGlobalDnd(enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = setHugsDndEnabledUseCase(enabled)
            val error = result.component2()
            if (error != null) {
                _state.update { it.copy(isSaving = false, error = error) }
                _effects.emit(HugsSettingsEffect.ShowError(error))
            } else {
                _state.update { it.copy(isSaving = false, globalDndEnabled = enabled) }
            }
        }
    }

    private fun savePairSettings() {
        val current = _state.value
        val pair = current.activePair ?: return
        val user = current.currentUser ?: return

        val quietStartMinutes = textToMinutes(current.quietHoursStartText)
        val quietEndMinutes = textToMinutes(current.quietHoursEndText)
        val maxPerHour = current.maxHugsPerHourText.toIntOrNull()

        val settings = PairMemberSettings(
            muted = current.isMuted,
            quietHoursStartMinutes = quietStartMinutes,
            quietHoursEndMinutes = quietEndMinutes,
            maxHugsPerHour = maxPerHour,
        )

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = updatePairMemberSettingsUseCase(
                pairId = PairId(pair.id.value),
                userId = UserId(user.id.value),
                settings = settings,
            )
            val error = result.component2()
            if (error != null) {
                _state.update { it.copy(isSaving = false, error = error) }
                _effects.emit(HugsSettingsEffect.ShowError(error))
            } else {
                _state.update { it.copy(isSaving = false) }
                _effects.emit(HugsSettingsEffect.Close)
            }
        }
    }

    private fun disconnectPair() {
        val current = _state.value
        val pair = current.activePair ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = blockPairUseCase(pair.id)
            val error = result.component2()
            if (error != null) {
                _state.update { it.copy(isSaving = false, error = error) }
                _effects.emit(HugsSettingsEffect.ShowError(error))
            } else {
                // После успешной отвязки пары синхронизируем список пар,
                // чтобы локальная БД и UI сразу отразили изменения.
                syncPairsUseCase()
                _state.update { it.copy(isSaving = false) }
                _effects.emit(HugsSettingsEffect.Close)
            }
        }
    }

    private fun unblockPair() {
        val current = _state.value
        val pair = current.activePair ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = unblockPairUseCase(pair.id)
            val error = result.component2()
            if (error != null) {
                _state.update { it.copy(isSaving = false, error = error) }
                _effects.emit(HugsSettingsEffect.ShowError(error))
            } else {
                syncPairsUseCase()
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun minutesToText(totalMinutes: Int): String {
        val h = (totalMinutes / 60).coerceIn(0, 23)
        val m = (totalMinutes % 60).coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }

    private fun textToMinutes(text: String): Int? {
        val parts = text.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
