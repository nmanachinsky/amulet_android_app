package com.example.amulet.data.practices.mapper

import com.example.amulet.core.database.entity.PracticeCategoryEntity
import com.example.amulet.core.database.entity.PracticeEntity
import com.example.amulet.core.database.entity.PracticeSessionEntity
import com.example.amulet.core.database.entity.UserPreferencesEntity
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.practices.model.Practice
import com.example.amulet.shared.domain.practices.model.PracticeAudioMode
import com.example.amulet.shared.domain.practices.model.PracticeCategory
import com.example.amulet.shared.domain.practices.model.PracticeGoal
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticeLevel
import com.example.amulet.shared.domain.practices.model.PracticeScript
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeStep
import com.example.amulet.shared.domain.practices.model.PracticeStepType
import com.example.amulet.shared.domain.practices.model.PracticeStepType.BODY_SCAN
import com.example.amulet.shared.domain.practices.model.PracticeStepType.BREATH_STEP
import com.example.amulet.shared.domain.practices.model.PracticeStepType.SOUND_SCAPE
import com.example.amulet.shared.domain.practices.model.PracticeStepType.TEXT_HINT
import com.example.amulet.shared.domain.practices.model.parsePracticeSessionSource
import com.example.amulet.shared.domain.practices.model.PracticeSessionStatus
import com.example.amulet.shared.domain.practices.model.PracticeType
import com.example.amulet.shared.domain.user.model.UserPreferences
import com.example.amulet.shared.domain.patterns.model.PatternId
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.devices.model.DeviceId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

fun PracticeEntity.toDomain(): Practice {
    val tags = try {
        if (tagsJson.isBlank() || tagsJson == "[]") emptyList()
        else kotlinx.serialization.json.Json.decodeFromString<List<String>>(tagsJson)
    } catch (e: Exception) { emptyList() }
    
    val contraindications = try {
        if (contraindicationsJson.isBlank() || contraindicationsJson == "[]") emptyList()
        else kotlinx.serialization.json.Json.decodeFromString<List<String>>(contraindicationsJson)
    } catch (e: Exception) { emptyList() }
    
    val stepsJsonLocal = stepsJson
    val steps = try {
        if (stepsJsonLocal.isNullOrBlank() || stepsJsonLocal == "[]") emptyList()
        else kotlinx.serialization.json.Json.decodeFromString<List<String>>(stepsJsonLocal)
    } catch (e: Exception) { emptyList() }

    val safetyNotesJsonLocal = safetyNotesJson
    val safetyNotes = try {
        if (safetyNotesJsonLocal.isNullOrBlank() || safetyNotesJsonLocal == "[]") emptyList()
        else kotlinx.serialization.json.Json.decodeFromString<List<String>>(safetyNotesJsonLocal)
    } catch (e: Exception) { emptyList() }

    val practiceType = PracticeType.valueOf(type)

    val scriptJsonLocal = scriptJson
    val scriptFromDb = try {
        if (scriptJsonLocal.isNullOrBlank()) null
        else kotlinx.serialization.json.Json.decodeFromString<PracticeScript>(scriptJsonLocal)
    } catch (e: Exception) { null }

    val script = scriptFromDb
        ?: if (steps.isEmpty()) {
            null
        } else {
            // Без детального скрипта длительность поровну распределяется по шагам -- иначе
            // логика перехода (computeCurrentStep) при durationSec=null прыгает на последний
            // шаг и замирает. Касается всех типов, включая MEDITATION.
            val perStepDurationSec: Int? = durationSec?.takeIf { it > 0 && steps.isNotEmpty() }
                ?.let { total -> (total / steps.size).coerceAtLeast(1) }

            PracticeScript(
                steps = steps.mapIndexed { index, text ->
                    val stepType = when (practiceType) {
                        PracticeType.BREATH -> BREATH_STEP
                        PracticeType.SOUND -> SOUND_SCAPE
                        PracticeType.MEDITATION -> if (title.contains("сканирование тела", ignoreCase = true)) {
                            BODY_SCAN
                        } else {
                            TEXT_HINT
                        }
                    }
                    PracticeStep(
                        order = index,
                        type = stepType,
                        title = null,
                        description = text,
                        durationSec = perStepDurationSec,
                        patternId = null,
                        audioUrl = null,
                        extra = emptyMap()
                    )
                }
            )
        }

    val hasDeviceScript = script?.steps?.any { it.patternId != null } == true
    
    return Practice(
        id = id,
        type = practiceType,
        title = title,
        description = description,
        durationSec = durationSec,
        level = level?.let { PracticeLevel.valueOf(it) },
        goal = goal?.let { PracticeGoal.valueOf(it) },
        tags = tags,
        contraindications = contraindications,
        patternId = patternId?.let { PatternId(it) },
        audioUrl = audioUrl,
        isFavorite = false, // Set separately via favorites table
        usageCount = usageCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        steps = steps,
        safetyNotes = safetyNotes,
        script = script,
        hasDeviceScript = hasDeviceScript,
    )
}

