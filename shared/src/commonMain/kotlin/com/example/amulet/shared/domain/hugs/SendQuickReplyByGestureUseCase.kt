package com.example.amulet.shared.domain.hugs

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.hugs.model.GestureType
import com.example.amulet.shared.domain.hugs.model.PairId
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import kotlinx.coroutines.flow.first

/**
 * Use case для отправки quick-reply «объятия» по жесту амулета.
 *
 * Инкапсулирует логику поиска биндинга (pairId + fromUserId + gestureType)
 * и делегирует реальную отправку в [SendHugUseCase].
 */
class SendQuickReplyByGestureUseCase(
    private val pairsRepository: PairsRepository,
    private val sendHug: SendHugUseCase,
) {

    suspend operator fun invoke(
        pairId: PairId,
        fromUserId: UserId,
        toUserId: UserId?,
        gestureType: GestureType,
    ): AppResult<Unit> {
        // Текущие биндинги quick replies для пользователя в паре.
        val replies = pairsRepository.observeQuickReplies(pairId, fromUserId)
            .first()

        Logger.d(replies.toString(), "SendQuickReplyByGestureUseCase")
        Logger.d(gestureType.name, "SendQuickReplyByGestureUseCase")
        val reply = replies.firstOrNull { it.gestureType == gestureType && it.emotionId != null }
            ?: return Err(AppError.Validation(mapOf("gestureType" to "Quick reply is not configured for this gesture")))

        Logger.d("send", "SendQuickReplyByGestureUseCase")
        // Пока что не используем inReplyToHugId, так как контракт sendHug не поддерживает его явно.
        return sendHug(
            pairId = pairId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            emotion = null,
            quickReply = reply,
            payload = null,
        )
    }
}
