package com.example.amulet.data.devices.repository

import com.example.amulet.core.ble.model.AmuletCommand
import com.example.amulet.core.ble.model.AnimationPlan
import com.example.amulet.core.ble.model.UploadState
import com.example.amulet.data.devices.datasource.ble.DevicesBleDataSource
import com.example.amulet.data.devices.datasource.local.DevicesLocalDataSource
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.DeviceAnimationPlan
import com.example.amulet.shared.domain.devices.model.DeviceTimelineSegment
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.practices.model.Practice
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class DeviceControlRepositoryImpl @Inject constructor(
    private val bleDataSource: DevicesBleDataSource,
    private val localDataSource: DevicesLocalDataSource
) : DeviceControlRepository {

    override suspend fun setBrightness(deviceId: DeviceId, brightness: Double): AppResult<Unit> {
        val exists = localDataSource.getDeviceById(deviceId.value) != null
        if (!exists) return Err(AppError.NotFound)
        
        val level = (brightness * 255.0).roundToInt().coerceIn(0, 255)
        return sendCustomCommand("SET_BRIGHTNESS:$level")
    }

    override suspend fun setHaptics(deviceId: DeviceId, haptics: Double): AppResult<Unit> {
        val exists = localDataSource.getDeviceById(deviceId.value) != null
        if (!exists) return Err(AppError.NotFound)
        
        val level = (haptics * 255.0).roundToInt().coerceIn(0, 255)
        return sendCustomCommand("SET_VIB_STRENGTH:$level")
    }

    override suspend fun playPattern(patternId: String): AppResult<Unit> {
        return sendCustomCommand("PLAY:$patternId")
    }

    override suspend fun hasPattern(patternId: String): AppResult<Boolean> {
        return sendCustomCommand("HAS_PLAN:$patternId").map { true }
    }

    override fun uploadTimelinePlan(plan: DeviceAnimationPlan, hardwareVersion: Int): Flow<Int> {
        val payload = assemblePayload(plan.segments)
        
        val blePlan = AnimationPlan(
            id = plan.id,
            payload = payload,
            totalDurationMs = plan.totalDurationMs,
            hardwareVersion = hardwareVersion,
            isPreview = plan.isPreview
        )

        return bleDataSource.uploadAnimation(blePlan)
            .map { progress ->
                if (progress.state is UploadState.Failed) {
                    val cause = (progress.state as UploadState.Failed).cause
                    throw cause ?: IllegalStateException("Animation upload failed for planId=${plan.id}")
                }
                progress.percent
            }
    }

    override suspend fun uploadPracticeScript(practice: Practice): AppResult<Unit> {
        val practiceScript = practice.script ?: return Ok(Unit)
        val patternIds = practiceScript.steps
            .sortedBy { it.order }
            .mapNotNull { it.patternId }

        if (patternIds.isEmpty()) return Ok(Unit)

        val beginResult = sendCustomCommand("BEGIN_PRACTICE_SCRIPT:${practice.id}")
        beginResult.onFailure { return Err(it) }

        patternIds.forEachIndexed { index, patternId ->
            val addResult = sendCustomCommand("ADD_PRACTICE_STEP:${practice.id}:${index + 1}:$patternId")
            addResult.onFailure { return Err(it) }
        }

        val commitResult = sendCustomCommand("COMMIT_PRACTICE_SCRIPT:${practice.id}")
        return commitResult.map { }
    }

    override suspend fun hasPracticeScript(practiceId: String): AppResult<Boolean> {
        val result = sendCustomCommand("HAS_PRACTICE_SCRIPT:$practiceId")
        return result.fold(
            success = { Ok(true) },
            failure = { Err(it) }
        )
    }

    override suspend fun playPracticeScript(practiceId: String): AppResult<Unit> {
        return sendCustomCommand("PLAY_PRACTICE_SCRIPT:$practiceId")
    }

    override suspend fun clearDevice(): AppResult<Unit> {
        return sendCustomCommand("CLEAR_ALL")
    }

    override fun observeNotifications(type: NotificationType?): Flow<String> {
        return bleDataSource.observeNotifications(type)
    }

    private suspend fun sendCustomCommand(command: String): AppResult<Unit> {
        val cmd = AmuletCommand.Custom(command)
        return bleDataSource.sendCommand(cmd)
    }

    private fun assemblePayload(segments: List<DeviceTimelineSegment>): ByteArray {
        val byteArrays = segments.map { it.toByteArray() }
        val totalSize = byteArrays.sumOf { it.size }
        val payload = ByteArray(totalSize)
        var offset = 0
        byteArrays.forEach { segmentBytes ->
            segmentBytes.copyInto(
                destination = payload,
                destinationOffset = offset
            )
            offset += segmentBytes.size
        }
        return payload
    }
}
