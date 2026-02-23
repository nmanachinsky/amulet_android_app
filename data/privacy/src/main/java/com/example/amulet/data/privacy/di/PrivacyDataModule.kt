package com.example.amulet.data.privacy.di

import com.example.amulet.data.privacy.NotificationsRepositoryImpl
import com.example.amulet.shared.domain.notifications.NotificationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PrivacyDataModule {

    @Binds
    @Singleton
    fun bindNotificationsRepository(impl: NotificationsRepositoryImpl): NotificationsRepository
}
