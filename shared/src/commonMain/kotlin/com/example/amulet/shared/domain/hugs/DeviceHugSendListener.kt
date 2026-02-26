package com.example.amulet.shared.domain.hugs

import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.hugs.model.GestureType
import com.example.amulet.shared.domain.hugs.model.PairStatus
import com.example.amulet.shared.domain.hugs.model.PairId
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.hugs.SendQuickReplyByGestureUseCase
import com.example.amulet.shared.domain.hugs.ObservePairsUseCase
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
            deviceControlRepository.observeNotifications(NotificationType.STATUS)
                .collect { message ->
                    if (message == "NOTIFY:STATUS:HUG_SEND") {
                        try {
                            handleHugSend()
                        } catch (_: Throwable) {
                        }
                    }
                }
        }
    }

    private suspend fun handleHugSend() {
        val currentUser = observeCurrentUserUseCase().first() ?: return

        val pairs = observePairsUseCase().first()
        val activePair = pairs.firstOrNull { it.status == PairStatus.ACTIVE } ?: pairs.firstOrNull() ?: return

        val fromUserId: UserId = currentUser.id
        val toUserId: UserId? = activePair.members.firstOrNull { it.userId != fromUserId }?.userId
        val pairId: PairId = activePair.id

        sendQuickReplyByGestureUseCase(
            pairId = pairId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            gestureType = GestureType.DOUBLE_TAP,
        )
    }
}
