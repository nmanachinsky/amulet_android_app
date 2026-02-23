package com.example.amulet.data.auth.datasource.remote

import com.example.amulet.data.auth.mapper.AuthErrorMapper
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.model.UserCredentials
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class SupabaseAuthDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val errorMapper: AuthErrorMapper
) : AuthRemoteDataSource {

    private val auth get() = supabaseClient.auth

    override suspend fun signUp(credentials: UserCredentials): AppResult<UserId> =
        executeAuth("signUp") {
            auth.signUpWith(Email) {
                this.email = credentials.email
                this.password = credentials.password
            }
            auth.currentSessionOrNull()
        }

    override suspend fun signIn(credentials: UserCredentials): AppResult<UserId> =
        executeAuth("signIn") {
            auth.signInWith(Email) {
                this.email = credentials.email
                this.password = credentials.password
            }
            auth.currentSessionOrNull()
        }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String?): AppResult<UserId> =
        executeAuth("signInWithGoogle") {
            auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
                this.nonce = rawNonce
            }
            auth.currentSessionOrNull()
        }

    override suspend fun signOut(): AppResult<Unit> = runCatching {
        Logger.d("signOut: starting", TAG)
        withContext(Dispatchers.IO) {
            auth.signOut()
        }
    }.fold(
        onSuccess = {
            Logger.i("signOut: success", TAG)
            Ok(Unit)
        },
        onFailure = { throwable ->
            Logger.w("signOut: failed", throwable, TAG)
            Err(errorMapper.map(throwable))
        }
    )

    private suspend fun executeAuth(
        action: String,
        block: suspend () -> UserSession?
    ): AppResult<UserId> = runCatching {
        Logger.d("$action: starting", TAG)
        withContext(Dispatchers.IO) { block() }
    }.fold(
        onSuccess = { session ->
            if (session == null) {
                Logger.w("$action: session not available (email confirmation required?)", tag = TAG)
                Err(com.example.amulet.shared.core.AppError.Unauthorized)
            } else {
                val userId = session.user?.id ?: error("Missing Supabase user")
                Logger.i("$action: success userId=$userId", TAG)
                Ok(UserId(userId))
            }
        },
        onFailure = { throwable ->
            Logger.w("$action: failed", throwable, TAG)
            Err(errorMapper.map(throwable))
        }
    )

    private companion object {
        private const val TAG = "SupabaseAuthDataSource"
    }
}
