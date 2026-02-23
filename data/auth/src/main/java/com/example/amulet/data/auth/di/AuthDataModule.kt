package com.example.amulet.data.auth.di

import com.example.amulet.core.auth.UserSessionManager
import com.example.amulet.data.auth.datasource.local.AuthLocalDataSource
import com.example.amulet.data.auth.datasource.local.RoomAuthLocalDataSource
import com.example.amulet.data.auth.datasource.remote.AuthRemoteDataSource
import com.example.amulet.data.auth.datasource.remote.SupabaseAuthDataSource
import com.example.amulet.data.auth.repository.AuthRepositoryImpl
import com.example.amulet.shared.domain.auth.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AuthDataModule {

    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindAuthRemoteDataSource(impl: SupabaseAuthDataSource): AuthRemoteDataSource

    @Binds
    @Singleton
    fun bindAuthLocalDataSource(impl: RoomAuthLocalDataSource): AuthLocalDataSource
}
