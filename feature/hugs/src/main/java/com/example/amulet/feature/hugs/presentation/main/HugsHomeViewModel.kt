package com.example.amulet.feature.hugs.presentation.main

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amulet.shared.domain.hugs.ObserveHugsForPairUseCase
import com.example.amulet.shared.domain.hugs.ObservePairEmotionsUseCase
import com.example.amulet.shared.domain.hugs.ObservePairsUseCase
import com.example.amulet.shared.domain.hugs.ObservePairQuickRepliesUseCase
import com.example.amulet.shared.domain.hugs.SendHugUseCase
import com.example.amulet.shared.domain.hugs.SyncHugsAndEnsurePatternsUseCase
import com.example.amulet.shared.domain.hugs.SyncPairsAndFetchMemberProfilesUseCase
import com.example.amulet.shared.domain.hugs.UnblockPairUseCase
import com.example.amulet.shared.domain.hugs.UpdatePairQuickRepliesUseCase
import com.example.amulet.shared.domain.hugs.model.GestureType
import com.example.amulet.shared.domain.hugs.model.Emotion
import com.example.amulet.shared.domain.hugs.model.PairQuickReply
import com.example.amulet.shared.domain.hugs.model.PairStatus
import com.example.amulet.shared.domain.practices.usecase.GetUserPreferencesStreamUseCase
import com.example.amulet.shared.domain.practices.usecase.UpdateUserPreferencesUseCase
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.user.model.UserPreferences
import com.example.amulet.shared.domain.user.usecase.FetchUserProfileUseCase
import com.example.amulet.shared.domain.user.usecase.ObserveCurrentUserUseCase
import com.example.amulet.shared.domain.user.usecase.ObserveUserByIdUseCase
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.logging.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

