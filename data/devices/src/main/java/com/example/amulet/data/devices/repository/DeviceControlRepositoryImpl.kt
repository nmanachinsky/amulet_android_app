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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
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

        val beginResult = sendCustomCommand("BEGIN_PRACTICE:${practice.id}")
        beginResult.onFailure { return Err(it) }

        patternIds.forEachIndexed { index, patternId ->
            val addResult = sendCustomCommand("ADD_PRACTICE_STEP:${practice.id}:${index + 1}:$patternId")
            addResult.onFailure { return Err(it) }
        }

        val commitResult = sendCustomCommand("COMMIT_PRACTICE:${practice.id}")
        return commitResult.map { }
    }

    override suspend fun hasPracticeScript(practiceId: String): AppResult<Boolean> {
        val result = sendCustomCommand("HAS_PRACTICE:$practiceId")
        return result.fold(
            success = { Ok(true) },
            failure = { Err(it) }
        )
    }

    override suspend fun playPracticeScript(practiceId: String): AppResult<Unit> {
        return sendCustomCommand("PLAY_PRACTICE:$practiceId")
    }

    override suspend fun clearDevice(): AppResult<Unit> {
        return sendCustomCommand("CLEAR_ALL")
    }

    override suspend fun awaitPlaybackStarted(patternId: String, timeoutMs: Long): AppResult<Unit> {
        return try {
            withTimeout(timeoutMs) {
                observeNotifications(NotificationType.PATTERN)
                    .first { it.startsWith("NOTIFY:PATTERN:STARTED:$patternId") }
                Ok(Unit)
            }
        } catch (e: TimeoutCancellationException) {
            Err(AppError.BleError.CommandTimeout("Playback start timeout for pattern: $patternId"))
        } catch (e: Exception) {
            Err(AppError.Unknown)
        }
    }

    override fun observeNotifications(type: NotificationType?): Flow<String> {
        return bleDataSource.observeNotifications(type)
    }

    private suspend fun sendCustomCommand(command: String): AppResult<Unit> {
        val cmd = AmuletCommand.Custom(command)
        return bleDataSource.sendCommand(cmd)
    }

    private fun assemblePayload(segments: List<DeviceTimelineSegment>): ByteArray {
        val byteArrays = segments.map { segmentToByteArray(it) }
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

    /**
     * Сериализация сегмента в байты (SegmentLinearRgbV2, little-endian).
     * Перенесено из Domain слоя в Data слой.
     */
    private fun segmentToByteArray(segment: DeviceTimelineSegment): ByteArray {
        val result = ByteArray(21)
        var i = 0

        // opcode = 0x01 (LINEAR_RGB)
        result[i++] = 0x01

        result[i++] = (segment.targetMask and 0xFF).toByte()
        result[i++] = (segment.priority and 0xFF).toByte()
        result[i++] = mixModeToByte(segment.mixMode)

        writeUInt32LE(segment.startMs, result, i)
        i += 4
        writeUInt32LE(segment.durationMs, result, i)
        i += 4
        writeUInt16LE(segment.fadeInMs, result, i)
        i += 2
        writeUInt16LE(segment.fadeOutMs, result, i)
        i += 2

        result[i++] = easingToByte(segment.easingIn)
        result[i++] = easingToByte(segment.easingOut)

        result[i++] = (segment.color.red and 0xFF).toByte()
        result[i++] = (segment.color.green and 0xFF).toByte()
        result[i] = (segment.color.blue and 0xFF).toByte()

        return result
    }

    private fun mixModeToByte(mode: com.example.amulet.shared.domain.patterns.model.MixMode): Byte = when (mode) {
        com.example.amulet.shared.domain.patterns.model.MixMode.OVERRIDE -> 0
        com.example.amulet.shared.domain.patterns.model.MixMode.ADDITIVE -> 1
    }.toByte()

    private fun easingToByte(easing: com.example.amulet.shared.domain.patterns.model.Easing): Byte = when (easing) {
        com.example.amulet.shared.domain.patterns.model.Easing.LINEAR -> 0
    }.toByte()

    private fun writeUInt32LE(value: Long, target: ByteArray, offset: Int) {
        val v = value.coerceIn(0L, 0xFFFF_FFFFL)
        target[offset] = (v and 0xFF).toByte()
        target[offset + 1] = ((v ushr 8) and 0xFF).toByte()
        target[offset + 2] = ((v ushr 16) and 0xFF).toByte()
        target[offset + 3] = ((v ushr 24) and 0xFF).toByte()
    }

    private fun writeUInt16LE(value: Int, target: ByteArray, offset: Int) {
        val v = value.coerceIn(0, 0xFFFF)
        target[offset] = (v and 0xFF).toByte()
        target[offset + 1] = ((v ushr 8) and 0xFF).toByte()
    }
}
