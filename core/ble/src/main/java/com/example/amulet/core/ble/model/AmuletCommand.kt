package com.example.amulet.core.ble.model

import android.util.Base64

sealed interface AmuletCommand {
    
    data class SetRing(
        val colors: List<Rgb>
    ) : AmuletCommand
    
    data class SetLed(
        val index: Int,
        val color: Rgb
    ) : AmuletCommand
    
    data object ClearAll : AmuletCommand
    
    data class Delay(
        val durationMs: Int
    ) : AmuletCommand
    
    data class Play(
        val patternId: String
    ) : AmuletCommand
    
    data class HasPlan(
        val patternId: String
    ) : AmuletCommand
    
    data class BeginPracticeScript(
        val practiceId: String
    ) : AmuletCommand

    data class AddPracticeStep(
        val practiceId: String,
        val order: Int,
        val patternId: String,
    ) : AmuletCommand

    data class CommitPracticeScript(
        val practiceId: String
    ) : AmuletCommand

    data class HasPracticeScript(
        val practiceId: String
    ) : AmuletCommand

    data class PlayPracticeScript(
        val practiceId: String
    ) : AmuletCommand
    
    data class SetWifiCred(
        val ssidBase64: String,
        val passwordBase64: String
    ) : AmuletCommand
    
    data class WifiOtaStart(
        val url: String,
        val version: String,
        val checksum: String
    ) : AmuletCommand
    
    data object GetProtocolVersion : AmuletCommand
    
    data class Custom(
        val command: String,
        val parameters: List<String> = emptyList()
    ) : AmuletCommand
}

data class Rgb(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    init {
        require(red in 0..255) { "Red must be in 0..255" }
        require(green in 0..255) { "Green must be in 0..255" }
        require(blue in 0..255) { "Blue must be in 0..255" }
    }
    
    fun toHex(): String = "#%02X%02X%02X".format(red, green, blue)
    
    companion object {
        fun fromHex(hex: String): Rgb {
            val cleanHex = hex.removePrefix("#")
            require(cleanHex.length == 6) { "Hex color must be 6 characters" }
            
            return Rgb(
                red = cleanHex.substring(0, 2).toInt(16),
                green = cleanHex.substring(2, 4).toInt(16),
                blue = cleanHex.substring(4, 6).toInt(16)
            )
        }
    }
}

fun AmuletCommand.toCommandString(): String = when (this) {
    is AmuletCommand.SetRing -> {
        val colorsStr = colors.joinToString(",") { "${it.red},${it.green},${it.blue}" }
        "SET_RING:$colorsStr"
    }
    is AmuletCommand.SetLed -> "SET_LED:${index},${color.red},${color.green},${color.blue}"
    is AmuletCommand.ClearAll -> "CLEAR_ALL"
    is AmuletCommand.Delay -> "DELAY:$durationMs"
    is AmuletCommand.Play -> "PLAY:$patternId"
    is AmuletCommand.HasPlan -> "HAS_PLAN:$patternId"
    is AmuletCommand.BeginPracticeScript -> "BEGIN_PRACTICE_SCRIPT:$practiceId"
    is AmuletCommand.AddPracticeStep -> "ADD_PRACTICE_STEP:$practiceId,$order,$patternId"
    is AmuletCommand.CommitPracticeScript -> "COMMIT_PRACTICE_SCRIPT:$practiceId"
    is AmuletCommand.HasPracticeScript -> "HAS_PRACTICE_SCRIPT:$practiceId"
    is AmuletCommand.PlayPracticeScript -> "PLAY_PRACTICE_SCRIPT:$practiceId"
    is AmuletCommand.SetWifiCred -> "SET_WIFI_CRED:$ssidBase64,$passwordBase64"
    is AmuletCommand.WifiOtaStart -> "WIFI_OTA_START:$url,$version,$checksum"
    is AmuletCommand.GetProtocolVersion -> "GET_PROTOCOL_VERSION"
    is AmuletCommand.Custom -> {
        if (parameters.isEmpty()) command
        else "$command:${parameters.joinToString(":")}"
    }
}

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
