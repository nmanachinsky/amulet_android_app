package com.example.amulet.data.user.di

import com.example.amulet.data.user.UserRepositoryImpl
import com.example.amulet.data.user.datasource.local.UserLocalDataSource
import com.example.amulet.data.user.datasource.local.UserLocalDataSourceImpl
import com.example.amulet.data.user.datasource.remote.UserRemoteDataSource
import com.example.amulet.data.user.datasource.remote.UserRemoteDataSourceImpl
import com.example.amulet.data.user.mapper.UserMapper
import com.example.amulet.shared.domain.user.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface UserDataModule {

    @Binds
    @Singleton
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    fun bindUserRemoteDataSource(impl: UserRemoteDataSourceImpl): UserRemoteDataSource

    @Binds
    @Singleton
    fun bindUserLocalDataSource(impl: UserLocalDataSourceImpl): UserLocalDataSource

    companion object {
        @Provides
        fun provideUserMapper(): UserMapper = UserMapper
    }
}
