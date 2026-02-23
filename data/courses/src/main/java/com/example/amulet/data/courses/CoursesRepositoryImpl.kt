package com.example.amulet.data.courses

import com.example.amulet.data.courses.datasource.LocalCoursesDataSource
import com.example.amulet.data.courses.datasource.RemoteCoursesDataSource
import com.example.amulet.data.courses.mapper.toDomain
import com.example.amulet.data.courses.mapper.toJsonArrayString
import com.example.amulet.data.courses.seed.CourseSeed
import com.example.amulet.data.courses.seed.CourseSeedData
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.courses.CoursesRepository
import com.example.amulet.shared.domain.courses.model.Course
import com.example.amulet.shared.domain.courses.model.CourseId
import com.example.amulet.shared.domain.courses.model.CourseItem
import com.example.amulet.shared.domain.courses.model.CourseItemId
import com.example.amulet.shared.domain.courses.model.CourseModule
import com.example.amulet.shared.domain.courses.model.CourseProgress
import com.example.amulet.shared.domain.practices.model.PracticeId
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoursesRepositoryImpl @Inject constructor(
    private val local: LocalCoursesDataSource,
    private val remote: RemoteCoursesDataSource,
    private val json: Json
) : CoursesRepository {


    override fun getCoursesStream(): Flow<List<Course>> =
        local.observeCourses().map { it.map { e -> e.toDomain(json) } }

    override fun getCourseById(id: CourseId): Flow<Course?> =
        local.observeCourseById(id).map { it?.toDomain(json) }

    override fun getCourseItemsStream(courseId: CourseId): Flow<List<CourseItem>> =
        local.observeCourseItems(courseId).map { it.map { i -> i.toDomain() } }

    override fun getCourseModulesStream(courseId: CourseId): Flow<List<CourseModule>> =
        local.observeCourseModules(courseId).map { list -> list.map { it.toDomain() } }

    override fun getCourseProgressStream(userId: UserId, courseId: CourseId): Flow<CourseProgress?> =
        local.observeCourseProgress(userId.value, courseId).map { it?.toDomain(json) }

    override fun getAllCoursesProgressStream(userId: UserId): Flow<List<CourseProgress>> =
        local.observeAllProgress(userId.value).map { list -> list.map { it.toDomain(json) } }

    override fun getCoursesByPracticeId(practiceId: PracticeId): Flow<List<Course>> =
        local.observeCoursesByPracticeId(practiceId).map { entities ->
            entities.map { it.toDomain(json) }
        }

    override suspend fun refreshCatalog(): AppResult<Unit> {
        Logger.d("Начало обновления каталога курсов", "CoursesRepositoryImpl")
        return try {
            // Пытаемся получить курсы с сервера
            val remoteResult = remote.refreshCatalog()
            remoteResult.onFailure { error ->
                Logger.e("Ошибка получения курсов с сервера: $error", throwable = Exception(error.toString()), tag = "CoursesRepositoryImpl")
                Logger.d("Переходим к сидированию предустановленных курсов (офлайн)", "CoursesRepositoryImpl")
                val courses: List<CourseSeed> = CourseSeedData.getCourses()
                val courseItems = CourseSeedData.getCourseItems()
                val courseModules = CourseSeedData.getCourseModules()
                val seeds = courses.map { seed ->
                    seed.copy(
                        items = courseItems[seed.id] ?: emptyList(),
                        modules = courseModules[seed.id] ?: emptyList()
                    )
                }
                local.seedPresets(seeds)
                Logger.d("Сидирование курсов завершено: ${seeds.size} курсов", "CoursesRepositoryImpl")
                return Ok(Unit)
            }
            // Если сервер вернул данные, используем их
            Logger.d("Курсы получены с сервера", "CoursesRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка обновления каталога курсов: $e", throwable = e, tag = "CoursesRepositoryImpl")
            Err(AppError.Unknown)
        }
    }

    override suspend fun seedLocalData(): AppResult<Unit> {
        Logger.d("Начало локального сидирования курсов", "CoursesRepositoryImpl")
        return try {
            val courses: List<CourseSeed> = CourseSeedData.getCourses()
            val courseItems = CourseSeedData.getCourseItems()
            val courseModules = CourseSeedData.getCourseModules()
            val seeds = courses.map { seed ->
                seed.copy(
                    items = courseItems[seed.id] ?: emptyList(),
                    modules = courseModules[seed.id] ?: emptyList()
                )
            }
            local.seedPresets(seeds)
            Logger.d("Локальное сидирование курсов завершено: ${seeds.size} курсов", "CoursesRepositoryImpl")
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("Ошибка локального сидирования курсов: $e", throwable = e, tag = "CoursesRepositoryImpl")
            Err(AppError.Unknown)
        }
    }

    override suspend fun startCourse(userId: UserId, courseId: CourseId): AppResult<CourseProgress> {
        val items = local.observeCourseItems(courseId).first()
        val now = System.currentTimeMillis()
        val progress = com.example.amulet.core.database.entity.CourseProgressEntity(
            userId = userId.value,
            courseId = courseId,
            completedItemIdsJson = emptyList<String>().toJsonArrayString(json),
            currentItemId = items.minByOrNull { it.order }?.id,
            percent = 0,
            totalTimeSec = 0,
            updatedAt = now
        )
        local.upsertProgress(progress)
        return Ok(progress.toDomain(json))
    }

    override suspend fun continueCourse(userId: UserId, courseId: CourseId): AppResult<CourseItemId?> {
        val p = local.observeCourseProgress(userId.value, courseId).first()
        return Ok(p?.currentItemId)
    }

    override suspend fun completeItem(userId: UserId, courseId: CourseId, itemId: CourseItemId): AppResult<CourseProgress> {
        val progress = local.observeCourseProgress(userId.value, courseId).first()
            ?: return Err(AppError.NotFound)
        val items = local.observeCourseItems(courseId).first().sortedBy { it.order }
        val completedSet = progress.toDomain(json).completedItemIds.toMutableSet()
        completedSet.add(itemId)
        val next = items.firstOrNull { it.id !in completedSet }?.id
        val percent = if (items.isEmpty()) 0 else (completedSet.size * 100 / items.size)
        val updated = progress.copy(
            completedItemIdsJson = completedSet.toList().toJsonArrayString(json),
            currentItemId = next,
            percent = percent,
            updatedAt = System.currentTimeMillis()
        )
        local.upsertProgress(updated)
        return Ok(updated.toDomain(json))
    }

    override suspend fun resetProgress(userId: UserId, courseId: CourseId): AppResult<Unit> {
        local.resetProgress(userId.value, courseId)
        return Ok(Unit)
    }
}
