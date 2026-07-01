package space.pitchstone.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import space.pitchstone.android.data.api.DynamicUrlInterceptor
import space.pitchstone.android.data.api.GatewayApi
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicUrlInterceptor: DynamicUrlInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(16, TimeUnit.MINUTES)
            .writeTimeout(16, TimeUnit.MINUTES)
            .callTimeout(17, TimeUnit.MINUTES)
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideGatewayApi(okHttpClient: OkHttpClient): GatewayApi {
        return Retrofit.Builder()
            .baseUrl("https://kartiks-mac-mini-7.tailafb282.ts.net/") // Placeholder, overridden by DynamicUrlInterceptor
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GatewayApi::class.java)
    }
}
