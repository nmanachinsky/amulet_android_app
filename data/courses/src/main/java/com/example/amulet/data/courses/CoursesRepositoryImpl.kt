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
        local.observeCourses().map { list -> list.map { it.toDomain(json) } }

    override fun getCourseById(id: CourseId): Flow<Course?> =
        local.observeCourseById(id).map { it?.toDomain(json) }

    override fun getCourseItemsStream(courseId: CourseId): Flow<List<CourseItem>> =
        local.observeCourseItems(courseId).map { list -> list.map { it.toDomain() } }

    override fun getCourseModulesStream(courseId: CourseId): Flow<List<CourseModule>> =
        local.observeCourseModules(courseId).map { list -> list.map { it.toDomain() } }

    override fun getCourseProgressStream(userId: UserId, courseId: CourseId): Flow<CourseProgress?> =
        local.observeCourseProgress(userId.value, courseId).map { it?.toDomain(json) }

    override fun getAllCoursesProgressStream(userId: UserId): Flow<List<CourseProgress>> =
        local.observeAllProgress(userId.value).map { list -> list.map { it.toDomain(json) } }

    override fun getCoursesByPracticeId(practiceId: PracticeId): Flow<List<Course>> =
        local.observeCoursesByPracticeId(practiceId).map { list -> list.map { it.toDomain(json) } }

    override suspend fun refreshCatalog(): AppResult<Unit> {
        return try {
            val remoteResult = remote.refreshCatalog()
            remoteResult.onFailure {
                val seeds = buildSeedCourses()
                local.seedPresets(seeds)
            }
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("refreshCatalog failed", throwable = e, tag = "CoursesRepository")
            Err(AppError.Unknown)
        }
    }

    override suspend fun seedLocalData(): AppResult<Unit> {
        return try {
            local.seedPresets(buildSeedCourses())
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("seedLocalData failed", throwable = e, tag = "CoursesRepository")
            Err(AppError.Unknown)
        }
    }

    private fun buildSeedCourses(): List<CourseSeed> {
        val courses = CourseSeedData.getCourses()
        val items = CourseSeedData.getCourseItems()
        val modules = CourseSeedData.getCourseModules()
        return courses.map { seed ->
            seed.copy(
                items = items[seed.id] ?: emptyList(),
                modules = modules[seed.id] ?: emptyList()
            )
        }
    }

    override suspend fun startCourse(userId: UserId, courseId: CourseId): AppResult<CourseProgress> {
        val items = local.observeCourseItems(courseId).first()
        val firstItem = items.minByOrNull { it.order }?.id
        val now = System.currentTimeMillis()
        val progress = com.example.amulet.core.database.entity.CourseProgressEntity(
            userId = userId.value,
            courseId = courseId,
            completedItemIdsJson = emptyList<String>().toJsonArrayString(json),
            currentItemId = firstItem,
            percent = 0,
            totalTimeSec = 0,
            updatedAt = now
        )
        local.upsertProgress(progress)
        return Ok(progress.toDomain(json))
    }

    override suspend fun continueCourse(userId: UserId, courseId: CourseId): AppResult<CourseItemId?> {
        val progress = local.observeCourseProgress(userId.value, courseId).first()
        return Ok(progress?.currentItemId)
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
