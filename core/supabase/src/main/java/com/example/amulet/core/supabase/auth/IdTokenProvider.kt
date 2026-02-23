package com.example.amulet.core.supabase.auth

/**
 * Провайдер токена авторизации для HTTP-запросов.
 * Реализация должна возвращать полный Authorization заголовок (например, "Bearer xxx").
 */
fun interface IdTokenProvider {
    suspend fun getIdToken(): String?
}