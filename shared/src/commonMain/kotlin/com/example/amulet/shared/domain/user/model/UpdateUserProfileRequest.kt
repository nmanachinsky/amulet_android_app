package com.example.amulet.shared.domain.user.model

import com.example.amulet.shared.domain.privacy.model.UserConsents

/**
 * Запрос на обновление профиля текущего пользователя на backend.
 *
 * Все поля опциональны: передаём только те, которые реально хотим изменить
 * (семантика конкретного backend API определяется на слое data).
 *
 * Часовой пояс и язык сюда намеренно не входят: это локальные настройки,
 * которые хранятся только на устройстве и не синхронизируются с сервером
 * (см. [UserRepository.updateLocalPreferences]).
 */
data class UpdateUserProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val consents: UserConsents? = null,
)
