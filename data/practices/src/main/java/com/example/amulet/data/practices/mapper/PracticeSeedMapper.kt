package com.example.amulet.data.practices.mapper

import com.example.amulet.core.database.entity.PracticeCategoryEntity
import com.example.amulet.core.database.entity.PracticeEntity
import com.example.amulet.data.practices.seed.PracticeScriptSeedData
import com.example.amulet.data.practices.seed.PracticeSeed
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/**
 * Мапперы для преобразования PracticeSeed в Entity.
 */

fun PracticeSeed.toEntity(): PracticeEntity = PracticeEntity(
    id = id,
    type = type.name,
    title = title,
    description = description,
    durationSec = durationSec,
    level = level?.name,
    goal = goal?.name,
    tagsJson = tags.toJsonArrayString(),
    contraindicationsJson = contraindications.toJsonArrayString(),
    patternId = patternId,
    audioUrl = audioUrl,
    usageCount = usageCount,
    localesJson = "[]", // Пустой JSON массив для локалей
    createdAt = createdAt,
    updatedAt = updatedAt,
    stepsJson = if (steps.isEmpty()) null else steps.toJsonArrayString(),
    safetyNotesJson = if (safetyNotes.isEmpty()) null else safetyNotes.toJsonArrayString(),
    scriptJson = PracticeScriptSeedData.getScriptForPractice(id)?.let { Json.encodeToString(it) }
)

fun PracticeSeed.toCategoryEntity(): PracticeCategoryEntity = PracticeCategoryEntity(
    id = category?.lowercase()?.replace(" ", "_") ?: "other",
    title = category ?: "Другое",
    order = 0
)

private fun List<String>.toJsonArrayString(): String =
    Json.encodeToString(JsonArray(this.map { JsonPrimitive(it) }))
