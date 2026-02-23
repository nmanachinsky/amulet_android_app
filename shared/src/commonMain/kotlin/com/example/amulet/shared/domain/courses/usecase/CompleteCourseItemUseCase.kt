package com.example.amulet.shared.domain.courses.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.courses.CoursesRepository
import com.example.amulet.shared.domain.courses.model.CourseId
import com.example.amulet.shared.domain.courses.model.CourseItemId
import com.example.amulet.shared.domain.courses.model.CourseProgress
import com.github.michaelbull.result.flatMap

class CompleteCourseItemUseCase(
    private val repository: CoursesRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        courseId: CourseId,
        itemId: CourseItemId
    ): AppResult<CourseProgress> =
        getCurrentUserIdUseCase().flatMap { userId ->
            repository.completeItem(userId, courseId, itemId)
        }
}
