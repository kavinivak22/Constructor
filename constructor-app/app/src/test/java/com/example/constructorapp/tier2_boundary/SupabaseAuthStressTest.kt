package com.example.constructorapp.tier2_boundary

import com.example.constructorapp.data.remote.SessionManager
import com.example.constructorapp.data.remote.SupabaseAuthInterceptor
import com.example.constructorapp.data.remote.SupabaseTokenAuthenticator
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SupabaseAuthStressTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        sessionManager = SessionManager()
    }

    @Test
    fun testAuthInterceptor_injectsApikeyAndBearerToken() {
        sessionManager.setAccessToken("test-access-token-123")
        val interceptor = SupabaseAuthInterceptor(sessionManager, "test-api-key-xyz")

        var interceptedRequest: Request? = null
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://example.supabase.co/rest/v1/projects").build()
            override fun proceed(request: Request): Response {
                interceptedRequest = request
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("[]".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            override fun connection(): Connection? = null
            override fun call(): Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 1000
            override fun readTimeoutMillis(): Int = 1000
            override fun writeTimeoutMillis(): Int = 1000
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        interceptor.intercept(chain)
        assertNotNull(interceptedRequest)
        assertEquals("test-api-key-xyz", interceptedRequest!!.header("apikey"))
        assertEquals("Bearer test-access-token-123", interceptedRequest!!.header("Authorization"))
    }

    @Test
    fun testAuthInterceptor_doesNotOverwriteExistingAuthorizationHeader() {
        sessionManager.setAccessToken("default-session-token")
        val interceptor = SupabaseAuthInterceptor(sessionManager, "test-api-key-xyz")

        var interceptedRequest: Request? = null
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder()
                .url("https://example.supabase.co/rest/v1/projects")
                .header("Authorization", "CustomTokenExplicit")
                .build()

            override fun proceed(request: Request): Response {
                interceptedRequest = request
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("[]".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            override fun connection(): Connection? = null
            override fun call(): Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 1000
            override fun readTimeoutMillis(): Int = 1000
            override fun writeTimeoutMillis(): Int = 1000
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        interceptor.intercept(chain)
        assertNotNull(interceptedRequest)
        assertEquals("test-api-key-xyz", interceptedRequest!!.header("apikey"))
        assertEquals("CustomTokenExplicit", interceptedRequest!!.header("Authorization"))
    }

    @Test
    fun testAuthInterceptor_omitsAuthorizationWhenTokenNull() {
        sessionManager.setAccessToken(null)
        val interceptor = SupabaseAuthInterceptor(sessionManager, "test-api-key-xyz")

        var interceptedRequest: Request? = null
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://example.supabase.co/rest/v1/projects").build()
            override fun proceed(request: Request): Response {
                interceptedRequest = request
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("[]".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            override fun connection(): Connection? = null
            override fun call(): Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis(): Int = 1000
            override fun readTimeoutMillis(): Int = 1000
            override fun writeTimeoutMillis(): Int = 1000
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        interceptor.intercept(chain)
        assertNotNull(interceptedRequest)
        assertEquals("test-api-key-xyz", interceptedRequest!!.header("apikey"))
        assertNull(interceptedRequest!!.header("Authorization"))
    }

    @Test
    fun testTokenAuthenticator_successfulRefreshOn401() {
        sessionManager.saveTokens(
            accessToken = "expired-token",
            refreshToken = "valid-refresh-token",
            userId = "user-1",
            email = "user@example.com"
        )

        val refreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                if (req.url.toString().contains("/auth/v1/token?grant_type=refresh_token")) {
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("""{"access_token":"refreshed-token-456","refresh_token":"new-refresh-token-789"}""".toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(404)
                        .message("Not Found")
                        .body("".toResponseBody("application/json".toMediaType()))
                        .build()
                }
            }
            .build()

        val authenticator = SupabaseTokenAuthenticator(
            sessionManager = sessionManager,
            refreshClient = refreshClient,
            baseUrl = "https://example.supabase.co/",
            apiKey = "test-api-key",
            json = json
        )

        val request = Request.Builder()
            .url("https://example.supabase.co/rest/v1/projects")
            .header("Authorization", "Bearer expired-token")
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val retryRequest = authenticator.authenticate(null, response)
        assertNotNull("Authenticator should return retry request", retryRequest)
        assertEquals("Bearer refreshed-token-456", retryRequest!!.header("Authorization"))

        assertEquals("refreshed-token-456", sessionManager.getAccessToken())
        assertEquals("new-refresh-token-789", sessionManager.getRefreshToken())
        assertEquals("user-1", sessionManager.getUserId())
        assertEquals("user@example.com", sessionManager.getUserEmail())
    }

    @Test
    fun testTokenAuthenticator_concurrencyMutexLockSingleRefresh() {
        sessionManager.saveTokens(
            accessToken = "concurrent-expired-token",
            refreshToken = "concurrent-refresh-token",
            userId = "user-concurrent",
            email = "concurrent@example.com"
        )

        val refreshCount = AtomicInteger(0)
        val refreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                refreshCount.incrementAndGet()
                // Simulate network latency during token refresh
                Thread.sleep(50)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"access_token":"mutex-refreshed-token","refresh_token":"mutex-next-refresh"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val authenticator = SupabaseTokenAuthenticator(
            sessionManager = sessionManager,
            refreshClient = refreshClient,
            baseUrl = "https://example.supabase.co/",
            apiKey = "test-api-key",
            json = json
        )

        val threadCount = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val retryResults = mutableListOf<Request?>()
        val exceptions = mutableListOf<Throwable>()

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    startLatch.await()
                    val originalRequest = Request.Builder()
                        .url("https://example.supabase.co/rest/v1/projects?query=$i")
                        .header("Authorization", "Bearer concurrent-expired-token")
                        .build()
                    val response = Response.Builder()
                        .request(originalRequest)
                        .protocol(Protocol.HTTP_1_1)
                        .code(401)
                        .message("Unauthorized")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()

                    val retry = authenticator.authenticate(null, response)
                    synchronized(retryResults) {
                        retryResults.add(retry)
                    }
                } catch (t: Throwable) {
                    synchronized(exceptions) {
                        exceptions.add(t)
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        // Fire all 20 threads simultaneously
        startLatch.countDown()
        assertTrue("All threads should finish within timeout", doneLatch.await(5, TimeUnit.SECONDS))
        executor.shutdown()

        assertTrue("No exceptions thrown during concurrent authentication", exceptions.isEmpty())
        assertEquals(threadCount, retryResults.size)

        // MUTEX VERIFICATION: Exactly 1 refresh network call was executed!
        assertEquals("Mutex lock should ensure exactly 1 network refresh call for 20 concurrent requests", 1, refreshCount.get())

        // All 20 threads must have obtained the refreshed token
        for (retry in retryResults) {
            assertNotNull("Retry request must not be null", retry)
            assertEquals("Bearer mutex-refreshed-token", retry!!.header("Authorization"))
        }

        assertEquals("mutex-refreshed-token", sessionManager.getAccessToken())
        assertEquals("mutex-next-refresh", sessionManager.getRefreshToken())
    }

    @Test
    fun testTokenAuthenticator_missingRefreshTokenReturnsNull() {
        sessionManager.setAccessToken("some-token")
        sessionManager.setRefreshToken(null)

        val refreshClient = OkHttpClient()
        val authenticator = SupabaseTokenAuthenticator(
            sessionManager = sessionManager,
            refreshClient = refreshClient,
            baseUrl = "https://example.supabase.co/",
            apiKey = "test-api-key",
            json = json
        )

        val request = Request.Builder()
            .url("https://example.supabase.co/rest/v1/projects")
            .header("Authorization", "Bearer some-token")
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val retryRequest = authenticator.authenticate(null, response)
        assertNull("Missing refresh token must abort and return null", retryRequest)
    }

    @Test
    fun testTokenAuthenticator_refreshHttpFailureReturnsNull() {
        sessionManager.saveTokens(
            accessToken = "expired-token",
            refreshToken = "invalid-refresh-token",
            userId = "user-1",
            email = "user@example.com"
        )

        val refreshClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(400)
                    .message("Bad Request")
                    .body("""{"error":"invalid_grant","error_description":"Invalid Refresh Token"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val authenticator = SupabaseTokenAuthenticator(
            sessionManager = sessionManager,
            refreshClient = refreshClient,
            baseUrl = "https://example.supabase.co/",
            apiKey = "test-api-key",
            json = json
        )

        val request = Request.Builder()
            .url("https://example.supabase.co/rest/v1/projects")
            .header("Authorization", "Bearer expired-token")
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val retryRequest = authenticator.authenticate(null, response)
        assertNull("HTTP failure on token refresh must abort and return null", retryRequest)
    }

    @Test
    fun testTokenAuthenticator_networkIOExceptionReturnsNullSafely() {
        sessionManager.saveTokens(
            accessToken = "expired-token",
            refreshToken = "valid-refresh-token",
            userId = "user-1",
            email = "user@example.com"
        )

        val refreshClient = OkHttpClient.Builder()
            .addInterceptor {
                throw IOException("Simulated network timeout/disconnect")
            }
            .build()

        val authenticator = SupabaseTokenAuthenticator(
            sessionManager = sessionManager,
            refreshClient = refreshClient,
            baseUrl = "https://example.supabase.co/",
            apiKey = "test-api-key",
            json = json
        )

        val request = Request.Builder()
            .url("https://example.supabase.co/rest/v1/projects")
            .header("Authorization", "Bearer expired-token")
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val retryRequest = authenticator.authenticate(null, response)
        assertNull("Network IOException on token refresh must catch safely and return null", retryRequest)
    }
}
