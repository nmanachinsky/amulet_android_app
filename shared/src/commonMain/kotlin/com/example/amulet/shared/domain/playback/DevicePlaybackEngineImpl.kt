package com.example.amulet.shared.domain.playback

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.devices.model.DeviceAnimationPlan
import com.example.amulet.shared.domain.devices.model.DeviceLiveStatus
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.devices.usecase.ObserveConnectedDeviceStatusUseCase
import com.example.amulet.shared.domain.patterns.compiler.DeviceTimelineCompiler
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.model.PatternSpec
import com.example.amulet.shared.domain.patterns.usecase.GetPatternByIdUseCase
import com.example.amulet.shared.domain.practices.model.Practice
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeStep
import com.example.amulet.shared.domain.practices.usecase.HasPracticeScriptOnDeviceUseCase
import com.example.amulet.shared.domain.practices.usecase.UploadPracticeScriptToDeviceUseCase
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class DevicePlaybackEngineImpl(
    private val deviceTimelineCompiler: DeviceTimelineCompiler,
    private val deviceControlRepository: DeviceControlRepository,
    private val observeConnectedDeviceStatus: ObserveConnectedDeviceStatusUseCase,
    private val getPatternById: GetPatternByIdUseCase,
    private val uploadPracticeScriptToDevice: UploadPracticeScriptToDeviceUseCase,
    private val hasPracticeScriptOnDevice: HasPracticeScriptOnDeviceUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : DevicePlaybackEngine {

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()
    private val mutex = Mutex()

    override suspend fun play(media: PlayableMedia): AppResult<Unit> = mutex.withLock {
        withContext(dispatcher) {
        val status = awaitConnectedDeviceStatus()
            ?: return@withContext Err(AppError.BleError.DeviceDisconnected)

        when (media) {
            is PlayableMedia.Preview -> playPreview(media, status)
            is PlayableMedia.SinglePattern -> playSinglePattern(media, status)
            is PlayableMedia.PracticeScript -> playPracticeScript(media, status)
        }
        }
    }

    override suspend fun stop(): AppResult<Unit> = mutex.withLock {
        withContext(dispatcher) {
            try {
                _playbackState.value = PlaybackState.IDLE
                deviceControlRepository.clearDevice()
            } catch (e: Exception) {
                Logger.d("stop: exception $e", tag = TAG)
                Err(AppError.Unknown)
            }
        }
    }

    private suspend fun playPreview(
        media: PlayableMedia.Preview,
        status: DeviceLiveStatus
    ): AppResult<Unit> {
        _playbackState.value = PlaybackState.COMPILING
        Logger.d("playPreview: start specType=${media.spec.type} intensity=${media.intensity}", tag = TAG)

        val timeline = media.spec.timeline
        val segments = deviceTimelineCompiler.compile(
            timeline = timeline,
            hardwareVersion = status.hardwareVersion,
            firmwareVersion = status.firmwareVersion,
            intensity = media.intensity
        )

        val planId = "preview-${System.currentTimeMillis()}"
        val devicePlan = DeviceAnimationPlan(
            id = planId,
            totalDurationMs = timeline.durationMs.toLong(),
            segments = segments,
            isPreview = true
        )

        _playbackState.value = PlaybackState.UPLOADING
        val uploadResult = uploadPlanWithRetry(devicePlan, status.hardwareVersion)
        if (uploadResult.isErr) {
            _playbackState.value = PlaybackState.ERROR
            return uploadResult
        }

        _playbackState.value = PlaybackState.PLAYING
        val playResult = deviceControlRepository.playPattern(planId)
        if (playResult.isErr) {
            _playbackState.value = PlaybackState.ERROR
        }
        return playResult
    }

    private suspend fun playSinglePattern(
        media: PlayableMedia.SinglePattern,
        status: DeviceLiveStatus
    ): AppResult<Unit> {
        Logger.d("playSinglePattern: patternId=${media.patternId} intensity=${media.intensity}", tag = TAG)

        val pattern = getPatternById(media.patternId).firstOrNull()
            ?: return Err(AppError.NotFound)

        _playbackState.value = PlaybackState.COMPILING
        val timeline = pattern.spec.timeline
        val segments = deviceTimelineCompiler.compile(
            timeline = timeline,
            hardwareVersion = status.hardwareVersion,
            firmwareVersion = status.firmwareVersion,
            intensity = media.intensity
        )

        val planId = media.patternId.value
        val devicePlan = DeviceAnimationPlan(
            id = planId,
            totalDurationMs = timeline.durationMs.toLong(),
            segments = segments,
            isPreview = false
        )

        _playbackState.value = PlaybackState.UPLOADING
        val uploadResult = uploadPlanWithRetry(devicePlan, status.hardwareVersion)
        if (uploadResult.isErr) {
            _playbackState.value = PlaybackState.ERROR
            return uploadResult
        }

        _playbackState.value = PlaybackState.PLAYING
        val playResult = deviceControlRepository.playPattern(planId)
        if (playResult.isErr) {
            _playbackState.value = PlaybackState.ERROR
        }
        return playResult
    }

    private suspend fun playPracticeScript(
        media: PlayableMedia.PracticeScript,
        status: DeviceLiveStatus
    ): AppResult<Unit> {
        Logger.d("playPracticeScript: practiceId=${media.practiceId} steps=${media.steps.size}", tag = TAG)

        val patternIdsToPreload = media.steps.mapNotNull { it.patternId?.let(::PatternId) }.distinct()
        if (patternIdsToPreload.isNotEmpty()) {
            _playbackState.value = PlaybackState.COMPILING
            for (patternId in patternIdsToPreload) {
                val pattern = getPatternById(patternId).firstOrNull() ?: continue
                val timeline = pattern.spec.timeline
                val segments = deviceTimelineCompiler.compile(
                    timeline = timeline,
                    hardwareVersion = status.hardwareVersion,
                    firmwareVersion = status.firmwareVersion,
                    intensity = media.intensity
                )
                val plan = DeviceAnimationPlan(
                    id = pattern.id.value,
                    totalDurationMs = timeline.durationMs.toLong(),
                    segments = segments,
                )
                Logger.d("playPracticeScript: preloading pattern=${pattern.id.value}", tag = TAG)
                _playbackState.value = PlaybackState.UPLOADING
                val uploadResult = uploadPlanWithRetry(plan, status.hardwareVersion)
                if (uploadResult.isErr) {
                    _playbackState.value = PlaybackState.ERROR
                    return uploadResult
                }
            }
        }

        val hasScriptOnDevice = hasPracticeScriptOnDevice(media.practiceId)
        if (!hasScriptOnDevice) {
            _playbackState.value = PlaybackState.UPLOADING
            val practice = createPracticeFromScript(media.practiceId, media.steps)
            val uploadResult = uploadPracticeScriptToDevice(practice)
            if (uploadResult.isErr) {
                _playbackState.value = PlaybackState.ERROR
                return uploadResult
            }
        }

        _playbackState.value = PlaybackState.PLAYING
        val playResult = deviceControlRepository.playPracticeScript(media.practiceId)
        if (playResult.isErr) {
            _playbackState.value = PlaybackState.ERROR
        }
        return playResult
    }

    private fun createPracticeFromScript(
        practiceId: PracticeId,
        steps: List<PracticeStep>
    ): Practice {
        return Practice(
            id = practiceId,
            type = com.example.amulet.shared.domain.practices.model.PracticeType.MEDITATION,
            title = "",
            description = null,
            durationSec = null,
            level = null,
            goal = null,
            patternId = null,
            audioUrl = null,
            script = com.example.amulet.shared.domain.practices.model.PracticeScript(steps),
            hasDeviceScript = true,
            createdAt = null,
            updatedAt = null
        )
    }

    private suspend fun awaitConnectedDeviceStatus(): DeviceLiveStatus? {
        return try {
            withTimeout(DEFAULT_DEVICE_STATUS_TIMEOUT_MS) {
                observeConnectedDeviceStatus().first { it != null }
            }
        } catch (e: Exception) {
            Logger.d("awaitConnectedDeviceStatus: timeout or error: $e", tag = TAG)
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
                deviceControlRepository.uploadTimelinePlan(plan, hardwareVersion)
                    .collect { }
                Logger.d("uploadPlanWithRetry: success on attempt=${attempt + 1}", tag = TAG)
                return Ok(Unit)
            } catch (e: Exception) {
                lastError = AppError.Unknown
                Logger.w("uploadPlanWithRetry: attempt=${attempt + 1} failed: $e", tag = TAG)
            }
            attempt++
            if (attempt < maxAttempts) {
                kotlinx.coroutines.delay(retryDelayMs * attempt)
            }
        }
        return Err(lastError ?: AppError.Unknown)
    }

    companion object {
        private const val TAG = "DevicePlaybackEngine"
        private const val DEFAULT_DEVICE_STATUS_TIMEOUT_MS = 10_000L
        private const val DEFAULT_UPLOAD_RETRY_ATTEMPTS = 3
        private const val DEFAULT_UPLOAD_RETRY_DELAY_MS = 500L
    }
}
