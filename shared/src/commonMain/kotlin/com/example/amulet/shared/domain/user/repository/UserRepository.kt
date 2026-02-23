package com.example.amulet.shared.domain.user.repository

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.user.model.UpdateUserProfileRequest
import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun fetchProfile(userId: UserId): AppResult<User>

    fun observeUser(userId: UserId): Flow<User?>

    /**
     * Обновляет профиль текущего пользователя на backend и в локальном кэше.
     *
     * Реализация должна:
     * - вызвать API обновления профиля;
     * - замапить ответ в доменную модель User;
     * - сохранить свежие данные в локальное хранилище, чтобы их увидел ObserveCurrentUserUseCase.
     */
    suspend fun updateProfile(request: UpdateUserProfileRequest): AppResult<User>
}
