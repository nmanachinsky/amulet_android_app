package com.example.amulet.data.practices

import com.example.amulet.data.practices.datasource.LocalPracticesDataSource
import com.example.amulet.data.practices.datasource.RemotePracticesDataSource
import com.example.amulet.data.practices.mapper.toDomain
import com.example.amulet.data.practices.mapper.toDomain as toExtrasDomain
import com.example.amulet.data.practices.mapper.toEntity
import com.example.amulet.data.practices.seed.PracticeSeedData
import com.example.amulet.data.practices.seed.toSeed
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.Practice
import com.example.amulet.shared.domain.practices.model.PracticeBadge
import com.example.amulet.shared.domain.practices.model.PracticeCategory
import com.example.amulet.shared.domain.practices.model.PracticeCollection
import com.example.amulet.shared.domain.practices.model.PracticeFilter
import com.example.amulet.shared.domain.practices.model.PracticeGoal
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.practices.model.PracticePlan
import com.example.amulet.shared.domain.practices.model.PracticeSchedule
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionId
import com.example.amulet.shared.domain.practices.model.PracticeSessionStatus
import com.example.amulet.shared.domain.practices.model.PracticeStatistics
import com.example.amulet.shared.domain.practices.model.PracticeTag
import com.example.amulet.shared.domain.practices.model.ScheduledSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionSource
import com.example.amulet.shared.domain.practices.model.toStorageString
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.user.model.UserPreferences
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.practices.model.PracticeAudioMode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticesRepositoryImpl @Inject constructor(
    private val local: LocalPracticesDataSource,
    private val remote: RemotePracticesDataSource,
    private val json: Json
) : PracticesRepository {

    override fun getPracticesStream(userId: UserId, filter: PracticeFilter): Flow<List<Practice>> {
        val base = local.observePractices().map { it.map { e -> e.toDomain() } }
        val filtered = base.map { list ->
            val from = filter.durationFromSec
            val to = filter.durationToSec
            list.filter { p ->
                (filter.type == null || p.type == filter.type) &&
                (filter.categoryId == null || true) &&
                (from == null || (p.durationSec ?: Int.MAX_VALUE) >= from) &&
                (to == null || (p.durationSec ?: 0) <= to) &&
                (filter.goal == null || p.goal == filter.goal)
            }
        }
        return if (filter.onlyFavorites) {
            combine(filtered, local.observeFavoriteIds(userId.value)) { practices, favIds ->
                practices.filter { it.id in favIds }
            }
        } else filtered
    }

    override fun getPracticeById(userId: UserId, id: PracticeId): Flow<Practice?> =
        local.observePracticeByIdWithFavorites(id).map { relation ->
            relation?.practice?.toDomain()?.copy(
                isFavorite = relation.favorites.any { it.userId == userId.value }
            )
        }

    override fun getCategoriesStream(): Flow<List<PracticeCategory>> =
        local.observeCategories().map { it.map { c -> c.toDomain() } }

    override fun getFavoritesStream(userId: UserId): Flow<List<Practice>> =
        combine(local.observePractices(), local.observeFavoriteIds(userId.value)) { entities, favIds ->
            entities.filter { it.id in favIds }.map { it.toDomain() }
        }

    override fun getRecommendationsStream(userId: UserId, limit: Int?, contextGoal: PracticeGoal?): Flow<List<Practice>> {
        val prefs = getUserPreferencesStream(userId)
        val recent = local.observeSessionsForUser(userId.value)
        val all = local.observePractices()
        return combine(all, prefs, recent) { practices, preferences, sessions ->
            val completedIds = sessions.filter { it.completed }.map { it.practiceId }.toSet()
            val prefDur = preferences.preferredDurationsSec
            val scored = practices.map { e ->
                val p = e.toDomain()
                val d = p.durationSec
                var score = (if (d != null && prefDur.any { kotlin.math.abs(it - d) <= 120 }) 2 else 0) +
                    (if (p.id !in completedIds) 1 else 0)
                
                if (contextGoal != null && p.goal == contextGoal) {
                    score += 5 // Boost score if matches context goal
                }
                
                p to score
            }
            scored.sortedByDescending { it.second }.map { it.first }.let { if (limit != null) it.take(limit) else it }
        }
    }

    override suspend fun search(userId: UserId, query: String, filter: PracticeFilter): AppResult<List<Practice>> {
        val list = getPracticesStream(userId, filter).first()
        return Ok(list.filter { it.title.contains(query, true) || (it.description?.contains(query, true) == true) })
    }

    override suspend fun upsertPractice(practice: Practice): AppResult<Unit> {
        val tagsJson = json.encodeToString(json.encodeToJsonElement(practice.tags))
        val contraindicationsJson = json.encodeToString(json.encodeToJsonElement(practice.contraindications))
        val stepsJson = practice.steps.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) }
        val safetyNotesJson = practice.safetyNotes.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) }
        val scriptJson = practice.script?.let { json.encodeToString(it) }

        val entity = com.example.amulet.core.database.entity.PracticeEntity(
            id = practice.id,
            type = practice.type.name,
            title = practice.title,
            description = practice.description,
            durationSec = practice.durationSec,
            level = practice.level?.name,
            goal = practice.goal?.name,
            tagsJson = tagsJson,
            contraindicationsJson = contraindicationsJson,
            patternId = practice.patternId?.value,
            audioUrl = practice.audioUrl,
            usageCount = practice.usageCount,
            localesJson = "[]",
            createdAt = practice.createdAt,
            updatedAt = practice.updatedAt,
            stepsJson = stepsJson,
            safetyNotesJson = safetyNotesJson,
            scriptJson = scriptJson,
        )

        local.upsertPractices(listOf(entity))
        return Ok(Unit)
    }

    override suspend fun refreshCatalog(): AppResult<Unit> {
        Logger.d("Начало обновления каталога практик", "PracticesRepositoryImpl")
        return try {
            // Пытаемся получить практики с сервера
            val remoteResult = remote.refreshCatalog()
            remoteResult.onFailure { error ->
                Logger.e("Ошибка получения практик с сервера: $error", throwable = Exception(error.toString()), tag = "PracticesRepositoryImpl")
                Logger.d("Переходим к сидированию предустановленных практик (офлайн)", "PracticesRepositoryImpl")
                val practices = PracticeSeedData.getPractices()
                val seeds = practices.map { it.toSeed() }
                local.seedPresets(seeds)
                Logger.d("Сидирование практик завершено: ${seeds.size} практик", "PracticesRepositoryImpl")
                return Ok(Unit)
            }

            // Если сервер вернул данные, используем их
            Logger.d("Практики получены с сервера", "PracticesRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка обновления каталога практик: $e", throwable = e, tag = "PracticesRepositoryImpl")
            Err(AppError.Unknown)
        }
    }

    override suspend fun seedLocalData(): AppResult<Unit> {
        Logger.d("Начало локального сидирования практик", "PracticesRepositoryImpl")
        return try {
            val practices = PracticeSeedData.getPractices()
            val seeds = practices.map { it.toSeed() }
            Logger.d("Практики для сидирования: ${seeds.size}, первые patternId: ${seeds.take(3).map { it.patternId }}", "PracticesRepositoryImpl")
            local.seedPresets(seeds)
            Logger.d("Локальное сидирование практик завершено: ${seeds.size} практик", "PracticesRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка локального сидирования практик: $e", throwable = e, tag = "PracticesRepositoryImpl")
            Err(AppError.Unknown)
        }
    }

    override suspend fun setFavorite(userId: UserId, practiceId: PracticeId, favorite: Boolean): AppResult<Unit> {
        local.setFavorite(userId.value, practiceId, favorite)
        return Ok(Unit)
    }

    override fun getActiveSessionStream(userId: UserId): Flow<PracticeSession?> {
        return local.observeSessionsForUser(userId.value).map { list ->
            list.firstOrNull { it.status == PracticeSessionStatus.ACTIVE.name }?.toDomain()
        }
    }

    override fun getSessionsHistoryStream(userId: UserId, limit: Int?): Flow<List<PracticeSession>> =
        local.observeSessionsForUser(userId.value).map { list ->
            val mapped = list.map { it.toDomain() }
            if (limit != null) mapped.take(limit) else mapped
        }

    override suspend fun startPractice(
        userId: UserId,
        practiceId: PracticeId,
        intensity: Double?,
        brightness: Double?,
        vibrationLevel: Double?,
        audioMode: com.example.amulet.shared.domain.practices.model.PracticeAudioMode?,
        source: PracticeSessionSource?,
    ): AppResult<PracticeSession> {
        val now = System.currentTimeMillis()
        val session = com.example.amulet.core.database.entity.PracticeSessionEntity(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId.value,
            practiceId = practiceId,
            deviceId = null,
            status = PracticeSessionStatus.ACTIVE.name,
            startedAt = now,
            completedAt = null,
            durationSec = null,
            completed = false,
            intensity = intensity,
            brightness = brightness,
            moodBefore = null,
            moodAfter = null,
            feedbackNote = null,
            source = source?.toStorageString(),
            actualDurationSec = null,
            vibrationLevel = vibrationLevel,
            audioMode = audioMode?.name,
            rating = null
        )
        local.upsertSession(session)
        return Ok(session.toDomain())
    }

    override suspend fun stopSession(sessionId: PracticeSessionId, completed: Boolean): AppResult<PracticeSession> {
        val current = local.getSessionById(sessionId) ?: return Err(AppError.NotFound)
        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = if (completed) PracticeSessionStatus.COMPLETED.name else PracticeSessionStatus.CANCELLED.name,
            completedAt = now,
            durationSec = (((now - current.startedAt) / 1000).toInt()).coerceAtLeast(0),
            completed = completed
        )
        local.upsertSession(updated)
        return Ok(updated.toDomain())
    }

    override suspend fun updateSessionMoodBefore(
        sessionId: PracticeSessionId,
        moodBefore: MoodKind?,
    ): AppResult<PracticeSession> {
        val current = local.getSessionById(sessionId) ?: return Err(AppError.NotFound)
        val updated = current.copy(
            moodBefore = moodBefore?.name,
        )
        local.upsertSession(updated)
        return Ok(updated.toDomain())
    }

    override suspend fun updateSessionFeedback(
        sessionId: PracticeSessionId,
        rating: Int?,
        moodAfter: MoodKind?,
        feedbackNote: String?,
    ): AppResult<PracticeSession> {
        val current = local.getSessionById(sessionId) ?: return Err(AppError.NotFound)
        val updated = current.copy(
            rating = rating,
            moodAfter = moodAfter?.name,
            feedbackNote = feedbackNote,
        )
        local.upsertSession(updated)
        return Ok(updated.toDomain())
    }

    override suspend fun skipScheduledSession(
        userId: UserId,
        session: ScheduledSession
    ): AppResult<Unit> {
        val source = PracticeSessionSource.ScheduleSkip(session.id).toStorageString()
        val entity = com.example.amulet.core.database.entity.PracticeSessionEntity(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId.value,
            practiceId = session.practiceId,
            deviceId = null,
            status = PracticeSessionStatus.CANCELLED.name,
            startedAt = session.scheduledTime,
            completedAt = session.scheduledTime,
            durationSec = 0,
            completed = false,
            intensity = null,
            brightness = null,
            moodBefore = null,
            moodAfter = null,
            feedbackNote = null,
            source = source,
            actualDurationSec = 0,
            vibrationLevel = null,
            audioMode = null,
            rating = null
        )
        local.upsertSession(entity)
        return Ok(Unit)
    }

    override fun getUserPreferencesStream(userId: UserId): Flow<UserPreferences> =
        local.observePreferences(userId.value).map { e ->
            e.toDomain(json)
        }

    override fun getSchedulesStream(userId: UserId): Flow<List<PracticeSchedule>> =
        local.observeSchedules(userId.value).map { list ->
            list.map { e -> e.toDomain(json) }
        }
    
    override fun getScheduleByPracticeId(userId: UserId, practiceId: PracticeId): Flow<PracticeSchedule?> =
        local.observeScheduleByPracticeId(userId.value, practiceId).map { entity ->
            entity?.toDomain(json)
        }

    override suspend fun upsertSchedule(
        userId: UserId,
        schedule: com.example.amulet.shared.domain.practices.model.PracticeSchedule
    ): AppResult<Unit> {
        val entity = schedule.toEntity(userId.value, json)
        local.upsertSchedule(entity)
        return Ok(Unit)
    }

    override suspend fun deleteSchedule(
        scheduleId: String
    ): AppResult<Unit> {
        local.deleteSchedule(scheduleId)
        return Ok(Unit)
    }

    override suspend fun updateUserPreferences(userId: UserId, preferences: UserPreferences): AppResult<Unit> {
        val entity = preferences.toEntity(userId.value, json)
        local.upsertPreferences(entity)
        return Ok(Unit)
    }

    // region Plans

    override fun getPlansStream(userId: UserId): Flow<List<PracticePlan>> =
        local.observePlans(userId.value).map { list -> list.map { it.toExtrasDomain() as PracticePlan } }

    override fun getPlanById(userId: UserId, id: String): Flow<PracticePlan?> =
        local.observePlanById(id).map { it?.toExtrasDomain() as PracticePlan }

    override fun getSchedulesByPlanStream(planId: String): Flow<List<PracticeSchedule>> =
        local.observeSchedulesByPlan(planId).map { list ->
            list.map { e ->
                PracticeSchedule(
                    id = e.id,
                    userId = e.userId,
                    practiceId = e.practiceId,
                    courseId = e.courseId,
                    daysOfWeek = json.decodeFromString<List<Int>>(e.daysOfWeekJson),
                    timeOfDay = e.timeOfDay,
                    reminderEnabled = e.reminderEnabled,
                    createdAt = e.createdAt,
                    planId = e.planId,
                    updatedAt = e.updatedAt
                )
            }
        }

    override suspend fun upsertPlan(userId: UserId, plan: PracticePlan): AppResult<Unit> {
        val entity = com.example.amulet.core.database.entity.PlanEntity(
            id = plan.id,
            userId = userId.value,
            title = plan.title,
            description = plan.description,
            status = plan.status,
            type = plan.type,
            createdAt = plan.createdAt,
            updatedAt = plan.updatedAt
        )
        local.upsertPlan(entity)
        return Ok(Unit)
    }

    override suspend fun deletePlan(planId: String): AppResult<Unit> {
        local.deletePlan(planId)
        return Ok(Unit)
    }

    // endregion

    // region Statistics & badges

    override fun getStatisticsStream(userId: UserId): Flow<PracticeStatistics?> =
        local.observeUserPracticeStats(userId.value).map { it?.toExtrasDomain() as PracticeStatistics }

    override fun getBadgesStream(userId: UserId): Flow<List<PracticeBadge>> =
        local.observeBadges(userId.value).map { list -> list.map { it.toExtrasDomain() as PracticeBadge } }

    // endregion

    // region Tags & collections

    override fun getPracticeTagsStream(): Flow<List<PracticeTag>> =
        local.observePracticeTags().map { list -> list.map { it.toExtrasDomain() as PracticeTag } }

    override fun getCollectionsStream(): Flow<List<PracticeCollection>> =
        local.observeCollections().map { collections ->
            // Для простоты сейчас не подтягиваем items по коллекции пачками;
            // предполагается, что вызов будет делать отдельные запросы при необходимости.
            // Здесь возвращаем коллекции без items.
            collections.map { c ->
                PracticeCollection(
                    id = c.id,
                    code = c.code,
                    title = c.title,
                    description = c.description,
                    order = c.order,
                    items = emptyList()
                )
            }
        }

    // endregion
}
