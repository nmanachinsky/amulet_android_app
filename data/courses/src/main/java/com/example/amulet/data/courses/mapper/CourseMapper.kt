package com.example.amulet.data.courses.mapper

import com.example.amulet.core.database.entity.CourseEntity
import com.example.amulet.core.database.entity.CourseItemEntity
import com.example.amulet.core.database.entity.CourseProgressEntity
import com.example.amulet.shared.domain.courses.model.Course
import com.example.amulet.shared.domain.courses.model.CourseItem
import com.example.amulet.shared.domain.courses.model.CourseItemType
import com.example.amulet.shared.domain.courses.model.CourseProgress
import com.example.amulet.shared.domain.courses.model.CourseRhythm
import com.example.amulet.shared.domain.courses.model.UnlockCondition
import com.example.amulet.shared.domain.practices.model.PracticeGoal
import com.example.amulet.shared.domain.practices.model.PracticeLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.decodeFromString

fun CourseEntity.toDomain(json: Json): Course = Course(
    id = id,
    title = title,
    description = description,
    goal = goal?.let { PracticeGoal.valueOf(it) },
    level = level?.let { PracticeLevel.valueOf(it) },
    rhythm = rhythm?.let { CourseRhythm.valueOf(it) } ?: CourseRhythm.DAILY,
    tags = tagsJson.safeParseStringList(json),
    totalDurationSec = totalDurationSec,
    modulesCount = modulesCount,
    recommendedDays = recommendedDays,
    difficulty = difficulty,
    coverUrl = coverUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CourseItemEntity.toDomain(): CourseItem = CourseItem(
    id = id,
    courseId = courseId,
    order = order,
    type = CourseItemType.valueOf(type),
    practiceId = practiceId,
    title = title,
    description = description,
    mandatory = mandatory,
    minDurationSec = minDurationSec,
    moduleId = moduleId?.let { com.example.amulet.shared.domain.courses.model.CourseModuleId(it) },
    unlockCondition = unlockConditionJson?.let { 
        runCatching { Json.decodeFromString<UnlockCondition>(it) }.getOrNull() 
    }
)

fun CourseProgressEntity.toDomain(json: Json): CourseProgress = CourseProgress(
    courseId = courseId,
    completedItemIds = completedItemIdsJson.safeParseStringSet(json),
    currentItemId = currentItemId,
    percent = percent,
    totalTimeSec = totalTimeSec,
    updatedAt = updatedAt
)

fun List<String>.toJsonArrayString(json: Json): String =
    json.encodeToString(JsonArray(this.map { JsonPrimitive(it) }))

fun List<String>.toJsonArrayString(): String =
    toJsonArrayString(Json)

fun UnlockCondition.toJson(): String =
    Json.encodeToString(this)

private fun String?.safeParseStringList(json: Json): List<String> =
    runCatching {
        if (this.isNullOrBlank()) emptyList() else json.parseToJsonElement(this).jsonArray.map { it.jsonPrimitive.content }
    }.getOrElse { emptyList() }

private fun String?.safeParseStringSet(json: Json): Set<String> =
    safeParseStringList(json).toSet()
