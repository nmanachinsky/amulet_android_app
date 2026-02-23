package com.example.amulet.core.supabase.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.amulet.core.supabase.auth.IdTokenProvider
import com.example.amulet.core.supabase.SupabaseEnvironment
import com.example.amulet.core.supabase.auth.SupabaseAuthManager
import com.example.amulet.core.supabase.auth.SupabaseIdTokenProvider
import com.example.amulet.core.supabase.session.SupabaseAuthSessionManager
import com.example.amulet.core.supabase.session.SupabaseSessionStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.storage.storage
import javax.inject.Singleton

private val Context.supabaseSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "supabase_session"
)

@Module
@InstallIn(SingletonComponent::class)
abstract class SupabaseModule {

    @Binds
    abstract fun bindIdTokenProvider(impl: SupabaseIdTokenProvider): IdTokenProvider

    companion object {

        @Provides
        @Singleton
        fun provideSupabaseSessionDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> = context.supabaseSessionDataStore

        @Provides
        @Singleton
        fun provideSupabaseClient(
            environment: SupabaseEnvironment,
            sessionManager: SupabaseAuthSessionManager
        ): SupabaseClient =
            createSupabaseClient(environment.supabaseUrl, environment.anonKey) {
                install(Auth) {
                    autoLoadFromStorage = true
                    autoSaveToStorage = true
                    alwaysAutoRefresh = true
                    this.sessionManager = sessionManager
                    enableLifecycleCallbacks = false
                }
            }

        @Provides
        fun provideGoTrue(client: SupabaseClient) = client.auth

        @Provides
        fun provideFunctions(client: SupabaseClient) = client.functions

        @Provides
        fun provideStorage(client: SupabaseClient) = client.storage
    }
}
