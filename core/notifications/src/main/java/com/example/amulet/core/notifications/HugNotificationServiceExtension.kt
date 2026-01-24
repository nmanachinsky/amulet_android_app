package com.example.amulet.core.notifications

import android.graphics.Color
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension
import androidx.core.graphics.toColorInt

@Keep
class HugNotificationServiceExtension : INotificationServiceExtension {

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        val additionalData = event.notification.additionalData
        val type = additionalData?.optString(KEY_TYPE)?.takeIf { it.isNotBlank() }
        if (type != TYPE_HUG) return

        val fromUserName = additionalData.optString(KEY_FROM_USER_NAME).takeIf { it.isNotBlank() }
        val emotionName = additionalData.optString(KEY_EMOTION_NAME).takeIf { it.isNotBlank() }
        val emotionColorHex = additionalData.optString(KEY_EMOTION_COLOR).takeIf { it.isNotBlank() }

        val content = HugNotificationContentProvider.incomingHug(
            fromUserName = fromUserName,
            emotionName = emotionName,
            emotionColorHex = emotionColorHex,
        )

        val parsedColor = emotionColorHex?.let { hex -> runCatching { hex.toColorInt() }.getOrNull() }

        event.notification.setExtender(
            NotificationCompat.Extender { builder ->
                builder
                    .setContentTitle(content.title)
                    .setContentText(content.message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content.message))
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setGroup(GROUP_HUGS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .also { b ->
                        parsedColor?.let { b.setColor(it) }
                    }
            }
        )
    }

    private companion object {
        private const val KEY_TYPE = "type"
        private const val TYPE_HUG = "hug"

        private const val KEY_FROM_USER_NAME = "fromUserName"
        private const val KEY_EMOTION_NAME = "emotionName"
        private const val KEY_EMOTION_COLOR = "emotionColorHex"

        private const val GROUP_HUGS = "hugs"
    }
}
