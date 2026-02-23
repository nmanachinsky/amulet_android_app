package com.example.amulet.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.example.amulet.core.auth.session.AuthStateMapper
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindUserSessionManager(impl: UserSessionManagerImpl): UserSessionManager

    companion object {

        @Provides
        @Singleton
        @Named("userSession")
        fun provideUserSessionDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> = context.userSessionDataStore

        @Provides
        fun provideAuthStateMapper(): AuthStateMapper = AuthStateMapper()
    }
}

private val Context.userSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_session"
)
