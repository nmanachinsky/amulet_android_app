package com.example.amulet.core.foreground

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.amulet.shared.core.logging.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground-сервис для всех взаимодействий с амулетом (практики, объятия, предпросмотр паттернов и т.д.).
 * Держит BLE-подключение и гарантирует работу в фоне под системной защитой.
 */
@AndroidEntryPoint
class AmuletForegroundService : Service(), AmuletForegroundOrchestrator.Host {

    @Inject lateinit var orchestrator: AmuletForegroundOrchestrator
    @Inject lateinit var practiceController: PracticeForegroundController

    override fun onBind(intent: Intent?): IBinder? = null

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
