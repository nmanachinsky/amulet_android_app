package com.example.amulet.data.auth.repository

import com.example.amulet.core.auth.UserSessionManager
import com.example.amulet.data.auth.datasource.local.AuthLocalDataSource
import com.example.amulet.data.auth.datasource.remote.AuthRemoteDataSource
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.auth.model.UserCredentials
import com.example.amulet.shared.domain.auth.repository.AuthRepository
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.flatMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val localDataSource: AuthLocalDataSource,
    private val userSessionManager: UserSessionManager
) : AuthRepository {

    override val authState: StateFlow<AuthState> = userSessionManager.authState

    override suspend fun signUp(credentials: UserCredentials): AppResult<UserId> {
        Logger.d("signUp: starting registration", TAG)
        return remoteDataSource.signUp(credentials).also { result ->
            when (result.isOk) {
                true -> {
                    userSessionManager.setLoggedIn(result.value)
                    Logger.i("signUp: success userId=${result.value.value}", TAG)
                }
                false -> Logger.w("signUp: failed error=${result.error}", tag = TAG)
            }
        }
    }

    override suspend fun signIn(credentials: UserCredentials): AppResult<UserId> {
        Logger.d("signIn: starting authentication", TAG)
        return remoteDataSource.signIn(credentials).also { result ->
            when (result.isOk) {
                true -> {
                    userSessionManager.setLoggedIn(result.value)
                    Logger.i("signIn: success userId=${result.value.value}", TAG)
                }
                false -> Logger.w("signIn: failed error=${result.error}", tag = TAG)
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String?): AppResult<UserId> {
        Logger.d("signInWithGoogle: starting Google authentication", TAG)
        return remoteDataSource.signInWithGoogle(idToken, rawNonce).also { result ->
            when (result.isOk) {
                true -> {
                    userSessionManager.setLoggedIn(result.value)
                    Logger.i("signInWithGoogle: success userId=${result.value.value}", TAG)
                }
                false -> Logger.w("signInWithGoogle: failed error=${result.error}", tag = TAG)
            }
        }
    }

    override suspend fun signOut(): AppResult<Unit> {
        Logger.d("signOut: starting logout", TAG)
        val currentUserId = userSessionManager.getCurrentUserId()
        return remoteDataSource.signOut().flatMap {
            runCatching {
                userSessionManager.setLoggedOut()
                currentUserId?.let { localDataSource.deleteByUserId(it.value) }
            }.fold(
                onSuccess = {
                    Logger.i("signOut: success", TAG)
                    Ok(Unit)
                },
                onFailure = { throwable ->
                    Logger.e("signOut: local clear failed", throwable, TAG)
                    Err(AppError.DatabaseError)
                }
            )
        }
    }

    override suspend fun restoreSession(userId: UserId): AppResult<Unit> {
        Logger.d("restoreSession: userId=${userId.value}", TAG)
        return runCatching {
            userSessionManager.setLoggedIn(userId)
        }.fold(
            onSuccess = {
                Logger.i("restoreSession: success userId=${userId.value}", TAG)
                Ok(Unit)
            },
            onFailure = { throwable ->
                Logger.e("restoreSession: failed", throwable, TAG)
                Err(AppError.DatabaseError)
            }
        )
    }

    override suspend fun startGuestSession(): AppResult<Unit> {
        Logger.d("startGuestSession", TAG)
        return runCatching {
            val guestId = UserId(UUID.randomUUID().toString())
            userSessionManager.setGuest(guestId)
        }.fold(
            onSuccess = {
                Logger.i("startGuestSession: success", TAG)
                Ok(Unit)
            },
            onFailure = { throwable ->
                Logger.e("startGuestSession: failed", throwable, TAG)
                Err(AppError.DatabaseError)
            }
        )
    }

    override suspend fun clearSession(): AppResult<Unit> {
        Logger.d("clearSession", TAG)
        return runCatching {
            userSessionManager.clear()
            // localDataSource.clearAll() // TODO: раскомментировать если нужно удалять данные
        }.fold(
            onSuccess = {
                Logger.i("clearSession: success", TAG)
                Ok(Unit)
            },
            onFailure = { throwable ->
                Logger.e("clearSession: failed", throwable, TAG)
                Err(AppError.DatabaseError)
            }
        )
    }

    override fun isLoggedIn(): Boolean = userSessionManager.isLoggedIn()

    override fun isGuest(): Boolean = userSessionManager.isGuest()

    override fun getCurrentUserId(): UserId? = userSessionManager.getCurrentUserId()

    override fun hasActiveSession(): Boolean = userSessionManager.hasActiveSession()

    private companion object {
        const val TAG = "AuthRepositoryImpl"
    }
}
