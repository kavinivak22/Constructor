package com.example.constructorapp.data.remote

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private const val BASE_URL = "https://yrleyquvxogcgbgbrmfl.supabase.co/"
    private const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlybGV5cXV2eG9nY2diZ2JybWZsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk1MzUyODUsImV4cCI6MjA3NTExMTI4NX0.qIEuY_Xe8lmshPzR5bVZq8WmUR92e30m0hGxf1nRYgE"

    // Session state
    var authToken: String? = null
    var userId: String? = null
    var userEmail: String? = null

    val authHeader: String
        get() = authToken?.let { "Bearer $it" } ?: ""

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", API_KEY)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: SupabaseApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SupabaseApi::class.java)
}
