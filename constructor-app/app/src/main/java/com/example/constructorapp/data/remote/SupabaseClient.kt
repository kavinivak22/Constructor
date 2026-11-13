package com.example.constructorapp.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Supabase client with Bearer token authentication, automatic 401 token refresh,
 * and thread-safe session synchronization.
 */
object SupabaseClient {
    const val BASE_URL = "https://yrleyquvxogcgbgbrmfl.supabase.co/"
    const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlybGV5cXV2eG9nY2diZ2JybWZsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk1MzUyODUsImV4cCI6MjA3NTExMTI4NX0.qIEuY_Xe8lmshPzR5bVZq8WmUR92e30m0hGxf1nRYgE"

    val sessionManager = SessionManager()

    // Backward-compatible session accessors
    var authToken: String?
        get() = sessionManager.getAccessToken()
        set(value) { sessionManager.setAccessToken(value) }

    var refreshToken: String?
        get() = sessionManager.getRefreshToken()
        set(value) { sessionManager.setRefreshToken(value) }

    var userId: String?
        get() = sessionManager.getUserId()
        set(value) { sessionManager.setUserId(value) }

    var userEmail: String?
        get() = sessionManager.getUserEmail()
        set(value) { sessionManager.setUserEmail(value) }

    var companyId: String?
        get() = sessionManager.getCompanyId()
        set(value) { sessionManager.setCompanyId(value) }

    val authHeader: String
        get() = authToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" } ?: "Bearer $API_KEY"

    fun isLoggedIn(): Boolean = sessionManager.isSessionValid()

