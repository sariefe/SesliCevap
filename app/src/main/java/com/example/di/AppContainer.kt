package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.remote.GeminiApiService
import com.example.data.repository.VoiceAssistantRepository
import com.example.data.repository.VoiceAssistantRepositoryImpl
import com.example.data.speech.SpeechRecognitionManager
import com.example.data.speech.TextToSpeechManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val database: AppDatabase
    val geminiApiService: GeminiApiService
    val voiceRepository: VoiceAssistantRepository
    val speechRecognitionManager: SpeechRecognitionManager
    val textToSpeechManager: TextToSpeechManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    override val geminiApiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    override val voiceRepository: VoiceAssistantRepository by lazy {
        VoiceAssistantRepositoryImpl(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            geminiApiService = geminiApiService,
            conversationHistoryDao = database.conversationHistoryDao()
        )
    }

    override val speechRecognitionManager: SpeechRecognitionManager by lazy {
        SpeechRecognitionManager(context)
    }

    override val textToSpeechManager: TextToSpeechManager by lazy {
        TextToSpeechManager(context)
    }
}