fun PracticeSessionEntity.toDomain(): PracticeSession = PracticeSession(
    id = id,
    userId = UserId(userId),
    practiceId = practiceId,
    deviceId = deviceId?.let { DeviceId(it) },
    status = PracticeSessionStatus.valueOf(status),
    startedAt = startedAt,
    completedAt = completedAt,
    durationSec = durationSec,
    intensity = intensity,
    brightness = brightness,
    completed = completed,
    moodBefore = moodBefore?.let { runCatching { MoodKind.valueOf(it) }.getOrNull() },
    moodAfter = moodAfter?.let { runCatching { MoodKind.valueOf(it) }.getOrNull() },
    feedbackNote = feedbackNote,
    source = parsePracticeSessionSource(source),
    actualDurationSec = actualDurationSec,
    vibrationLevel = vibrationLevel,
    audioMode = audioMode?.let {
        runCatching { PracticeAudioMode.valueOf(it) }.getOrNull()
    },
    rating = rating
)

fun PracticeCategoryEntity.toDomain(): PracticeCategory = PracticeCategory(
    id = id,
    title = title,
    order = order
)

fun UserPreferencesEntity.toDomain(
    goals: List<String>,
    interests: List<String>,
    durations: List<Int>
): UserPreferences = UserPreferences(
    defaultIntensity = defaultIntensity,
    defaultBrightness = defaultBrightness,
    goals = goals,
    interests = interests,
    preferredDurationsSec = durations,
    hugsDndEnabled = hugsDndEnabled ?: false,
    defaultHugEmotionId = defaultHugEmotionId,
)

fun UserPreferencesEntity?.toDomain(json: Json): UserPreferences {
    if (this == null) return UserPreferences()

    val goals = runCatching {
        json.parseToJsonElement(goalsJson).jsonArray.map { it.jsonPrimitive.content }
    }.getOrElse { emptyList() }

    val interests = runCatching {
        json.parseToJsonElement(interestsJson).jsonArray.map { it.jsonPrimitive.content }
    }.getOrElse { emptyList() }

    val durations = runCatching {
        json.parseToJsonElement(preferredDurationsJson).jsonArray.mapNotNull { je ->
            je.jsonPrimitive.content.toIntOrNull()
        }
    }.getOrElse { emptyList() }

    val base = toDomain(
        goals = goals,
        interests = interests,
        durations = durations,
    )

    return base.copy(
        defaultAudioMode = defaultAudioMode?.let { name ->
            runCatching { PracticeAudioMode.valueOf(name) }.getOrNull()
        }
    )
}

fun UserPreferences.toEntity(
    userId: String,
    json: Json,
): UserPreferencesEntity {
    val goalsJson = json.encodeToString(json.encodeToJsonElement(goals))
    val interestsJson = json.encodeToString(json.encodeToJsonElement(interests))
    val durationsJson = json.encodeToString(json.encodeToJsonElement(preferredDurationsSec))

    return UserPreferencesEntity(
        userId = userId,
        defaultIntensity = defaultIntensity,
        defaultBrightness = defaultBrightness,
        goalsJson = goalsJson,
        interestsJson = interestsJson,
        preferredDurationsJson = durationsJson,
        defaultAudioMode = defaultAudioMode?.name,
        hugsDndEnabled = hugsDndEnabled,
        defaultHugColorHex = null,
        defaultHugPatternId = null,
        defaultHugEmotionId = defaultHugEmotionId,
    )
}
