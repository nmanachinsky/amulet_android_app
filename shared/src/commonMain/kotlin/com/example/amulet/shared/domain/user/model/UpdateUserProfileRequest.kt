package com.example.amulet.shared.domain.user.model

import com.example.amulet.shared.domain.privacy.model.UserConsents

/**
 * Запрос на обновление профиля текущего пользователя.
 *
 * Все поля опциональны: передаём только те, которые реально хотим изменить
 * (семантика конкретного backend API определяется на слое data).
 */
data class UpdateUserProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val timezone: String? = null,
    val language: String? = null,
    val consents: UserConsents? = null,
)
