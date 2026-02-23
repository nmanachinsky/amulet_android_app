package com.example.amulet.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import com.example.amulet.shared.domain.auth.model.AuthState
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserSessionManager {

    override val authState: StateFlow<AuthState> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val state = prefs[KEY_AUTH_STATE]
            val userId = prefs[KEY_USER_ID]
            when (state) {
                STATE_LOGGED_IN -> if (userId != null) AuthState.LoggedIn(UserId(userId)) else AuthState.LoggedOut
                STATE_GUEST -> AuthState.Guest
                STATE_LOGGED_OUT -> AuthState.LoggedOut
                else -> AuthState.Loading
            }
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    override val currentUserId: StateFlow<UserId?> = authState.map { state ->
        when (state) {
            is AuthState.LoggedIn -> state.userId
            else -> null
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Main),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    override val isLoggedIn: StateFlow<Boolean> = authState.map { it is AuthState.LoggedIn }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    override val isGuest: StateFlow<Boolean> = authState.map { it is AuthState.Guest }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    override suspend fun setLoggedIn(userId: UserId) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTH_STATE] = STATE_LOGGED_IN
            prefs[KEY_USER_ID] = userId.value
        }
    }

    override suspend fun setGuest() {
        dataStore.edit { prefs ->
            prefs[KEY_AUTH_STATE] = STATE_GUEST
            prefs.remove(KEY_USER_ID)
        }
    }

    override suspend fun setLoggedOut() {
        dataStore.edit { prefs ->
            prefs[KEY_AUTH_STATE] = STATE_LOGGED_OUT
            prefs.remove(KEY_USER_ID)
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_AUTH_STATE = stringPreferencesKey("auth_state")
        val KEY_USER_ID  = stringPreferencesKey("user_id")
        const val STATE_LOGGED_IN = "logged_in"
        const val STATE_GUEST = "guest"
        const val STATE_LOGGED_OUT = "logged_out"
    }
}