    fun init(context: Context) {
        sessionManager.init(context)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    // Bare client used exclusively for refresh calls to prevent recursive Authenticator loops
    private val refreshHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(SupabaseAuthInterceptor(sessionManager, API_KEY))
        .authenticator(SupabaseTokenAuthenticator(sessionManager, refreshHttpClient, BASE_URL, API_KEY, json))
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

/**
 * Interceptor that automatically injects `apikey` and `Authorization: Bearer <token>`
 */
class SupabaseAuthInterceptor(
    private val sessionManager: SessionManager,
    private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("apikey", apiKey)

        val token = sessionManager.getAccessToken()
        if (!token.isNullOrBlank() && original.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}

/**
 * Authenticator that catches 401 Unauthorized responses and performs synchronous
 * token refresh via Supabase /auth/v1/token?grant_type=refresh_token
 */
class SupabaseTokenAuthenticator(
    private val sessionManager: SessionManager,
    private val refreshClient: OkHttpClient,
    private val baseUrl: String,
    private val apiKey: String,
    private val json: Json
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val originalToken = sessionManager.getAccessToken()

        if (response.priorResponse != null) {
            // Already retried once, clear invalid session and stop
            sessionManager.clear()
            return null
        }

        synchronized(refreshLock) {
            val latestToken = sessionManager.getAccessToken()
            if (!latestToken.isNullOrBlank() && latestToken != originalToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            val currentRefreshToken = sessionManager.getRefreshToken()
            if (!currentRefreshToken.isNullOrBlank()) {
                val newTokens = performTokenRefresh(currentRefreshToken)
                if (newTokens != null) {
                    sessionManager.saveTokens(
                        accessToken = newTokens.accessToken,
                        refreshToken = newTokens.refreshToken,
                        userId = sessionManager.getUserId(),
                        email = sessionManager.getUserEmail()
                    )

                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                }
            }

            // Refresh failed or no refresh token: session is invalid
            sessionManager.clear()
            return null
        }
    }

    private fun performTokenRefresh(refreshToken: String): RefreshTokenResponse? {
        val url = "${baseUrl.trimEnd('/')}/auth/v1/token?grant_type=refresh_token"
        val jsonPayload = """{"refresh_token":"$refreshToken"}"""
        val mediaType = "application/json".toMediaType()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody(mediaType))
            .build()

        return try {
            val refreshResponse = refreshClient.newCall(request).execute()
            if (refreshResponse.isSuccessful && refreshResponse.body != null) {
                val responseStr = refreshResponse.body!!.string()
                json.decodeFromString<RefreshTokenResponse>(responseStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class RefreshTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

/**
 * Thread-safe session manager with in-memory state and SharedPreferences backing.
 */
class SessionManager {
    private var prefs: SharedPreferences? = null

    @Volatile private var inMemoryAccessToken: String? = null
    @Volatile private var inMemoryRefreshToken: String? = null
    @Volatile private var inMemoryUserId: String? = null
    @Volatile private var inMemoryUserEmail: String? = null
    @Volatile private var inMemoryCompanyId: String? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("constructor_auth_prefs", Context.MODE_PRIVATE)
        inMemoryAccessToken = prefs?.getString("access_token", null)
        inMemoryRefreshToken = prefs?.getString("refresh_token", null)
        inMemoryUserId = prefs?.getString("user_id", null)
        inMemoryUserEmail = prefs?.getString("user_email", null)
        inMemoryCompanyId = prefs?.getString("company_id", null)
    }

    fun getAccessToken(): String? = inMemoryAccessToken
    fun getRefreshToken(): String? = inMemoryRefreshToken
    fun getUserId(): String? = inMemoryUserId
    fun getUserEmail(): String? = inMemoryUserEmail
    fun getCompanyId(): String? = inMemoryCompanyId

    fun isSessionValid(): Boolean {
        val token = inMemoryAccessToken ?: return false
        val hasRefreshToken = !inMemoryRefreshToken.isNullOrBlank()
        if (hasRefreshToken) return true
        return !isTokenExpired(token)
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val payloadBytes = android.util.Base64.decode(
                parts[1],
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            )
            val payload = String(payloadBytes, Charsets.UTF_8)
            val expIndex = payload.indexOf("\"exp\":")
            if (expIndex == -1) return false
            val endExp = payload.indexOfAny(charArrayOf(',', '}'), expIndex + 6)
            val expStr = payload.substring(expIndex + 6, if (endExp != -1) endExp else payload.length).trim()
            val expSeconds = expStr.toLongOrNull() ?: return false
            val nowSeconds = System.currentTimeMillis() / 1000
            nowSeconds >= (expSeconds - 30)
        } catch (_: Exception) {
            false
        }
    }

    fun setAccessToken(token: String?) {
        inMemoryAccessToken = token
        prefs?.edit()?.putString("access_token", token)?.apply()
    }

    fun setRefreshToken(token: String?) {
        inMemoryRefreshToken = token
        prefs?.edit()?.putString("refresh_token", token)?.apply()
    }

    fun setUserId(id: String?) {
        inMemoryUserId = id
        prefs?.edit()?.putString("user_id", id)?.apply()
    }

    fun setUserEmail(email: String?) {
        inMemoryUserEmail = email
        prefs?.edit()?.putString("user_email", email)?.apply()
    }

    fun setCompanyId(companyId: String?) {
        inMemoryCompanyId = companyId
        prefs?.edit()?.putString("company_id", companyId)?.apply()
    }

    fun saveTokens(accessToken: String, refreshToken: String, userId: String?, email: String?, companyId: String? = null) {
        inMemoryAccessToken = accessToken
        inMemoryRefreshToken = refreshToken
        inMemoryUserId = userId
        inMemoryUserEmail = email
        inMemoryCompanyId = companyId

        prefs?.edit()
            ?.putString("access_token", accessToken)
            ?.putString("refresh_token", refreshToken)
            ?.putString("user_id", userId)
            ?.putString("user_email", email)
            ?.putString("company_id", companyId)
            ?.apply()
    }

    fun clear() {
        inMemoryAccessToken = null
        inMemoryRefreshToken = null
        inMemoryUserId = null
        inMemoryUserEmail = null
        inMemoryCompanyId = null
        prefs?.edit()?.clear()?.apply()
    }
}
