package com.example.amulet.core.network.service

import com.example.amulet.core.network.dto.user.PrivacyRightsResponseDto
import com.example.amulet.core.network.dto.user.UserInitRequestDto
import com.example.amulet.core.network.dto.user.UserResponseDto
import com.example.amulet.core.network.dto.user.UserUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface UsersApiService {

    @POST("users/{route}")
    suspend fun initUser(
        @Path("route") route: String = "users.me.init",
        @Body request: UserInitRequestDto = UserInitRequestDto()
    ): UserResponseDto

    @GET("users/{route}")
    suspend fun getCurrentUser(
        @Path("route") route: String = "users.me"
    ): UserResponseDto

    @GET("users/by-id/{userId}")
    suspend fun getUserById(
        @Path("userId") userId: String
    ): UserResponseDto

    @PATCH("users/{route}")
    suspend fun updateCurrentUser(
        @Path("route") route: String = "users.me",
        @Body request: UserUpdateRequestDto
    ): UserResponseDto

    @GET("privacy/rights")
    suspend fun getPrivacyRights(): PrivacyRightsResponseDto

    @POST("privacy/export")
    suspend fun requestDataExport(): PrivacyRightsResponseDto

    @POST("privacy/delete")
    suspend fun requestAccountDeletion(): PrivacyRightsResponseDto
}
