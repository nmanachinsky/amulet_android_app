package com.example.amulet.data.user.mapper

import com.example.amulet.core.database.entity.UserEntity
import com.example.amulet.core.network.dto.user.UserDto
import com.example.amulet.shared.domain.privacy.model.UserConsents
import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.model.UserId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Singleton
@OptIn(ExperimentalTime::class)
class UserMapper @Inject constructor() {

    fun toDomainFromDto(dto: UserDto): User = User(
        id = UserId(dto.id),
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl,
        email = null,
        timezone = dto.timezone,
        language = dto.language,
        consents = mapConsents(dto.consents),
        createdAt = dto.createdAt?.value?.let { Instant.fromEpochMilliseconds(it) },
        updatedAt = dto.updatedAt?.value?.let { Instant.fromEpochMilliseconds(it) }
    )

    fun toEntity(dto: UserDto): UserEntity = UserEntity(
        id = dto.id,
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl,
        timezone = dto.timezone,
        language = dto.language,
        consents = mapConsents(dto.consents),
        createdAt = dto.createdAt?.value?.let { Instant.fromEpochMilliseconds(it) },
        updatedAt = dto.updatedAt?.value?.let { Instant.fromEpochMilliseconds(it) }
    )

    fun toDomain(entity: UserEntity): User = User(
        id = UserId(entity.id),
        displayName = entity.displayName,
        avatarUrl = entity.avatarUrl,
        timezone = entity.timezone,
        language = entity.language,
        consents = entity.consents,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun mergeAvatarUrl(dto: UserDto, existingEntity: UserEntity?): String? {
        val dtoAvatar = dto.avatarUrl
        return dtoAvatar ?: existingEntity?.avatarUrl
    }

    private fun mapConsents(consents: Map<String, Any?>?): UserConsents? = consents?.let {
        val analytics = (it["analytics"] as? Boolean) ?: false
        val marketing = (it["marketing"] as? Boolean) ?: false
        val notifications = (it["notifications"] as? Boolean) ?: false
        val updatedAtString = it["updatedAt"] as? String
        val updatedAt = updatedAtString?.let { instant -> Instant.parse(instant) }

        UserConsents(
            analytics = analytics,
            marketing = marketing,
            notifications = notifications,
            updatedAt = updatedAt
        )
    }
}