@HiltViewModel
class HugsHomeViewModel @Inject constructor(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val observePairsUseCase: ObservePairsUseCase,
    private val observeHugsForPairUseCase: ObserveHugsForPairUseCase,
    private val observePairEmotionsUseCase: ObservePairEmotionsUseCase,
    private val observePairQuickRepliesUseCase: ObservePairQuickRepliesUseCase,
    private val updatePairQuickRepliesUseCase: UpdatePairQuickRepliesUseCase,
    private val sendHugUseCase: SendHugUseCase,
    private val syncHugsAndEnsurePatternsUseCase: SyncHugsAndEnsurePatternsUseCase,
    private val syncPairsAndFetchMemberProfilesUseCase: SyncPairsAndFetchMemberProfilesUseCase,
    private val unblockPairUseCase: UnblockPairUseCase,
    private val observeUserByIdUseCase: ObserveUserByIdUseCase,
    private val fetchUserProfileUseCase: FetchUserProfileUseCase,
    private val getUserPreferencesStreamUseCase: GetUserPreferencesStreamUseCase,
    private val updateUserPreferencesUseCase: UpdateUserPreferencesUseCase,
) : ViewModel() {

    private companion object {
        private const val SYNC_TIMEOUT_MS: Long = 15_000
        private const val AUTO_SYNC_MIN_INTERVAL_MS: Long = 60_000

        private val autoSyncMutex = Mutex()
        private var lastAutoSyncElapsedMs: Long = 0L
        private var isAutoSyncInProgress: Boolean = false
    }

    private val _state = MutableStateFlow(HugsHomeState())
    val state: StateFlow<HugsHomeState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<HugsHomeEffect>()
    val effects = _effects.asSharedFlow()

    init {
        observeData()
        observeUserPreferences()
    }

    private val requestedPartnerUserIds = mutableSetOf<String>()

    private var lastUserPreferences: UserPreferences = UserPreferences()

    private var lastLoggedDbSnapshot: String? = null

    private var lastLoggedCurrentUserId: String? = null

    private fun observeData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val userFlow = observeCurrentUserUseCase()
            val pairsFlow = observePairsUseCase()

            userFlow
                .combine(pairsFlow) { user, pairs ->
                    val activePair = pairs.firstOrNull { it.status == PairStatus.ACTIVE } ?: pairs.firstOrNull()
                    BaseData(user, activePair)
                }
                .flatMapLatest { base ->
                    val pair = base.pair
                    val user = base.user

                    if (pair == null || user == null) {
                        flowOf(FullData(base, partnerUser = null, hugs = emptyList(), emotions = emptyList(), quickReplies = emptyList()))
                    } else {
                        val partnerUserId = pair.members
                            .map { it.userId }
                            .firstOrNull { it != user.id }

                        val hugsFlow = observeHugsForPairUseCase(pair.id)
                        val emotionsFlow = observePairEmotionsUseCase(pair.id)
                        val quickRepliesFlow = observePairQuickRepliesUseCase(pair.id, user.id)
                        val partnerUserFlow = if (partnerUserId != null) {
                            observeUserByIdUseCase(partnerUserId)
                        } else {
                            flowOf(null)
                        }

                        combine(hugsFlow, emotionsFlow, quickRepliesFlow, partnerUserFlow) { hugs, emotions, quickReplies, partnerUser ->
                            FullData(base, partnerUser, hugs, emotions, quickReplies)
                        }
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = FullData(
                        base = BaseData(null, null),
                        partnerUser = null,
                        hugs = emptyList(),
                        emotions = emptyList(),
                        quickReplies = emptyList()
                    )
                )
                .collect { data ->
                    val pairId = data.base.pair?.id?.value
                    val currentUserId = data.base.user?.id?.value
                    val emotionsIds = data.emotions.map { it.id }.toSet()
                    val quickRepliesSnapshot = data.quickReplies
                        .sortedBy { it.gestureType }
                        .joinToString { "${it.gestureType}:${it.emotionId ?: "null"}" }

                    if (currentUserId != lastLoggedCurrentUserId) {
                        lastLoggedCurrentUserId = currentUserId
                        Logger.d(
                            "HugsHomeViewModel.observeData: currentUserId changed -> $currentUserId",
                            "HugsHomeViewModel"
                        )
                    }

                    val snapshot = "pairId=$pairId currentUserId=$currentUserId emotionsCount=${data.emotions.size} quickReplies=[$quickRepliesSnapshot]"
                    if (snapshot != lastLoggedDbSnapshot) {
                        lastLoggedDbSnapshot = snapshot
                        Logger.d(
                            "HugsHomeViewModel.observeData: db snapshot $snapshot",
                            "HugsHomeViewModel"
                        )

                        val missingIds = data.quickReplies
                            .mapNotNull { it.emotionId }
                            .filterNot { emotionsIds.contains(it) }
                            .distinct()
                        if (missingIds.isNotEmpty()) {
                            Logger.e(
                                "HugsHomeViewModel.observeData: quickReplies reference missing emotions pairId=$pairId missingEmotionIds=${missingIds.joinToString()}",
                                tag = "HugsHomeViewModel"
                            )
                        }
                    }

                    val pair = data.base.pair
                    val user = data.base.user
                    if (pair != null && user != null) {
                        val partnerUserId = pair.members
                            .map { it.userId }
                            .firstOrNull { it != user.id }

                        if (partnerUserId == null) {
                            Logger.d(
                                "HugsHomeViewModel.observeData: partnerUserId not resolved pairId=${pair.id.value} currentUserId=${user.id.value} members=${pair.members.map { it.userId.value }}",
                                "HugsHomeViewModel"
                            )
                        }
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentUser = data.base.user,
                            activePair = data.base.pair,
                            partnerUser = data.partnerUser,
                            hugs = data.hugs.sortedByDescending { hug -> hug.createdAt }.take(15),
                            pairEmotions = data.emotions.sortedBy { it.order },
                            quickReplies = data.quickReplies,
                        )
                    }

                    val partnerUserId = data.base.pair?.members
                        ?.map { it.userId }
                        ?.firstOrNull { it != data.base.user?.id }

                    if (partnerUserId != null && data.partnerUser == null &&
                        requestedPartnerUserIds.add(partnerUserId.value)
                    ) {
                        viewModelScope.launch {
                            fetchUserProfileUseCase(UserId(partnerUserId.value))
                        }
                    }
                }
        }
    }

    private fun syncOnEnter() {
        viewModelScope.launch {
            val shouldRunAutoSync = autoSyncMutex.withLock {
                val now = SystemClock.elapsedRealtime()
                val isRateLimited = now - lastAutoSyncElapsedMs < AUTO_SYNC_MIN_INTERVAL_MS

                if (isAutoSyncInProgress || isRateLimited) {
                    false
                } else {
                    isAutoSyncInProgress = true
                    lastAutoSyncElapsedMs = now
                    true
                }
            }
            if (!shouldRunAutoSync) return@launch

            if (_state.value.isRefreshing || _state.value.isSyncingOnEnter) return@launch
            _state.update { it.copy(isSyncingOnEnter = true, error = null) }
            try {
                syncAllWithTimeout()
            } finally {
                _state.update { it.copy(isSyncingOnEnter = false) }
                autoSyncMutex.withLock {
                    isAutoSyncInProgress = false
                }
            }
        }
    }

    fun onIntent(intent: HugsHomeIntent) {
        when (intent) {
            HugsHomeIntent.OnEnter -> syncOnEnter()
            HugsHomeIntent.Refresh -> refresh()
            HugsHomeIntent.SendHug -> sendHug()
            HugsHomeIntent.OpenDefaultEmotionPicker -> openDefaultEmotionPicker()
            is HugsHomeIntent.OpenQuickReplyPicker -> openQuickReplyPicker(intent)
            HugsHomeIntent.ClosePairEmotionPicker -> closePairEmotionPicker()
            is HugsHomeIntent.SelectPairEmotion -> selectPairEmotion(intent.emotionId)
            HugsHomeIntent.OpenSettings -> emitEffect(HugsHomeEffect.NavigateToSettings)
            HugsHomeIntent.OpenEmotions -> emitEffect(HugsHomeEffect.NavigateToEmotions)
            HugsHomeIntent.OpenPairing -> emitEffect(HugsHomeEffect.NavigateToPairing)
            HugsHomeIntent.UnblockPair -> unblockPair()
        }
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            getUserPreferencesStreamUseCase()
                .collect { prefs ->
                    lastUserPreferences = prefs
                    _state.update {
                        it.copy(
                            defaultHugEmotionId = prefs.defaultHugEmotionId,
                        )
                    }
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            if (_state.value.isRefreshing || _state.value.isSyncingOnEnter) return@launch
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                syncAllWithTimeout()
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun syncAllWithTimeout() {
        try {
            withTimeout(SYNC_TIMEOUT_MS) {
                syncAll()
            }
        } catch (_: TimeoutCancellationException) {
            _state.update { it.copy(error = AppError.Timeout) }
        }
    }

    private suspend fun syncAll() {
        val pairsResult = syncPairsAndFetchMemberProfilesUseCase()
        val pairsError = pairsResult.component2()
        if (pairsError != null) {
            _state.update { it.copy(error = pairsError) }
            emitEffect(HugsHomeEffect.ShowError(pairsError))
        }

        val hugsResult = syncHugsAndEnsurePatternsUseCase(direction = "all")
        val hugsError = hugsResult.component2()
        if (hugsError != null) {
            _state.update { it.copy(error = hugsError) }
            emitEffect(HugsHomeEffect.ShowError(hugsError))
        }
    }

    private fun sendHug() {
        val pair = _state.value.activePair ?: run {
            Logger.d("HugsHomeViewModel.sendHug: skip (no activePair)", "HugsHomeViewModel")
            return
        }
        if (pair.status == PairStatus.BLOCKED) {
            Logger.d("HugsHomeViewModel.sendHug: skip (pair BLOCKED) pairId=${pair.id.value}", "HugsHomeViewModel")
            return
        }
        val currentUser = _state.value.currentUser ?: run {
            Logger.d("HugsHomeViewModel.sendHug: skip (no currentUser)", "HugsHomeViewModel")
            return
        }

        val defaultEmotionId = _state.value.defaultHugEmotionId
        if (defaultEmotionId.isNullOrBlank()) {
            Logger.d("HugsHomeViewModel.sendHug: open pair emotion picker (no default emotion)", "HugsHomeViewModel")
            _state.update {
                it.copy(
                    isPairEmotionPickerOpen = true,
                    pairEmotionPickerMode = PairEmotionPickerMode.SendHug,
                )
            }
            return
        }

        val pairEmotion = _state.value.pairEmotions.firstOrNull { it.id == defaultEmotionId }
        if (pairEmotion == null) {
            Logger.d("HugsHomeViewModel.sendHug: open pair emotion picker (default emotion missing)", "HugsHomeViewModel")
            _state.update {
                it.copy(
                    isPairEmotionPickerOpen = true,
                    pairEmotionPickerMode = PairEmotionPickerMode.SendHug,
                )
            }
            return
        }

        val emotion = Emotion(
            colorHex = pairEmotion.colorHex,
            patternId = pairEmotion.patternId,
        )

        sendHugInternal(emotion)
    }

    private fun openDefaultEmotionPicker() {
        _state.update {
            it.copy(
                isPairEmotionPickerOpen = true,
                pairEmotionPickerMode = PairEmotionPickerMode.SetDefault,
            )
        }
    }

    private fun openQuickReplyPicker(intent: HugsHomeIntent.OpenQuickReplyPicker) {
        _state.update {
            it.copy(
                isPairEmotionPickerOpen = true,
                pairEmotionPickerMode = PairEmotionPickerMode.SetQuickReply(intent.gestureType),
            )
        }
    }

    private fun closePairEmotionPicker() {
        _state.update { it.copy(isPairEmotionPickerOpen = false, pairEmotionPickerMode = PairEmotionPickerMode.SendHug) }
    }

    private fun selectPairEmotion(emotionId: String?) {
        val mode = _state.value.pairEmotionPickerMode
        _state.update { it.copy(isPairEmotionPickerOpen = false, pairEmotionPickerMode = PairEmotionPickerMode.SendHug) }

        when (mode) {
            PairEmotionPickerMode.SendHug -> {
                val id = emotionId ?: return
                val pairEmotion = _state.value.pairEmotions.firstOrNull { it.id == id } ?: return
                val emotion = Emotion(colorHex = pairEmotion.colorHex, patternId = pairEmotion.patternId)
                sendHugInternal(emotion)
            }
            PairEmotionPickerMode.SetDefault -> {
                viewModelScope.launch {
                    val updated = lastUserPreferences.copy(defaultHugEmotionId = emotionId)
                    updateUserPreferencesUseCase(updated)
                }
            }
            is PairEmotionPickerMode.SetQuickReply -> {
                val pair = _state.value.activePair ?: return
                val currentUser = _state.value.currentUser ?: return
                val gestureType = mode.gestureType

                viewModelScope.launch {
                    val updated = buildList {
                        addAll(_state.value.quickReplies.filterNot { it.gestureType == gestureType })
                        add(
                            PairQuickReply(
                                pairId = pair.id,
                                userId = currentUser.id,
                                gestureType = gestureType,
                                emotionId = emotionId,
                            )
                        )
                    }

                    val result = updatePairQuickRepliesUseCase(
                        pairId = pair.id,
                        userId = currentUser.id,
                        replies = updated,
                    )
                    result.component2()?.let { error ->
                        emitEffect(HugsHomeEffect.ShowError(error))
                    }
                }
            }
        }
    }

    private fun sendHugInternal(emotion: Emotion) {
        val pair = _state.value.activePair ?: return
        if (pair.status == PairStatus.BLOCKED) return

        val currentUser = _state.value.currentUser ?: return
        val toUserId = _state.value.partnerUser?.id
            ?: pair.members.firstOrNull { it.userId != currentUser.id }?.userId
            ?: pair.members.firstOrNull()?.userId?.takeIf { it != currentUser.id }

        if (toUserId == null) {
            Logger.e(
                "HugsHomeViewModel.sendHug: validation error (toUserId is null) pairId=${pair.id.value} currentUserId=${currentUser.id.value} members=${pair.members.map { it.userId.value }}",
                throwable = IllegalStateException("toUserId is null"),
                tag = "HugsHomeViewModel"
            )
            emitEffect(
                HugsHomeEffect.ShowError(
                    AppError.Validation(mapOf("toUserId" to "Partner userId is required to send hug"))
                )
            )
            return
        }

        Logger.d(
            "HugsHomeViewModel.sendHug: start pairId=${pair.id.value} from=${currentUser.id.value} to=${toUserId?.value}",
            "HugsHomeViewModel"
        )

        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            val result = sendHugUseCase(
                pairId = pair.id,
                fromUserId = currentUser.id,
                toUserId = toUserId,
                emotion = emotion,
            )
            val error = result.component2()
            if (error != null) {
                Logger.e(
                    "HugsHomeViewModel.sendHug: failed pairId=${pair.id.value} error=$error",
                    throwable = Exception(error.toString()),
                    tag = "HugsHomeViewModel"
                )
                emitEffect(HugsHomeEffect.ShowError(error))
            } else {
                Logger.d("HugsHomeViewModel.sendHug: success pairId=${pair.id.value}", "HugsHomeViewModel")
            }
            _state.update { it.copy(isSending = false) }
        }
    }

    private fun unblockPair() {
        val pair = _state.value.activePair ?: return

        viewModelScope.launch {
            val result = unblockPairUseCase(pair.id)
            val error = result.component2()
            if (error != null) {
                emitEffect(HugsHomeEffect.ShowError(error))
            } else {
                syncPairsAndFetchMemberProfilesUseCase()
            }
        }
    }

    private fun emitEffect(effect: HugsHomeEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    private data class BaseData(
        val user: com.example.amulet.shared.domain.user.model.User?,
        val pair: com.example.amulet.shared.domain.hugs.model.Pair?,
    )

    private data class FullData(
        val base: BaseData,
        val partnerUser: com.example.amulet.shared.domain.user.model.User?,
        val hugs: List<com.example.amulet.shared.domain.hugs.model.Hug>,
        val emotions: List<com.example.amulet.shared.domain.hugs.model.PairEmotion>,
        val quickReplies: List<com.example.amulet.shared.domain.hugs.model.PairQuickReply>,
    )
}
