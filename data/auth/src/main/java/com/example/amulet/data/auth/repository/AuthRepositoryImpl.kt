package com.example.amulet.data.auth.repository

import com.example.amulet.data.auth.datasource.local.AuthLocalDataSource
import com.example.amulet.data.auth.datasource.remote.AuthRemoteDataSource
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.model.UserCredentials
import com.example.amulet.shared.domain.auth.repository.AuthRepository
import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.flatMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val localDataSource: AuthLocalDataSource,
    private val userSessionUpdater: UserSessionUpdater
) : AuthRepository {

    override suspend fun signUp(credentials: UserCredentials): AppResult<UserId> =
        remoteDataSource.signUp(credentials)

    override suspend fun signIn(credentials: UserCredentials): AppResult<UserId> =
        remoteDataSource.signIn(credentials)

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String?): AppResult<UserId> =
        remoteDataSource.signInWithGoogle(idToken, rawNonce)

    override suspend fun signOut(): AppResult<Unit> =
        remoteDataSource.signOut().flatMap {
            runCatching {
                userSessionUpdater.clearSession()
                localDataSource.clearAll()
            }.fold(
                onSuccess = {
                    Logger.i("Repository signOut: local cleared", TAG)
                    Ok(Unit)
                },
                onFailure = { throwable ->
                    Logger.w("Repository signOut: local clear failed", throwable, TAG)
                    Err(AppError.Unknown)
                }
            )
        }

    override suspend fun establishSession(user: User): AppResult<Unit> = runCatching {
        userSessionUpdater.updateSession(user)
    }.fold(
        onSuccess = {
            Logger.i("Repository establishSession success userId=${user.id.value}", TAG)
            Ok(Unit)
        },
        onFailure = { throwable ->
            Logger.w("Repository establishSession failed", throwable, TAG)
            Err(AppError.Unknown)
        }
    )

    override suspend fun enableGuestSession(displayName: String?, language: String?): AppResult<Unit> = runCatching {
        userSessionUpdater.enableGuestMode(displayName, language)
    }.fold(
        onSuccess = {
            Logger.i("Repository enableGuestSession success", TAG)
            Ok(Unit)
        },
        onFailure = { throwable ->
            Logger.w("Repository enableGuestSession failed", throwable, TAG)
            Err(AppError.Unknown)
        }
    )

    private companion object {
        const val TAG = "AuthRepositoryImpl"
    }
}
