package com.example.amulet.core.ble.model

import com.example.amulet.shared.domain.devices.model.AmuletCommand
import android.util.Base64

/**
 * Extension функции для преобразования доменных команд в BLE протокол.
 * Согласно спецификации из docs/20_DATA_LAYER/03_BLE_PROTOCOL.md
 */

/**
 * Преобразование команды в строку протокола BLE.
 */
fun AmuletCommand.toCommandString(): String = when (this) {
    is AmuletCommand.SetRing -> "SET_RING:${colors.joinToString(":") { it.toHex() }}"
    is AmuletCommand.SetLed -> "SET_LED:$index:${color.toHex()}"
    is AmuletCommand.ClearAll -> "CLEAR_ALL"
    is AmuletCommand.Delay -> "DELAY:$durationMs"
    is AmuletCommand.Play -> "PLAY:$patternId"
    is AmuletCommand.HasPlan -> "HAS_PLAN:$patternId"
    is AmuletCommand.BeginPracticeScript -> "BEGIN_PRACTICE:$practiceId"
    is AmuletCommand.AddPracticeStep -> "ADD_PRACTICE_STEP:$practiceId:$order:$patternId"
    is AmuletCommand.CommitPracticeScript -> "COMMIT_PRACTICE:$practiceId"
    is AmuletCommand.HasPracticeScript -> "HAS_PRACTICE:$practiceId"
    is AmuletCommand.PlayPracticeScript -> "PLAY_PRACTICE:$practiceId"
    is AmuletCommand.SetWifiCred -> "SET_WIFI_CRED:$ssidBase64:$passwordBase64"
    is AmuletCommand.WifiOtaStart -> "WIFI_OTA_START:$url:$version:$checksum"
    is AmuletCommand.GetProtocolVersion -> "GET_PROTOCOL_VERSION"
    is AmuletCommand.Custom -> if (parameters.isEmpty()) command else "$command:${parameters.joinToString(":")}" 
}

fun ByteArray.toBase64(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}
