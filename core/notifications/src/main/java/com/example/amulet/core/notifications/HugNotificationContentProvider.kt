package com.example.amulet.core.notifications

data class HugNotificationContent(
    val title: String,
    val message: String,
)

object HugNotificationContentProvider {

    fun incomingHug(
        fromUserName: String?,
        emotionName: String?,
        emotionColorHex: String?,
    ): HugNotificationContent {
        val safeName = fromUserName?.trim()?.takeIf { it.isNotBlank() }
        val title = if (safeName != null) {
            "$safeName прислал(а) объятие"
        } else {
            "Тебе пришло объятие"
        }

        val name = emotionName?.lowercase()?.trim()?.takeIf { it.isNotBlank() }
        val color = emotionColorHex?.lowercase()?.trim()?.takeIf { it.isNotBlank() }

        val message = when {
            name != null && ("люб" in name || "love" in name || "heart" in name) ->
                "Тёплое объятие уже ждёт тебя. Открой приложение, чтобы посмотреть." 

            name != null && ("поддерж" in name || "support" in name) ->
                "Объятие поддержки уже ждёт тебя. Открой приложение, чтобы посмотреть."

            name != null && ("тих" in name || "calm" in name || "спок" in name) ->
                "Тихое объятие уже ждёт тебя. Открой приложение, чтобы посмотреть."

            color != null && (color.startsWith("#ff") || color.startsWith("#e")) ->
                "Яркое объятие уже ждёт тебя. Открой приложение, чтобы посмотреть."

            else ->
                "Новое объятие уже ждёт тебя. Открой приложение, чтобы посмотреть."
        }

        return HugNotificationContent(
            title = title,
            message = message,
        )
    }

    fun queuedHug(emotionName: String?, emotionColorHex: String?): HugNotificationContent {
        val title = "Объятие в очереди"

        val name = emotionName?.lowercase()?.trim()
        val color = emotionColorHex?.lowercase()?.trim()

        val message = when {
            name != null && ("люб" in name || "love" in name || "heart" in name) ->
                "Тёплое объятие в очереди, дойдёт при подключении амулета."

            name != null && ("поддерж" in name || "support" in name) ->
                "Объятие поддержки в очереди, дойдёт при подключении амулета."

            name != null && ("тих" in name || "calm" in name || "спок" in name) ->
                "Тихое объятие в очереди, дойдёт при подключении амулета."

            color != null && (color.startsWith("#ff") || color.startsWith("#e")) ->
                "Яркое объятие в очереди, дойдёт при подключении амулета."

            else ->
                "Объятие будет воспроизведено, когда амулет подключится."
        }

        return HugNotificationContent(
            title = title,
            message = message,
        )
    }
}
