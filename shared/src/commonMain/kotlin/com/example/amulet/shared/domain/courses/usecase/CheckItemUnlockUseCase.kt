package com.example.amulet.shared.domain.courses.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.courses.CoursesRepository
import com.example.amulet.shared.domain.courses.model.CourseId
import com.example.amulet.shared.domain.courses.model.CourseItem
import com.example.amulet.shared.domain.courses.model.CourseItemId
import com.example.amulet.shared.domain.courses.model.UnlockCondition
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.flatMap
import kotlinx.coroutines.flow.firstOrNull

class CheckItemUnlockUseCase(
    private val coursesRepository: CoursesRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(courseId: CourseId, itemId: CourseItemId): AppResult<Boolean> =
        getCurrentUserIdUseCase().flatMap { userId ->
            val items = coursesRepository.getCourseItemsStream(courseId).firstOrNull() ?: return@flatMap Ok(false)
            val item = items.find { it.id == itemId } ?: return@flatMap Ok(false)

            val unlockCondition = item.unlockCondition ?: return@flatMap Ok(true)

            val progress = coursesRepository.getCourseProgressStream(userId, courseId).firstOrNull()
            val completedIds = progress?.completedItemIds ?: emptySet()

            Ok(evaluateCondition(unlockCondition, item, items, completedIds, progress?.percent ?: 0))
        }

    private fun evaluateCondition(
        condition: UnlockCondition,
        currentItem: CourseItem,
        allItems: List<CourseItem>,
        completedIds: Set<String>,
        percent: Int
    ): Boolean = when (condition) {
        is UnlockCondition.CompletePreviousItem -> {
            val prev = allItems.filter { it.order < currentItem.order }.maxByOrNull { it.order }
            prev?.let { completedIds.contains(it.id) } ?: true
        }
        is UnlockCondition.CompleteSpecificItem -> completedIds.contains(condition.itemId)
        is UnlockCondition.CompleteMultipleItems -> 
            condition.itemIds.count { completedIds.contains(it) } >= condition.count
        is UnlockCondition.MinimumProgress -> percent >= condition.percent
        is UnlockCondition.And -> condition.conditions.all { evaluateCondition(it, currentItem, allItems, completedIds, percent) }
        is UnlockCondition.Or -> condition.conditions.any { evaluateCondition(it, currentItem, allItems, completedIds, percent) }
    }
}
