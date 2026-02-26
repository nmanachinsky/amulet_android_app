package com.example.amulet.core.foreground

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeSessionId
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.patterns.usecase.GetPatternByIdUseCase
import com.example.amulet.shared.domain.patterns.usecase.PreviewPatternOnDeviceUseCase
import com.example.amulet.shared.domain.devices.model.DeviceId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Foreground-сервис для всех взаимодействий с амулетом (практики, объятия, предпросмотр паттернов и т.д.).
 * Держит BLE-подключение и гарантирует работу в фоне под системной защитой.
 */
@AndroidEntryPoint
class AmuletForegroundService : Service(), AmuletForegroundOrchestrator.Host {

    private val binder = AmuletControlBinder()

    // UseCase'ы из :shared, получаем через Hilt (под капотом — KoinBridgeModule)
    @Inject lateinit var getPatternByIdUseCase: GetPatternByIdUseCase
    @Inject lateinit var previewPatternOnDeviceUseCase: PreviewPatternOnDeviceUseCase

    @Inject lateinit var orchestrator: AmuletForegroundOrchestrator
    @Inject lateinit var practiceController: PracticeForegroundController

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Logger.d("AmuletForegroundService.onCreate", tag = TAG)
        orchestrator.attachHost(this)
        practiceController.onCreate(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d(
            "onStartCommand action=${intent?.action} flags=$flags startId=$startId",
            tag = TAG,
        )
        when (intent?.action) {
            PracticeForegroundController.ACTION_PRACTICE_STOP,
            PracticeForegroundController.ACTION_PRACTICE_OPEN -> {
                practiceController.handleIntent(intent.action)
            }
            else -> {
                practiceController.handleIntent(intent?.action)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("AmuletForegroundService.onDestroy", tag = TAG)
        practiceController.onDestroy()
    }

    override fun stopService() {
        Logger.d("stopService() requested, stopping foreground and self", tag = TAG)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    inner class AmuletControlBinder : Binder(), AmuletControl {
        override suspend fun startPracticeSession(practiceId: PracticeId) {
            Logger.d("startPracticeSession(practiceId=$practiceId)", tag = TAG)
            practiceController.startPractice(practiceId)
        }

        override suspend fun stopPracticeSession(sessionId: PracticeSessionId, completed: Boolean) {
            Logger.d(
                "stopPracticeSession(sessionId=$sessionId, completed=$completed)",
                tag = TAG,
            )
            practiceController.stopPractice(sessionId, completed)
        }

        override suspend fun previewPattern(patternId: PatternId, deviceId: DeviceId?) {
            Logger.d(
                "previewPattern(patternId=$patternId, deviceId=$deviceId)",
                tag = TAG,
            )
            val pattern = getPatternByIdUseCase(patternId).firstOrNull() ?: return
            previewPatternOnDeviceUseCase(pattern.spec).firstOrNull()
        }

        // Объятия пока не поддерживаются из foreground-сервиса.
    }

    companion object {
        private const val TAG = "AmuletForegroundService"
        private const val PRACTICES_CHANNEL_ID = "amulet_practices_channel"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_PRACTICE_PAUSE = "com.example.amulet.action.PRACTICE_PAUSE"
        private const val ACTION_PRACTICE_RESUME = "com.example.amulet.action.PRACTICE_RESUME"
        private const val ACTION_PRACTICE_STOP = "com.example.amulet.action.PRACTICE_STOP"
        private const val ACTION_PRACTICE_OPEN = "com.example.amulet.action.PRACTICE_OPEN"
    }
}

/**
 * Интерфейс управления foreground-сервисом амулета.
 * Может использоваться из UI/WorkManager через bound-соединение.
 */
interface AmuletControl {
    suspend fun startPracticeSession(practiceId: PracticeId)
    suspend fun stopPracticeSession(sessionId: PracticeSessionId, completed: Boolean)
    suspend fun previewPattern(patternId: PatternId, deviceId: DeviceId? = null)
}
