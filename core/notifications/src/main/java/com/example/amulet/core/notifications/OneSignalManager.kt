package com.example.amulet.core.notifications

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.example.amulet.shared.core.logging.Logger
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@Singleton
class OneSignalManager @Inject constructor(
    private val application: Application,
    private val pushNotificationRouter: PushNotificationRouter,
) {

    companion object {
        private const val TAG = "OneSignalManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playerIdFlow = MutableStateFlow<String?>(null)
    private var isInitialized: Boolean = false
    private var pendingLoginUserId: String? = null
    private val pushSubscriptionObserver = object : IPushSubscriptionObserver {
        override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
            playerIdFlow.value = state.current.id
        }
    }

    fun initialize(appId: String, isDebug: Boolean) {
        Logger.d("OneSignal.initialize called (isDebug=$isDebug, appIdBlank=${appId.isBlank()})", tag = TAG)
        if (appId.isBlank()) {
            Logger.w("OneSignal.initialize skipped: blank appId", tag = TAG)
            return
        }
        OneSignal.Debug.logLevel = if (isDebug) LogLevel.VERBOSE else LogLevel.NONE
        OneSignal.initWithContext(application, appId)
        isInitialized = true
        Logger.d("OneSignal.initialize done", tag = TAG)

        pendingLoginUserId?.let { userId ->
            Logger.d("OneSignal.initialize: applying pending login userId=$userId", tag = TAG)
            OneSignal.login(userId)
        }
        pendingLoginUserId = null

        updatePlayerId()

        OneSignal.User.pushSubscription.addObserver(pushSubscriptionObserver)

        // Обработка пуша, когда приложение на переднем плане.
        val foregroundListener = object : INotificationLifecycleListener {
            override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                val data = additionalDataToMap(event.notification.additionalData)

                pushNotificationRouter.handle(data)
                // Ничего не блокируем: уведомление покажется как обычно.
            }
        }
        OneSignal.Notifications.addForegroundLifecycleListener(foregroundListener)

        // Обработка клика по уведомлению (deeplink в экран объятий / конкретное объятие).
        val clickListener = object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val data = additionalDataToMap(event.notification.additionalData)

                val type = data["type"]
                val hugId = data["hugId"]

                val uri = if (type == "hug" && !hugId.isNullOrBlank()) {
                    "amulet://hugs/$hugId".toUri()
                } else {
                    "amulet://hugs".toUri()
                }

                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                application.startActivity(intent)
            }
        }
        OneSignal.Notifications.addClickListener(clickListener)
    }

    private fun additionalDataToMap(additionalData: Any?): Map<String, String> {
        val json = additionalData as? JSONObject ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = json.opt(key)?.toString().orEmpty()
        }
        return result
    }

    fun playerId(): StateFlow<String?> = playerIdFlow.asStateFlow()

    /**
     * Привязать текущего авторизованного пользователя к профилю OneSignal.
     * userId — доменный/бэкенд-идентификатор пользователя.
     */
    fun login(userId: String) {
        if (!isInitialized) {
            if (userId.isBlank()) {
                Logger.w("OneSignal.login skipped: not initialized + blank userId", tag = TAG)
                return
            }
            pendingLoginUserId = userId
            Logger.w("OneSignal.login delayed (not initialized) userId=$userId", tag = TAG)
            return
        }
        if (userId.isBlank()) {
            Logger.w("OneSignal.login skipped: blank userId", tag = TAG)
            return
        }
        Logger.d("OneSignal.login userId=$userId", tag = TAG)
        OneSignal.login(userId)
    }

    /**
     * Отвязать пользователя от OneSignal (например, при выходе из аккаунта).
     */
    fun logout() {
        if (!isInitialized) {
            Logger.w("OneSignal.logout skipped: not initialized", tag = TAG)
            pendingLoginUserId = null
            return
        }
        Logger.d("OneSignal.logout", tag = TAG)
        OneSignal.logout()
    }

    private fun updatePlayerId() {
        if (!isInitialized) return
        playerIdFlow.value = OneSignal.User.pushSubscription.id
    }
}
