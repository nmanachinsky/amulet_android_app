package com.example.amulet.core.network.interceptor

import com.example.amulet.core.supabase.auth.IdTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * HTTP interceptor для автоматического добавления Authorization заголовка.
 * IdTokenProvider должен возвращать полный заголовок (например, "Bearer xxx").
 */
class AuthInterceptor(
    private val idTokenProvider: IdTokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val authHeader = runBlocking { idTokenProvider.getIdToken() }
        if (authHeader.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }
        val authenticatedRequest = originalRequest.newBuilder()
            .header(AUTHORIZATION_HEADER, authHeader)
            .build()
        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
