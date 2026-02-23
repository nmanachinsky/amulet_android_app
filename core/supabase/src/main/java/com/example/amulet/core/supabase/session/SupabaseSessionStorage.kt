package com.example.amulet.core.supabase.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Хранилище Supabase-сессий с использованием DataStore.
 * Обеспечивает персистентное сохранение и восстановление сессий авторизации.
 */
@Singleton
@OptIn(ExperimentalTime::class)
class SupabaseSessionStorage @Inject constructor(
    @Named("supabaseSession") private val dataStore: DataStore<Preferences>
) {
    
    suspend fun saveSession(session: UserSession) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = session.accessToken
            session.refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it }
            // Сохраняем expiresAt как epoch millis
            prefs[KEY_EXPIRES_AT] = session.expiresAt.toEpochMilliseconds()
            prefs[KEY_TOKEN_TYPE] = session.tokenType
            session.user?.let { user ->
                prefs[KEY_USER_ID] = user.id
                user.email?.let { prefs[KEY_USER_EMAIL] = it }
            }
        }
    }
    
    suspend fun loadSession(): UserSession? {
        val prefs = dataStore.data.firstOrNull() ?: return null
        val accessToken = prefs[KEY_ACCESS_TOKEN] ?: return null
        val tokenType = prefs[KEY_TOKEN_TYPE] ?: return null
        val expiresAtMillis = prefs[KEY_EXPIRES_AT] ?: return null
        
        val userId = prefs[KEY_USER_ID]
        val userEmail = prefs[KEY_USER_EMAIL] ?: ""
        val userInfo = if (userId != null) {
            UserInfo(
                id = userId,
                aud = "authenticated",
                email = userEmail
            )
        } else null
        
        val expiresAt = Instant.fromEpochMilliseconds(expiresAtMillis)
        val nowMillis = System.currentTimeMillis()
        val expiresIn = (expiresAtMillis - nowMillis) / 1000
        return UserSession(
            accessToken = accessToken,
            refreshToken = prefs[KEY_REFRESH_TOKEN] ?: "",
            expiresAt = expiresAt,
            expiresIn = expiresIn,
            tokenType = tokenType,
            user = userInfo
        )
    }
    
    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
    
    fun observeSession() = dataStore.data.map { prefs ->
        val accessToken = prefs[KEY_ACCESS_TOKEN] ?: return@map null
        val tokenType = prefs[KEY_TOKEN_TYPE] ?: return@map null
        val expiresAtMillis = prefs[KEY_EXPIRES_AT] ?: return@map null
        
        val userId = prefs[KEY_USER_ID]
        val userEmail = prefs[KEY_USER_EMAIL] ?: ""
        val userInfo = if (userId != null) {
            UserInfo(
                id = userId,
                aud = "authenticated",
                email = userEmail
            )
        } else null
        
        val expiresAt = Instant.fromEpochMilliseconds(expiresAtMillis)
        val nowMillis = System.currentTimeMillis()
        val expiresIn = (expiresAtMillis - nowMillis) / 1000
        UserSession(
            accessToken = accessToken,
            refreshToken = prefs[KEY_REFRESH_TOKEN] ?: "",
            expiresAt = expiresAt,
            expiresIn = expiresIn,
            tokenType = tokenType,
            user = userInfo
        )
    }
    
    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("supabase_access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("supabase_refresh_token")
        val KEY_EXPIRES_AT = longPreferencesKey("supabase_expires_at")
        val KEY_TOKEN_TYPE = stringPreferencesKey("supabase_token_type")
        val KEY_USER_ID = stringPreferencesKey("supabase_user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("supabase_user_email")
    }
}
