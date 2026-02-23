package com.example.amulet.data.privacy

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.privacy.PrivacyRepository
import com.example.amulet.shared.domain.privacy.model.UserConsents
import com.example.amulet.shared.domain.user.model.User
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class PrivacyRepositoryImpl @Inject constructor(
    private val sessionProvider: UserSessionProvider,
    private val sessionUpdater: UserSessionUpdater,
    // TODO: внедрить PrivacyApiService и/или UserRepository, когда будет полноценная реализация
) : PrivacyRepository {

    // Временный in-memory стрим для прототипа. В реальной реализации должен браться из user/privay data layer.

    override fun getUserConsentsStream(): Flow<UserConsents> =
        sessionProvider.sessionContext
            .map { context ->
                when (context) {
                    is UserSessionContext.LoggedIn -> context.consents
                    else -> UserConsents()
                }
            }
            .distinctUntilChanged()

    override suspend fun updateUserConsents(consents: UserConsents): AppResult<Unit> {
        // TODO: вызвать backend API и синхронизировать с user-профилем

        val context = sessionProvider.currentContext
        val loggedIn = context as? UserSessionContext.LoggedIn
            ?: return Err(AppError.Unauthorized)

        val user = User(
            id = loggedIn.userId,
            displayName = loggedIn.displayName,
            avatarUrl = loggedIn.avatarUrl,
            timezone = loggedIn.timezone,
            language = loggedIn.language,
            consents = consents,
        )

        return runCatching {
            sessionUpdater.updateSession(user)
        }.fold(
            onSuccess = { Ok(Unit) },
            onFailure = { Err(AppError.Unknown) }
        )
    }

    override suspend fun requestDataExport(): AppResult<Unit> {
        // TODO: интеграция с PrivacyApiService.getPrivacyRights() или спец. эндпоинтом экспорта
        return Ok(Unit)
    }

    override suspend fun requestAccountDeletion(): AppResult<Unit> {
        // TODO: интеграция с API удаления/деактивации аккаунта
        return Ok(Unit)
    }
}
