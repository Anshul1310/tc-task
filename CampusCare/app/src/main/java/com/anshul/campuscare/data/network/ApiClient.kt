package com.anshul.campuscare.data.network

// ──────────────────────────────────────────────
// API Client
//
// Creates a single Retrofit instance that is shared
// across the entire app. This is the only place where
// the base URL and HTTP client are configured.
//
// The cookie jar stores session cookies (connect.sid)
// so the user stays logged in across API calls.
// ──────────────────────────────────────────────

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton that provides the Retrofit instance and API service.
 *
 * CHANGE THIS BASE_URL when deploying to a real server
 * or testing on a physical device.
 *
 * - Emulator: "http://10.0.2.2:3000" (maps to host machine's localhost)
 * - Physical device: "http://<your-computer-ip>:3000"
 * - Production: "https://your-server.com"
 */
object ApiClient {

    // ── Base URL ──────────────────────────────
    const val BASE_URL: String = "http://10.0.2.2:3000/"

    // ── Cookie Storage ────────────────────────
    // Stores cookies in memory so the session cookie
    // persists across multiple API calls.
    val cookieJar: SimpleCookieJar = SimpleCookieJar()

    // ── Logging ───────────────────────────────
    // Prints all HTTP requests and responses to Logcat.
    // Set level to NONE for production.
    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ── OkHttp Client ─────────────────────────
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Retrofit Instance ─────────────────────
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ── API Service ───────────────────────────
    // Use this to make API calls throughout the app.
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

// ──────────────────────────────────────────────
// Simple Cookie Jar
//
// A basic in-memory cookie jar that stores all
// cookies the server sends. The key cookie is
// "connect.sid" which is the Express session ID.
// ──────────────────────────────────────────────
class SimpleCookieJar : CookieJar {

    // Store cookies grouped by the host that set them
    private val cookieStore: HashMap<String, MutableList<Cookie>> = HashMap()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies.toMutableList()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies: MutableList<Cookie>? = cookieStore[url.host]
        return cookies ?: emptyList()
    }

    /**
     * Manually add a cookie (used when extracting cookies from WebView).
     */
    fun addCookie(url: HttpUrl, cookie: Cookie) {
        val existingCookies: MutableList<Cookie> = cookieStore.getOrPut(url.host) {
            mutableListOf()
        }
        // Remove any existing cookie with the same name
        existingCookies.removeAll { existingCookie ->
            existingCookie.name == cookie.name
        }
        existingCookies.add(cookie)
    }

    /**
     * Clear all stored cookies (used during logout).
     */
    fun clearAllCookies() {
        cookieStore.clear()
    }
}
