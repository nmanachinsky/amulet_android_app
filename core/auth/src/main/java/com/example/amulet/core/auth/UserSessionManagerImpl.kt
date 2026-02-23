package com.example.amulet.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.example.amulet.core.auth.session.AuthPreferencesKeys
import com.example.amulet.core.auth.session.AuthStateMapper
import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val mapper: AuthStateMapper
) : UserSessionManager {

    override val authState: StateFlow<AuthState> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> mapper.map(prefs) }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    override suspend fun setLoggedIn(userId: UserId) {
        dataStore.edit { prefs ->
            prefs[AuthPreferencesKeys.AUTH_STATE] = STATE_LOGGED_IN
            prefs[AuthPreferencesKeys.USER_ID] = userId.value
        }
    }

    override suspend fun setGuest(userId: UserId) {
        dataStore.edit { prefs ->
            prefs[AuthPreferencesKeys.AUTH_STATE] = STATE_GUEST
            prefs[AuthPreferencesKeys.USER_ID] = userId.value
        }
    }

    override suspend fun setLoggedOut() {
        dataStore.edit { prefs ->
            prefs[AuthPreferencesKeys.AUTH_STATE] = STATE_LOGGED_OUT
            prefs.remove(AuthPreferencesKeys.USER_ID)
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    override fun isLoggedIn(): Boolean = authState.value is AuthState.LoggedIn

    override fun isGuest(): Boolean = authState.value is AuthState.Guest

    override fun getCurrentUserId(): UserId? = when (val state = authState.value) {
        is AuthState.LoggedIn -> state.userId
        is AuthState.Guest -> state.userId
        else -> null
    }

    override fun hasActiveSession(): Boolean = authState.value.let {
        it is AuthState.LoggedIn || it is AuthState.Guest
    }

    private companion object {
        const val STATE_LOGGED_IN = "logged_in"
        const val STATE_GUEST = "guest"
        const val STATE_LOGGED_OUT = "logged_out"
    }
}
