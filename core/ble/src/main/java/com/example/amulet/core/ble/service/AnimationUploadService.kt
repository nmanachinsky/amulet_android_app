package com.example.amulet.core.ble.service

import com.example.amulet.core.ble.DeviceCommandSender
import com.example.amulet.core.ble.internal.FlowControlManager
import com.example.amulet.core.ble.internal.GattConstants
import com.example.amulet.core.ble.model.AnimationPlan
import com.example.amulet.core.ble.model.BleResult
import com.example.amulet.core.ble.model.UploadProgress
import com.example.amulet.core.ble.model.UploadState
import com.example.amulet.core.ble.model.toBase64
import com.example.amulet.core.ble.model.toCommandString
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.core.ble.model.AmuletCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface AnimationUploadService {
    fun uploadAnimation(plan: AnimationPlan): Flow<UploadProgress>
}

class AnimationUploadServiceImpl @Inject constructor(
    private val commandSender: DeviceCommandSender,
    private val flowControlManager: FlowControlManager
) : AnimationUploadService {
    override fun uploadAnimation(plan: AnimationPlan): Flow<UploadProgress> = flow {
        val totalBytes = plan.payload.size
        val totalChunks = if (totalBytes == 0) 0 else ((totalBytes + GattConstants.ANIMATION_PAYLOAD_CHUNK_SIZE - 1) / GattConstants.ANIMATION_PAYLOAD_CHUNK_SIZE)

        Logger.d("AnimationUploadService: planId=${plan.id} payloadBytes=$totalBytes totalChunks=$totalChunks", TAG)
        emit(UploadProgress(totalChunks, 0, UploadState.Preparing))

        try {
            // BEGIN_PLAN
            val beginPlanParameters = buildList {
                add(plan.id)
                add(plan.totalDurationMs.toString())
                if (plan.isPreview) {
                    add("PREVIEW")
                }
            }
            commandSender.sendCommand(AmuletCommand.Custom(command = "BEGIN_PLAN", parameters = beginPlanParameters))
            flowControlManager.waitForReady()

            emit(UploadProgress(totalChunks, 0, UploadState.Uploading))

            // ADD_SEGMENTS
            val chunks: List<ByteArray> = if (totalBytes == 0) emptyList() else plan.payload.toList().chunked(GattConstants.ANIMATION_PAYLOAD_CHUNK_SIZE).map { it.toByteArray() }
            for ((index, chunk) in chunks.withIndex()) {
                flowControlManager.waitForReady()

                val base64 = chunk.toBase64()
                if (plan.id.contains("_seg_")) {
                    Logger.d(
                        "AnimationUploadService: SEG_DEBUG planId=${plan.id} chunkIndex=${index + 1}/${chunks.size} chunkBytes=${chunk.size} base64Len=${base64.length}",
                        TAG
                    )
                }
                val addSegmentsCommand = AmuletCommand.Custom(
                    command = "ADD_SEGMENTS",
                    parameters = listOf(plan.id, (index + 1).toString(), base64)
                )
                commandSender.sendCommand(addSegmentsCommand)

                emit(UploadProgress(totalChunks, index + 1, UploadState.Uploading))
            }

            // COMMIT_PLAN
            emit(UploadProgress(totalChunks, totalChunks, UploadState.Committing))
            val commitTimeoutMs = (plan.totalDurationMs + GattConstants.COMMAND_TIMEOUT_MS)
                .coerceAtMost(GattConstants.ANIMATION_TIMEOUT_MS)
            Logger.d(
                "AnimationUploadService: COMMIT_PLAN with timeoutMs=$commitTimeoutMs totalDurationMs=${plan.totalDurationMs}",
                TAG
            )
            
            val firstResult = commandSender.sendCommand(
                AmuletCommand.Custom("COMMIT_PLAN", listOf(plan.id))
            )
            
            val finalResult = if (firstResult is BleResult.Error) {
                Logger.w(
                    "AnimationUploadService: COMMIT_PLAN failed on first attempt for planId=${plan.id}, result=$firstResult, retrying once",
                    null,
                    TAG
                )
                emit(UploadProgress(totalChunks, 0, UploadState.Failed(Exception("COMMIT_PLAN failed: $firstResult"))))
                return@flow
            } else {
                firstResult
            }

            if (finalResult is BleResult.Success) {
                emit(UploadProgress(totalChunks, totalChunks, UploadState.Completed))
            }

        } catch (e: Exception) {
            Logger.e("AnimationUploadService: failed for planId=${plan.id}", e, TAG)
            emit(UploadProgress(totalChunks, 0, UploadState.Failed(e)))
        }
    }

    companion object {
        private const val TAG = "AnimationUploadService"
    }
}
