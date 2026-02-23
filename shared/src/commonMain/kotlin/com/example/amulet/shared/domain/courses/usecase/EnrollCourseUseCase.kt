package com.example.amulet.shared.domain.courses.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.courses.CoursesRepository
import com.example.amulet.shared.domain.courses.model.CourseId
import com.example.amulet.shared.domain.courses.model.EnrollmentParams
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.flatMap
import kotlinx.coroutines.flow.first

class EnrollCourseUseCase(
    private val coursesRepository: CoursesRepository,
    private val practicesRepository: PracticesRepository,
    private val createScheduleForCourseUseCase: CreateScheduleForCourseUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(params: EnrollmentParams): AppResult<Int> =
        getCurrentUserIdUseCase().flatMap { userId ->
            val courseId = params.courseId

            val courseItems = coursesRepository.getCourseItemsStream(courseId).first()
            
            if (courseItems.isEmpty()) {
                return Err(AppError.NotFound)
            }
            
            val progress = coursesRepository.getCourseProgressStream(userId, courseId).first()
            if (progress == null || progress.percent == 0) {
                coursesRepository.startCourse(userId, courseId)
            }
            
            val schedules = createScheduleForCourseUseCase(params = params, courseItems = courseItems)
            
            schedules.forEach { schedule ->
                practicesRepository.upsertSchedule(userId, schedule)
            }
            
            Ok(schedules.size)
        }
}
