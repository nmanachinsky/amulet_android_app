package com.example.amulet.shared.domain.courses.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.courses.CoursesRepository
import com.example.amulet.shared.domain.courses.model.CourseId
import com.example.amulet.shared.domain.courses.model.CourseItem
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.flatMap
import kotlinx.coroutines.flow.first

class GetNextCourseItemUseCase(
    private val coursesRepository: CoursesRepository,
    private val checkItemUnlockUseCase: CheckItemUnlockUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(courseId: CourseId): AppResult<CourseItem?> =
        getCurrentUserIdUseCase().flatMap { userId ->
            val items = coursesRepository.getCourseItemsStream(courseId).first()
            val progress = coursesRepository.getCourseProgressStream(userId, courseId).first()

            val completedIds = progress?.completedItemIds ?: emptySet()

            val candidates = items
                .filter { it.mandatory }
                .filter { it.id !in completedIds }
                .sortedBy { it.order }

            val nextItem = candidates.firstOrNull { item ->
                checkItemUnlockUseCase(courseId, item.id).component1() ?: false
            }

            Ok(nextItem)
        }
}
