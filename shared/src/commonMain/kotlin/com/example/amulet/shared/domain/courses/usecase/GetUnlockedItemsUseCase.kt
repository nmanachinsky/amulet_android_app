package com.example.amulet.shared.domain.courses.usecase

import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.courses.CoursesRepository
import com.example.amulet.shared.domain.courses.model.CourseId
import com.example.amulet.shared.domain.courses.model.CourseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class GetUnlockedItemsUseCase(
    private val coursesRepository: CoursesRepository,
    private val checkItemUnlockUseCase: CheckItemUnlockUseCase,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    operator fun invoke(courseId: CourseId): Flow<List<CourseItem>> =
        observeCurrentUserIdUseCase().flatMapLatest { userId ->
            if (userId == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                combine(
                    coursesRepository.getCourseItemsStream(courseId),
                    coursesRepository.getCourseProgressStream(userId, courseId)
                ) { items, _ ->
                    items.filter { item ->
                        checkItemUnlockUseCase(courseId, item.id).component1() ?: false
                    }
                }
            }
        }
}
