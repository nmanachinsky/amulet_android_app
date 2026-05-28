package com.example.amulet.shared.domain.hugs

import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.hugs.model.GestureType
import com.example.amulet.shared.domain.hugs.model.PairId
import com.example.amulet.shared.domain.hugs.model.PairStatus
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.user.usecase.ObserveCurrentUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DeviceHugSendListener(
    private val deviceControlRepository: DeviceControlRepository,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val observePairsUseCase: ObservePairsUseCase,
    private val sendQuickReplyByGestureUseCase: SendQuickReplyByGestureUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            // Слушаем только статусные уведомления (начинаются с NOTIFY:STATUS:)
            deviceControlRepository.observeNotifications(NotificationType.STATUS)
                .collect { message ->
                    Logger.d(message, TAG)

                    // Точное совпадение с тем, что шлет прошивка (bleManager.notifyStatus("HUG_SENDING"))
                    if (message == "NOTIFY:STATUS:HUG_SENDING") {
                        Logger.d("Received HUG_SENDING notification, handling hug send", TAG)
                        try {
                            handleHugSend()
                        } catch (e: Throwable) {
                            Logger.e("Error handling hug send", e, TAG)
                        }
                    }
                }
        }
    }

    private suspend fun handleHugSend() {
        Logger.d("handleHugSend: fetching current user and pairs")
        val currentUser = observeCurrentUserUseCase().first() ?: run {
            Logger.w("handleHugSend: no current user found", null,TAG)
            return
        }
        val pairs = observePairsUseCase().first()

        // Ищем активную пару
        val activePair = pairs.firstOrNull { it.status == PairStatus.ACTIVE } ?: pairs.firstOrNull() ?: run {
            Logger.w("handleHugSend: no active pair found", null,TAG)
            return
        }

        val fromUserId: UserId = currentUser.id
        val toUserId: UserId? = activePair.members.firstOrNull { it.userId != fromUserId }?.userId
        val pairId: PairId = activePair.id

        Logger.d("handleHugSend: sending quick reply pairId=$pairId fromUserId=$fromUserId toUserId=$toUserId", TAG)
        // Отправляем быстрое объятие, привязанное к долгому нажатию (как в прошивке)
        sendQuickReplyByGestureUseCase(
            pairId = pairId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            gestureType = GestureType.LONG_PRESS, // Исправлено с DOUBLE_TAP на LONG_PRESS
        )
    }
    companion object {
        const val TAG = "DeviceHugSendListener"
    }
}