package com.example.amulet.shared.domain.courses.usecase

import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.courses.CoursesRepository
import com.example.amulet.shared.domain.courses.model.CourseId
import com.example.amulet.shared.domain.courses.model.CourseProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class GetCourseProgressStreamUseCase(
    private val repository: CoursesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(courseId: CourseId): Flow<CourseProgress?> =
        observeCurrentUserIdUseCase().flatMapLatest { userId ->
            userId?.let { repository.getCourseProgressStream(it, courseId) } 
                ?: flowOf(null)
        }
}
