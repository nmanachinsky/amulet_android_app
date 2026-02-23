package com.example.amulet.data.user

import com.example.amulet.core.network.dto.user.UserUpdateRequestDto
import com.example.amulet.data.user.datasource.local.UserLocalDataSource
import com.example.amulet.data.user.datasource.remote.UserRemoteDataSource
import com.example.amulet.data.user.mapper.UserMapper
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.user.model.UpdateUserProfileRequest
import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.user.repository.UserRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource,
    private val mapper: UserMapper
) : UserRepository {

    override suspend fun fetchProfile(userId: UserId): AppResult<User> {
        Logger.d("fetchProfile: userId=${userId.value}", TAG)
        return remoteDataSource.fetchUser(userId.value).fold(
            success = { dto ->
                val existingResult = localDataSource.findById(dto.id)
                val existing = existingResult.value
                val mergedAvatarUrl = mapper.mergeAvatarUrl(dto, existing)

                val user = mapper.toDomainFromDto(dto).copy(avatarUrl = mergedAvatarUrl)
                val entity = mapper.toEntity(dto).copy(avatarUrl = mergedAvatarUrl)

                localDataSource.upsert(entity)
                Logger.i("fetchProfile: success userId=${user.id.value}", TAG)
                Ok(user)
            },
            failure = { error ->
                val cachedResult = localDataSource.findById(userId.value)
                cachedResult.value?.let { cached ->
                    Logger.w("fetchProfile: fallback to cache userId=${userId.value}", tag = TAG)
                    Ok(mapper.toDomain(cached))
                } ?: run {
                    Logger.e("fetchProfile: failed error=$error", tag = TAG)
                    Err(error)
                }
            }
        )
    }

    override fun observeUser(userId: UserId): Flow<User?> {
        return localDataSource.observeById(userId.value)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }

    override suspend fun updateProfile(request: UpdateUserProfileRequest): AppResult<User> {
        Logger.d("updateProfile: starting", TAG)
        val dtoRequest = UserUpdateRequestDto(
            displayName = request.displayName,
            avatarUrl = request.avatarUrl,
            timezone = request.timezone,
            language = request.language,
            consents = null,
        )

        return remoteDataSource.updateCurrentUser(dtoRequest).fold(
            success = { dto ->
                val existingResult = localDataSource.findById(dto.id)
                val existing = existingResult.value
                val mergedAvatarUrl = dto.avatarUrl ?: request.avatarUrl ?: existing?.avatarUrl

                val user = mapper.toDomainFromDto(dto).copy(avatarUrl = mergedAvatarUrl)
                val entity = mapper.toEntity(dto).copy(avatarUrl = mergedAvatarUrl)

                localDataSource.upsert(entity)
                Logger.i("updateProfile: success userId=${user.id.value}", TAG)
                Ok(user)
            },
            failure = { error ->
                Logger.e("updateProfile: failed error=$error", tag = TAG)
                Err(error)
            }
        )
    }

    private companion object {
        const val TAG = "UserRepositoryImpl"
    }
}
