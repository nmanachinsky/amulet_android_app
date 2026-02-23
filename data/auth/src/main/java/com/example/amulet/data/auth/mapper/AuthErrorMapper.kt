package com.example.amulet.data.auth.mapper

import com.example.amulet.shared.core.AppError
import io.github.jan.supabase.exceptions.RestException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthErrorMapper @Inject constructor() {

    fun map(throwable: Throwable): AppError = when (throwable) {
        is RestException -> mapRestException(throwable)
        else -> AppError.Unknown
    }

    private fun mapRestException(exception: RestException): AppError = when (exception.statusCode) {
        400, 422 -> AppError.Validation(
            exception.message?.let { mapOf("message" to it) } ?: emptyMap()
        )
        401 -> AppError.Unauthorized
        403 -> AppError.Forbidden
        404 -> AppError.NotFound
        409 -> AppError.Conflict
        else -> AppError.Server(exception.statusCode, exception.message)
    }
}
