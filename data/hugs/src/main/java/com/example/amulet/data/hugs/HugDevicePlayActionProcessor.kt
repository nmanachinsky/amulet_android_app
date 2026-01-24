package com.example.amulet.data.hugs

import com.example.amulet.core.database.entity.OutboxActionEntity
import com.example.amulet.core.sync.processing.ActionProcessor
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.patterns.PatternPlaybackService
import com.example.amulet.shared.domain.patterns.PatternsRepository
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * ActionProcessor для отложенного воспроизведения hug-паттернов на амулете.
 *
 * Живёт в data-слое фичи "объятия" и подключается к OutboxEngine через Dagger map.
 */
class HugDevicePlayActionProcessor @Inject constructor(
    private val patternsRepository: PatternsRepository,
    private val playbackService: PatternPlaybackService,
    private val json: Json,
) : ActionProcessor {

    override suspend fun process(action: OutboxActionEntity): AppResult<Unit> {
        val payload = runCatching { json.parseToJsonElement(action.payloadJson).jsonObject }
            .getOrElse { return Err(AppError.Validation(mapOf("payload" to "Invalid JSON"))) }

        val hugId = payload["hugId"]?.jsonPrimitive?.contentOrNull
            ?: return Err(AppError.Validation(mapOf("hugId" to "Missing hugId")))

        val patternIdValue = payload["patternId"]?.jsonPrimitive?.contentOrNull
            ?: return Err(AppError.Validation(mapOf("patternId" to "Missing patternId")))

        val intensity = payload["intensity"]?.jsonPrimitive?.doubleOrNull ?: 1.0

        val pattern = patternsRepository.getPatternById(PatternId(patternIdValue)).firstOrNull()
            ?: return Err(AppError.NotFound)

        val result = playbackService.playOnConnectedDevice(
            spec = pattern.spec,
            intensity = intensity,
            isPreview = true,
        )

        return result.fold(
            success = { Ok(Unit) },
            failure = { error ->
                // Для команд воспроизведения hug-паттерна считаем временные BLE-проблемы
                // (нет устройства / отключено) аналогом сетевой ошибки.
                // Это позволяет движку Outbox применить стандартную стратегию ретраев
                // с экспоненциальным бэкоффом.
                val mapped = when (error) {
                    is AppError.BleError.DeviceNotFound,
                    is AppError.BleError.DeviceDisconnected -> AppError.Network
                    else -> error
                }
                Err(mapped)
            }
        )
    }
}
