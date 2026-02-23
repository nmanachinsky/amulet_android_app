package com.example.amulet.shared.domain.user.repository

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.privacy.model.UserConsents
import com.example.amulet.shared.domain.user.model.UpdateUserProfileRequest
import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Получает профиль пользователя с удаленного сервера.
     * При ошибке делает fallback на кэш.
     */
    suspend fun fetchProfile(userId: UserId): AppResult<User>

    /**
     * Загружает профиль в локальный кэш без возврата данных.
     * Используется для "разогрева" кэша после авторизации.
     */
    suspend fun preloadProfileToCache(userId: UserId): AppResult<Unit>

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

    /**
     * Возвращает поток согласий пользователя.
     */
    fun getUserConsentsStream(userId: UserId): Flow<UserConsents>

    /**
     * Обновляет согласия пользователя на backend и в локальном кэше.
     */
    suspend fun updateUserConsents(userId: UserId, consents: UserConsents): AppResult<Unit>

    /**
     * Запускает процесс экспорта данных пользователя (right to access).
     */
    suspend fun requestDataExport(userId: UserId): AppResult<Unit>

    /**
     * Запускает процесс удаления аккаунта и связанных данных (right to erasure).
     */
    suspend fun requestAccountDeletion(userId: UserId): AppResult<Unit>

    /**
     * Создает гостевого пользователя в локальной БД.
     * Используется при включении гостевого режима.
     */
    suspend fun createGuestUser(userId: UserId): AppResult<Unit>
}
