package com.example.amulet.core.network

import com.example.amulet.core.supabase.auth.IdTokenProvider
import com.example.amulet.core.network.di.ApiBaseUrl
import com.example.amulet.core.network.interceptor.AuthInterceptor
import com.example.amulet.core.network.interceptor.CaptchaInterceptor
import com.example.amulet.core.network.serialization.JsonProvider
import com.example.amulet.core.network.service.AdminApiService
import com.example.amulet.core.network.service.HugsApiService
import com.example.amulet.core.network.service.NotificationsApiService
import com.example.amulet.core.network.service.OtaApiService
import com.example.amulet.core.network.service.PairsApiService
import com.example.amulet.core.network.service.PatternsApiService
import com.example.amulet.core.network.service.PracticesApiService
import com.example.amulet.core.network.service.PrivacyApiService
import com.example.amulet.core.network.service.RulesApiService
import com.example.amulet.core.network.service.TelemetryApiService
import com.example.amulet.core.network.service.UsersApiService
import com.example.amulet.core.supabase.SupabaseEnvironment
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.example.amulet.core.turnstile.TurnstileTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val JSON_MEDIA_TYPE = "application/json"

    @Provides
    @Singleton
    @ApiBaseUrl
    fun provideApiBaseUrl(environment: SupabaseEnvironment): String = environment.restUrl.trimEnd('/') + "/"

    @Provides
    @Singleton
    fun provideJson(): Json = JsonProvider.create()

    @Provides
    @Singleton
    fun provideNetworkExceptionMapper(json: Json): NetworkExceptionMapper =
        NetworkExceptionMapper(json)
    

    @Provides
    fun provideAuthInterceptor(idTokenProvider: IdTokenProvider): AuthInterceptor =
        AuthInterceptor(idTokenProvider)

    @Provides
    fun provideCaptchaInterceptor(tokenStore: TurnstileTokenStore): CaptchaInterceptor =
        CaptchaInterceptor(tokenStore)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        environment: SupabaseEnvironment
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(15_000, TimeUnit.MILLISECONDS)
            .readTimeout(30_000, TimeUnit.MILLISECONDS)
            .writeTimeout(30_000, TimeUnit.MILLISECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("apikey", environment.anonKey)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(authInterceptor)
            .apply { if (loggingInterceptor.level != HttpLoggingInterceptor.Level.NONE) addInterceptor(loggingInterceptor) }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json,
        okHttpClient: OkHttpClient,
        @ApiBaseUrl baseUrl: String
    ): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    @Provides
    fun provideUsersApiService(retrofit: Retrofit): UsersApiService = retrofit.create(UsersApiService::class.java)

    @Provides
    fun provideHugsApiService(retrofit: Retrofit): HugsApiService = retrofit.create(HugsApiService::class.java)

    @Provides
    fun providePairsApiService(retrofit: Retrofit): PairsApiService = retrofit.create(PairsApiService::class.java)

    @Provides
    fun providePatternsApiService(retrofit: Retrofit): PatternsApiService = retrofit.create(PatternsApiService::class.java)

    @Provides
    fun providePracticesApiService(retrofit: Retrofit): PracticesApiService = retrofit.create(PracticesApiService::class.java)

    @Provides
    fun providePrivacyApiService(retrofit: Retrofit): PrivacyApiService = retrofit.create(PrivacyApiService::class.java)

    @Provides
    fun provideRulesApiService(retrofit: Retrofit): RulesApiService = retrofit.create(RulesApiService::class.java)

    @Provides
    fun provideTelemetryApiService(retrofit: Retrofit): TelemetryApiService = retrofit.create(TelemetryApiService::class.java)

    @Provides
    fun provideAdminApiService(retrofit: Retrofit): AdminApiService = retrofit.create(AdminApiService::class.java)

    @Provides
    fun provideOtaApiService(retrofit: Retrofit): OtaApiService = retrofit.create(OtaApiService::class.java)

    @Provides
    fun provideNotificationsApiService(retrofit: Retrofit): NotificationsApiService = retrofit.create(NotificationsApiService::class.java)

}
