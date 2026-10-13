package id.lapor.lh.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.lapor.lh.BuildConfig
import id.lapor.lh.core.network.AuthApi
import id.lapor.lh.core.network.ReportApi
import id.lapor.lh.feature.auth.data.SessionManager
import id.lapor.lh.feature.auth.data.TokenAuthenticator
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionManager: SessionManager,
        authenticator: TokenAuthenticator,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val token = sessionManager.accessToken
                    val request =
                        if (token != null) {
                            chain
                                .request()
                                .newBuilder()
                                .header("Authorization", "Bearer $token")
                                .build()
                        } else {
                            chain.request()
                        }
                    chain.proceed(request)
                }.authenticator(authenticator)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideReportApi(retrofit: Retrofit): ReportApi = retrofit.create(ReportApi::class.java)
}
