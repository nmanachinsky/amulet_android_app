package com.example.amulet.data.courses.datasource

import com.example.amulet.core.database.dao.CourseDao
import com.example.amulet.core.database.entity.CourseEntity
import com.example.amulet.core.database.entity.CourseItemEntity
import com.example.amulet.core.database.entity.CourseModuleEntity
import com.example.amulet.core.database.entity.CourseProgressEntity
import com.example.amulet.data.courses.mapper.toEntity
import com.example.amulet.data.courses.seed.CourseSeed
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCoursesDataSourceImpl @Inject constructor(
    private val dao: CourseDao
) : LocalCoursesDataSource {
    override fun observeCourses(): Flow<List<CourseEntity>> = dao.observeCourses()
    override fun observeCourseById(courseId: String): Flow<CourseEntity?> = dao.observeCourseById(courseId)
    override fun observeCourseItems(courseId: String): Flow<List<CourseItemEntity>> = dao.observeCourseItems(courseId)
    override fun observeCourseModules(courseId: String): Flow<List<CourseModuleEntity>> = dao.observeCourseModules(courseId)
    override fun observeCourseProgress(userId: String, courseId: String): Flow<CourseProgressEntity?> = dao.observeCourseProgress(userId, courseId)
    override fun observeAllProgress(userId: String): Flow<List<CourseProgressEntity>> = dao.observeAllProgress(userId)
    override fun observeCoursesByPracticeId(practiceId: String): Flow<List<CourseEntity>> = dao.observeCoursesByPracticeId(practiceId)
    
    override suspend fun upsertCourses(items: List<CourseEntity>) { dao.upsertCourses(items) }
    override suspend fun upsertCourseItems(items: List<CourseItemEntity>) { dao.upsertCourseItems(items) }
    override suspend fun upsertCourseModules(items: List<CourseModuleEntity>) { dao.upsertCourseModules(items) }
    override suspend fun upsertProgress(entity: CourseProgressEntity) { dao.upsertProgress(entity) }
    override suspend fun resetProgress(userId: String, courseId: String) { dao.resetProgress(userId, courseId) }

    override suspend fun seedPresets(presets: List<CourseSeed>) {
        if (presets.isEmpty()) return
        dao.upsertCourses(presets.map { it.toEntity() })
        dao.upsertCourseModules(presets.flatMap { course -> course.modules.map { it.toEntity() } })
        dao.upsertCourseItems(presets.flatMap { course -> course.items.map { it.toEntity() } })
    }
}
