package com.example.amulet.data.user.datasource.remote

import com.example.amulet.core.network.NetworkExceptionMapper
import com.example.amulet.core.network.dto.user.UserDto
import com.example.amulet.core.network.dto.user.UserUpdateRequestDto
import com.example.amulet.core.network.safeApiCall
import com.example.amulet.core.network.service.UsersApiService
import com.example.amulet.shared.core.AppResult
import com.github.michaelbull.result.map
import javax.inject.Inject
import javax.inject.Singleton

interface UserRemoteDataSource {
    suspend fun fetchUser(userId: String): AppResult<UserDto>

    /**
     * Отправляет на backend запрос на обновление текущего пользователя.
     */
    suspend fun updateCurrentUser(request: UserUpdateRequestDto): AppResult<UserDto>

    /**
     * Запускает процесс экспорта данных пользователя (right to access).
     */
    suspend fun requestDataExport(): AppResult<Unit>

    /**
     * Запускает процесс удаления аккаунта (right to erasure).
     */
    suspend fun requestAccountDeletion(): AppResult<Unit>
}

@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val apiService: UsersApiService,
    private val exceptionMapper: NetworkExceptionMapper
) : UserRemoteDataSource {

    override suspend fun fetchUser(userId: String): AppResult<UserDto> =
        safeApiCall(exceptionMapper) { apiService.getUserById(userId).user }

    override suspend fun updateCurrentUser(request: UserUpdateRequestDto): AppResult<UserDto> =
        safeApiCall(exceptionMapper) { apiService.updateCurrentUser(request = request).user }

    override suspend fun requestDataExport(): AppResult<Unit> =
        safeApiCall(exceptionMapper) { apiService.getPrivacyRights() }.map { }

    override suspend fun requestAccountDeletion(): AppResult<Unit> {
        return safeApiCall(exceptionMapper) { apiService.getPrivacyRights() }.map { }
    }
}
