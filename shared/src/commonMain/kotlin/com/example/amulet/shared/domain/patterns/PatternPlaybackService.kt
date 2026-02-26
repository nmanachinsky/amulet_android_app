package com.example.amulet.shared.domain.patterns

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.devices.model.DeviceAnimationPlan
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository
import com.example.amulet.shared.domain.devices.usecase.ObserveConnectedDeviceStatusUseCase
import com.example.amulet.shared.domain.patterns.compiler.DeviceTimelineCompiler
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PatternSpec
import com.example.amulet.shared.domain.patterns.usecase.GetPatternByIdUseCase
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay

/**
 * Сервис воспроизведения паттернов на устройстве.
 * Инкапсулирует компиляцию PatternSpec.timeline в таймлайновый план (DeviceAnimationPlan) и загрузку на амулет.
 */
class PatternPlaybackService(
    private val deviceTimelineCompiler: DeviceTimelineCompiler,
    private val deviceControlRepository: DeviceControlRepository,
    private val deviceRegistryRepository: DeviceRegistryRepository,
    private val observeConnectedDeviceStatus: ObserveConnectedDeviceStatusUseCase,
    private val getPatternById: GetPatternByIdUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    /**
     * Воспроизвести паттерн на конкретном устройстве.
     */
    suspend fun playOnDevice(
        spec: PatternSpec,
        deviceId: DeviceId,
        intensity: Double = 1.0,
        isPreview: Boolean = false,
    ): AppResult<Unit> = withContext(dispatcher) {
        try {
            Logger.d("playOnDevice: start deviceId=$deviceId specType=${spec.type} intensity=$intensity", tag = TAG)
            val deviceResult = deviceRegistryRepository.getDevice(deviceId)
            deviceResult.fold(
                success = { device ->
                    Logger.d("playOnDevice: device loaded deviceId=${device.id.value} hw=${device.hardwareVersion} fw=${device.firmwareVersion}", tag = TAG)

                    val timeline = spec.timeline

                    val segments = deviceTimelineCompiler.compile(
                        timeline = timeline,
                        hardwareVersion = device.hardwareVersion,
                        firmwareVersion = device.firmwareVersion,
                        intensity = intensity
                    )

                    val planId = if (isPreview) {
                        "preview-${System.currentTimeMillis()}"
                    } else {
                        "pattern-${System.currentTimeMillis()}"
                    }
                    val devicePlan = DeviceAnimationPlan(
                        id = planId,
                        totalDurationMs = timeline.durationMs.toLong(),
                        segments = segments,
                        isPreview = isPreview
                    )

                    Logger.d(
                        "playOnDevice: devicePlan segments=${segments.size} duration=${devicePlan.totalDurationMs}",
                        tag = TAG
                    )

                    var lastProgress: Int? = null
                    deviceControlRepository.uploadTimelinePlan(devicePlan, device.hardwareVersion)
                        .collect { percent ->
                            lastProgress = percent
                        }
                    Logger.d("playOnDevice: uploadTimelinePlan finished, lastProgress=$lastProgress", tag = TAG)
                    Ok(Unit)
                },
                failure = { error ->
                    Logger.d("playOnDevice: getDevice failed error=$error", tag = TAG)
                    Err(error)
                }
            )
        } catch (e: Exception) {
            Logger.d("playOnDevice: exception $e", tag = TAG)
            Err(AppError.Unknown)
        }
    }

    /**
     * Воспроизвести паттерн на текущем подключенном устройстве.
     */
    suspend fun playOnConnectedDevice(
        spec: PatternSpec,
        intensity: Double = 1.0,
        isPreview: Boolean = false,
    ): AppResult<Unit> = withContext(dispatcher) {
        try {
            Logger.d("playOnConnectedDevice: start specType=${spec.type} intensity=$intensity", tag = TAG)
            val status = awaitConnectedDeviceStatus()
                ?: return@withContext Err(AppError.BleError.DeviceDisconnected)

            val timeline = spec.timeline

            val segments = deviceTimelineCompiler.compile(
                timeline = timeline,
                hardwareVersion = status.hardwareVersion,
                firmwareVersion = status.firmwareVersion,
                intensity = intensity
            )

            val planId = if (isPreview) {
                "preview-${System.currentTimeMillis()}"
            } else {
                "pattern-${System.currentTimeMillis()}"
            }
            val devicePlan = DeviceAnimationPlan(
                id = planId,
                totalDurationMs = timeline.durationMs.toLong(),
                segments = segments,
                isPreview = isPreview
            )

            uploadPlanWithRetry(devicePlan, status.hardwareVersion)
        } catch (e: Exception) {
            Logger.d("playOnConnectedDevice: exception $e", tag = TAG)
            Err(AppError.Unknown)
        }
    }

    /**
     * Предзагрузить набор паттернов на текущее устройство (BEGIN_PLAN/ADD_SEGMENTS/COMMIT_PLAN).
     */
    suspend fun preloadPatterns(
        patternIds: List<PatternId>,
        intensity: Double = 1.0,
    ): AppResult<Unit> = withContext(dispatcher) {
        val status = awaitConnectedDeviceStatus()
            ?: return@withContext Err(AppError.BleError.DeviceDisconnected)

        return@withContext try {
            val uniqueIds = patternIds.distinct()
            for (patternId in uniqueIds) {
                val pattern = getPatternById(patternId).firstOrNull() ?: continue
                val timeline = pattern.spec.timeline
                val segments = deviceTimelineCompiler.compile(
                    timeline = timeline,
                    hardwareVersion = status.hardwareVersion,
                    firmwareVersion = status.firmwareVersion,
                    intensity = intensity
                )
                val plan = DeviceAnimationPlan(
                    id = pattern.id.value,
                    totalDurationMs = timeline.durationMs.toLong(),
                    segments = segments,
                )
                Logger.d(
                    "preloadPatterns: uploading plan id=${plan.id} segments=${plan.segments.size} duration=${plan.totalDurationMs}",
                    tag = TAG
                )
                val uploadResult = uploadPlanWithRetry(plan, status.hardwareVersion)
                val error = uploadResult.component2()
                if (error != null) {
                    return@withContext Err(error)
                }
            }
            Ok(Unit)
        } catch (e: Exception) {
            Logger.d("preloadPatterns: exception $e", tag = TAG)
            Err(AppError.Unknown)
        }
    }

    /**
     * Предзагрузить произвольный таймлайновый план на текущее устройство с заданным идентификатором.
     * Используется, например, для агрегированных паттернов практик (id = practiceId).
     */
    suspend fun preloadTimelinePlan(
        planId: String,
        spec: PatternSpec,
        intensity: Double = 1.0,
    ): AppResult<Unit> = withContext(dispatcher) {
        val status = awaitConnectedDeviceStatus()
            ?: return@withContext Err(AppError.BleError.DeviceDisconnected)

        return@withContext try {
            val hasPlanOnDevice = try {
                val result = deviceControlRepository.hasPattern(planId)
                result.isOk
            } catch (e: Exception) {
                Logger.d("preloadTimelinePlan: HAS_PLAN command failed, fallback to upload error=$e", tag = TAG)
                false
            }

            if (hasPlanOnDevice) {
                Logger.d(
                    "preloadTimelinePlan: plan already exists on device, skipping upload id=$planId",
                    tag = TAG
                )
                return@withContext Ok(Unit)
            }

            val timeline = spec.timeline
            val segments = deviceTimelineCompiler.compile(
                timeline = timeline,
                hardwareVersion = status.hardwareVersion,
                firmwareVersion = status.firmwareVersion,
                intensity = intensity
            )
            val totalDurationMs = (spec.durationMs ?: timeline.durationMs).toLong()
            val plan = DeviceAnimationPlan(
                id = planId,
                totalDurationMs = totalDurationMs,
                segments = segments,
            )
            Logger.d(
                "preloadTimelinePlan: uploading plan id=$planId segments=${plan.segments.size} duration=${plan.totalDurationMs}",
                tag = TAG
            )
            uploadPlanWithRetry(plan, status.hardwareVersion)
        } catch (e: Exception) {
            Logger.d("preloadTimelinePlan: exception $e", tag = TAG)
            Err(AppError.Unknown)
        }
    }

    /**
     * Отправить PLAY и дождаться NOTIFY:PATTERN:STARTED.
     */
    suspend fun playAndAwaitStart(
        patternId: PatternId,
        timeoutMs: Long = DEFAULT_PLAY_TIMEOUT_MS,
    ): AppResult<Unit> = withContext(dispatcher) {
        val commandResult = deviceControlRepository.playPattern(patternId.value)
        commandResult.fold(
            success = {
                try {
                    withTimeout(timeoutMs) {
                        deviceControlRepository.observeNotifications(NotificationType.PATTERN)
                            .first { it.startsWith("NOTIFY:PATTERN:STARTED:${patternId.value}") }
                    }
                    Ok(Unit)
                } catch (e: Exception) {
                    Logger.w(
                        "playAndAwaitStart: timeout waiting NOTIFY:PATTERN:STARTED for ${patternId.value}",
                        e,
                        tag = TAG
                    )
                    Err(AppError.Timeout)
                }
            },
            failure = { Err(it) }
        )
    }

    /**
     * Наблюдать завершение анимации (NOTIFY:ANIMATION:COMPLETE).
     */
    fun observeAnimationComplete(patternId: PatternId? = null): Flow<String> {
        val baseFlow = deviceControlRepository.observeNotifications(NotificationType.ANIMATION)
            .mapNotNull { message ->
                if (message.startsWith("NOTIFY:ANIMATION:COMPLETE:")) {
                    message.substringAfterLast(":")
                } else null
            }
        return if (patternId == null) baseFlow else baseFlow.filter { it == patternId.value }
    }

    /**
     * Принудительно очистить текущий подключенный амулет (отправить ClearAll напрямую).
     */
    suspend fun clearCurrentDevice(): AppResult<Unit> = withContext(dispatcher) {
        try {
            Logger.d("clearCurrentDevice: start", tag = TAG)
            val status = awaitConnectedDeviceStatus()
                ?: return@withContext Err(AppError.BleError.DeviceDisconnected)

            deviceControlRepository.clearDevice()
        } catch (e: Exception) {
            Logger.d("clearCurrentDevice: exception $e", tag = TAG)
            Err(AppError.Unknown)
        }
    }

    private suspend fun awaitConnectedDeviceStatus(): com.example.amulet.shared.domain.devices.model.DeviceLiveStatus? {
        return try {
            withTimeout(DEFAULT_DEVICE_STATUS_TIMEOUT_MS) {
                observeConnectedDeviceStatus().first { it != null }
            }
        } catch (e: Exception) {
            Logger.d("awaitConnectedDeviceStatus: timeout or error while waiting for device status: $e", tag = TAG)
            null
        }
    }

    private suspend fun uploadPlanWithRetry(
        plan: DeviceAnimationPlan,
        hardwareVersion: Int,
        maxAttempts: Int = DEFAULT_UPLOAD_RETRY_ATTEMPTS,
        retryDelayMs: Long = DEFAULT_UPLOAD_RETRY_DELAY_MS,
    ): AppResult<Unit> {
        var attempt = 0
        var lastError: AppError? = null

        while (attempt < maxAttempts) {
            try {
                var lastProgress: Int? = null
                deviceControlRepository.uploadTimelinePlan(plan, hardwareVersion)
                    .collect { percent -> lastProgress = percent }
                Logger.d(
                    "uploadPlanWithRetry: success on attempt=${attempt + 1}, lastProgress=$lastProgress",
                    tag = TAG
                )
                return Ok(Unit)
            } catch (e: Exception) {
                lastError = AppError.Unknown
                Logger.w(
                    "uploadPlanWithRetry: attempt=${attempt + 1} failed with exception=$e",
                    e,
                    tag = TAG
                )
            }

            attempt++
            if (attempt < maxAttempts) {
                delay(retryDelayMs * attempt)
            }
        }

        return Err(lastError ?: AppError.Unknown)
    }
}

private const val TAG = "PatternPlaybackService"
private const val DEFAULT_PLAY_TIMEOUT_MS = 5_000L
private const val DEFAULT_DEVICE_STATUS_TIMEOUT_MS = 5_000L
private const val DEFAULT_UPLOAD_RETRY_ATTEMPTS = 3
private const val DEFAULT_UPLOAD_RETRY_DELAY_MS = 500L
