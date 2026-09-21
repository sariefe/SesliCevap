package com.example.di

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.ConversationHistoryDao
import com.example.data.local.dao.MessageDao
import com.example.data.remote.ElevenLabsApiService
import com.example.data.remote.GeminiApiService
import com.example.data.repository.VoiceAssistantRepository
import com.example.data.repository.VoiceAssistantRepositoryImpl
import com.example.data.speech.ElevenLabsTtsManager
import com.example.data.speech.SpeechRecognitionManager
import com.example.data.speech.TextToSpeechManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideConversationHistoryDao(database: AppDatabase): ConversationHistoryDao {
        return database.conversationHistoryDao()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // DEBUG: summary only. Release: no HTTP logs (headers/API keys stay out of Logcat).
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(retrofit: Retrofit): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideElevenLabsApiService(okHttpClient: OkHttpClient, moshi: Moshi): ElevenLabsApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.elevenlabs.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ElevenLabsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVoiceAssistantRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        geminiApiService: GeminiApiService,
        conversationHistoryDao: ConversationHistoryDao
    ): VoiceAssistantRepository {
        return VoiceAssistantRepositoryImpl(
            conversationDao = conversationDao,
            messageDao = messageDao,
            geminiApiService = geminiApiService,
            conversationHistoryDao = conversationHistoryDao
        )
    }

    @Provides
    @Singleton
    fun provideSpeechRecognitionManager(@ApplicationContext context: Context): SpeechRecognitionManager {
        return SpeechRecognitionManager(context)
    }

    @Provides
    @Singleton
    fun provideTextToSpeechManager(@ApplicationContext context: Context): TextToSpeechManager {
        return TextToSpeechManager(context)
    }

    @Provides
    @Singleton
    fun provideElevenLabsTtsManager(
        @ApplicationContext context: Context,
        elevenLabsApiService: ElevenLabsApiService,
        nativeTtsManager: TextToSpeechManager
    ): ElevenLabsTtsManager {
        return ElevenLabsTtsManager(
            context = context,
            elevenLabsApiService = elevenLabsApiService,
            nativeTtsManager = nativeTtsManager
        )
    }
}
