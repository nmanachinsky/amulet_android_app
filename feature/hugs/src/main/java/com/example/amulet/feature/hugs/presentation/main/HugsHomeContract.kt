package com.example.amulet.feature.hugs.presentation.main

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.domain.hugs.model.GestureType
import com.example.amulet.shared.domain.hugs.model.Hug
import com.example.amulet.shared.domain.hugs.model.Pair
import com.example.amulet.shared.domain.hugs.model.PairEmotion
import com.example.amulet.shared.domain.hugs.model.PairQuickReply
import com.example.amulet.shared.domain.user.model.User

sealed class PairEmotionPickerMode {
    object SendHug : PairEmotionPickerMode()
    object SetDefault : PairEmotionPickerMode()
    data class SetQuickReply(val gestureType: GestureType) : PairEmotionPickerMode()
}

data class HugsHomeState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val currentUser: User? = null,
    val activePair: Pair? = null,
    val partnerUser: User? = null,
    val hugs: List<Hug> = emptyList(),
    val quickReplies: List<PairQuickReply> = emptyList(),
    val pairEmotions: List<PairEmotion> = emptyList(),
    val isSyncingOnEnter: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val defaultHugEmotionId: String? = null,
    val isPairEmotionPickerOpen: Boolean = false,
    val pairEmotionPickerMode: PairEmotionPickerMode = PairEmotionPickerMode.SendHug,
)

sealed class HugsHomeIntent {
    object OnEnter : HugsHomeIntent()
    object Refresh : HugsHomeIntent()
    object SendHug : HugsHomeIntent()
    object OpenDefaultEmotionPicker : HugsHomeIntent()
    object ClosePairEmotionPicker : HugsHomeIntent()

    data class SelectPairEmotion(
        val emotionId: String?,
    ) : HugsHomeIntent()

    data class OpenQuickReplyPicker(
        val gestureType: GestureType,
    ) : HugsHomeIntent()

    object OpenSettings : HugsHomeIntent()
    object OpenEmotions : HugsHomeIntent()
    object OpenPairing : HugsHomeIntent()
    object UnblockPair : HugsHomeIntent()
}

sealed class HugsHomeEffect {
    object NavigateToSettings : HugsHomeEffect()
    object NavigateToEmotions : HugsHomeEffect()
    object NavigateToPairing : HugsHomeEffect()
    data class ShowError(val error: AppError) : HugsHomeEffect()
}
