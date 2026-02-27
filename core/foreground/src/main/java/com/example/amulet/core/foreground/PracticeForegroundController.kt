package com.example.amulet.core.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.amulet.core.foreground.AmuletForegroundOrchestrator
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.practices.PracticeProgress
import com.example.amulet.shared.domain.practices.PracticeSessionManager
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionId
import com.example.amulet.shared.domain.practices.model.PracticeSessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * Контроллер foreground-логики для практик.
 * Инкапсулирует уведомление, обработку action-интентов и реакцию на activeSession.
 */
class PracticeForegroundController @Inject constructor(
    private val practiceSessionManager: PracticeSessionManager,
    private val orchestrator: AmuletForegroundOrchestrator,
) {

    private lateinit var service: Service

    private var job = SupervisorJob()
    private var scope = CoroutineScope(job + Dispatchers.Main.immediate)

    fun onCreate(service: Service) {
        this.service = service
        Logger.d("PracticeForegroundController.onCreate", tag = TAG)

        // Пересоздаём scope, если предыдущий был отменён
        if (job.isCancelled) {
            job = SupervisorJob()
            scope = CoroutineScope(job + Dispatchers.Main.immediate)
            Logger.d("PracticeForegroundController: recreated scope after cancellation", tag = TAG)
        }

        if (!hasRequiredPermissionsForConnectedDeviceFgs()) {
            // Нет необходимых runtime‑разрешений – не стартуем FGS, чтобы не уронить приложение.
            Logger.w("Required FGS/Bluetooth permissions are missing, skipping startForeground", tag = TAG)
            return
        }
        Logger.d("Permissions OK, creating notification channel and starting foreground", tag = TAG)
        createNotificationChannel()
        val initial = buildPracticeNotification(session = null, progress = null)
        service.startForeground(NOTIFICATION_ID, initial)
        observeActiveSession()
    }

    fun onDestroy() {
        Logger.d("PracticeForegroundController.onDestroy", tag = TAG)
        scope.cancel()
    }

    fun handleIntent(action: String?) {
        Logger.d("handleIntent(action=$action)", tag = TAG)
        when (action) {
            ACTION_PRACTICE_STOP -> {
                scope.launch {
                    val session = practiceSessionManager.activeSession.firstOrNull()
                    val sessionId = session?.id ?: return@launch
                    Logger.d("ACTION_PRACTICE_STOP for sessionId=$sessionId", tag = TAG)
                    practiceSessionManager.stopSession(completed = false)
                    // дальнейшая остановка сервиса произойдёт через observeActiveSession
                }
            }
            ACTION_PRACTICE_OPEN -> {
                scope.launch {
                    val session = practiceSessionManager.activeSession.firstOrNull()
                    val practiceId = session?.practiceId
                    Logger.d("ACTION_PRACTICE_OPEN practiceId=$practiceId", tag = TAG)

                    val intent = if (practiceId != null) {
                        val uri = "amulet://practices/session/$practiceId".toUri()
                        Intent(Intent.ACTION_VIEW, uri)
                    } else {
                        service.packageManager.getLaunchIntentForPackage(service.packageName)
                    }?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }

                    if (intent != null) {
                        service.startActivity(intent)
                    }
                }
            }
        }
    }

    suspend fun startPractice(practiceId: PracticeId) {
        Logger.d("startPractice(practiceId=$practiceId)", tag = TAG)
        practiceSessionManager.startSession(practiceId)
    }

    suspend fun stopPractice(sessionId: PracticeSessionId, completed: Boolean) {
        val session = practiceSessionManager.activeSession.firstOrNull()
        if (session?.id != sessionId) {
            Logger.w("stopPractice called with mismatched sessionId=$sessionId, active=${session?.id}", tag = TAG)
            return
        }
        Logger.d("stopPractice(sessionId=$sessionId, completed=$completed)", tag = TAG)
        practiceSessionManager.stopSession(completed)
    }

    private fun observeActiveSession() {
        scope.launch {
            practiceSessionManager.activeSession
                .combine(practiceSessionManager.progress) { session, progress ->
                    session to progress
                }
                .collect { (session, progress) ->
                    if (session == null) {
                        Logger.d("activeSession is null -> setPracticeActive(false)", tag = TAG)
                        // Даже если orchestrator по какой-то причине не остановит сервис,
                        // мы обязаны убрать foreground-уведомление, иначе оно зависнет навсегда.
                        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
                        val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.cancel(NOTIFICATION_ID)
                        orchestrator.setPracticeActive(false)
                    } else {
                        Logger.d(
                            "activeSession updated: id=${session.id}, status=${session.status}, progress=$progress",
                            tag = TAG,
                        )
                        orchestrator.setPracticeActive(true)
                        updateNotificationForSession(session, progress)
                    }
                }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Logger.d("createNotificationChannel()", tag = TAG)
            val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(PRACTICES_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    PRACTICES_CHANNEL_ID,
                    "Практики",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = "Foreground service for practice sessions"
                manager.createNotificationChannel(channel)
                Logger.d("Notification channel $PRACTICES_CHANNEL_ID created", tag = TAG)
            }
        }
    }

    private fun buildPracticeNotification(session: PracticeSession?, progress: PracticeProgress?): Notification {
        val title = "Практика"

        val contentText = if (session != null && progress != null && progress.sessionId == session.id) {
            val total = progress.totalSec
            if (total != null) {
                val remaining = (total - progress.elapsedSec).coerceAtLeast(0)
                val minutes = remaining / 60
                val seconds = remaining % 60
                val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                "Осталось $timeFormatted"
            } else {
                "Идёт практика"
            }
        } else {
            "Идёт практика"
        }

        Logger.d(
            "buildPracticeNotification(sessionId=${session?.id}, status=${session?.status}, progress=$progress, contentText=$contentText)",
            tag = TAG,
        )

        val stopIntent = PendingIntent.getService(
            service,
            2,
            Intent(service, AmuletForegroundService::class.java).setAction(ACTION_PRACTICE_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openActivityIntent = if (session?.practiceId != null) {
            val uri = Uri.parse("amulet://practices/session/${session.practiceId}")
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } else {
            service.packageManager.getLaunchIntentForPackage(service.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        val builder = NotificationCompat.Builder(service, PRACTICES_CHANNEL_ID)
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)

        if (openActivityIntent != null) {
            val openPendingIntent = PendingIntent.getActivity(
                service,
                3,
                openActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.setContentIntent(openPendingIntent)
            builder.addAction(0, "Открыть", openPendingIntent)
        }

        builder.addAction(0, "Завершить", stopIntent)

        return builder.build()
    }

    private fun updateNotificationForSession(session: PracticeSession, progress: PracticeProgress?) {
        Logger.d(
            "updateNotificationForSession(sessionId=${session.id}, status=${session.status}, progress=$progress)",
            tag = TAG,
        )
        val notification = buildPracticeNotification(session, progress)
        service.startForeground(NOTIFICATION_ID, notification)
    }

    private fun hasRequiredPermissionsForConnectedDeviceFgs(): Boolean {
        if (Build.VERSION.SDK_INT < 34) {
            Logger.d("SDK < 34, hasRequiredPermissionsForConnectedDeviceFgs=true", tag = TAG)
            return true
        }

        val hasFgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val transportPerms = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        )

        val hasAnyTransport = transportPerms.any { perm ->
            service.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        }

        val result = hasFgs && hasAnyTransport
        Logger.d(
            "hasRequiredPermissionsForConnectedDeviceFgs: hasFgs=$hasFgs, hasAnyTransport=$hasAnyTransport, result=$result",
            tag = TAG,
        )
        return result
    }

    companion object {
        private const val TAG = "PracticeForegroundController"
        const val PRACTICES_CHANNEL_ID = "amulet_practices_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PRACTICE_STOP = "com.example.amulet.action.PRACTICE_STOP"
        const val ACTION_PRACTICE_OPEN = "com.example.amulet.action.PRACTICE_OPEN"
    }
}
