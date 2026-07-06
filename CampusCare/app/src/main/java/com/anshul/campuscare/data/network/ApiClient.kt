package com.anshul.campuscare.data.network

// ──────────────────────────────────────────────
// API Client
//
// Creates a single Retrofit instance that is shared
// across the entire app. This is the only place where
// the base URL and HTTP client are configured.
//
// The cookie jar stores session cookies (connect.sid)
// in SharedPreferences so the user stays logged in
// even after closing and reopening the app.
// ──────────────────────────────────────────────

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton that provides the Retrofit instance and API service.
 *
 * IMPORTANT: You must call ApiClient.initialize(context) once
 * in your MainActivity before using apiService. This loads
 * saved cookies from SharedPreferences.
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
    const val BASE_URL: String = "http://192.168.1.36:3000/"

    // ── Cookie Storage ────────────────────────
    // Stores cookies in SharedPreferences so the
    // session persists across app restarts.
    val cookieJar: PersistentCookieJar = PersistentCookieJar()

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

    /**
     * Call this once in MainActivity.onCreate() before
     * any API calls are made. This loads saved cookies
     * from SharedPreferences into the cookie jar.
     */
    fun initialize(context: Context) {
        cookieJar.initialize(context = context)
    }
}

// ──────────────────────────────────────────────
// Persistent Cookie Jar
//
// Stores cookies in SharedPreferences so the user
// stays logged in even after killing the app.
//
// The key cookie is "connect.sid" — the Express
// session ID. When saved, the user won't need to
// log in again until the session expires on the
// server (24 hours).
// ──────────────────────────────────────────────
class PersistentCookieJar : CookieJar {

    private val PREFS_NAME: String = "campuscare_cookies"
    private val KEY_COOKIES: String = "saved_cookies"

    // In-memory cookie store (loaded from SharedPreferences on init)
    private val cookieStore: HashMap<String, MutableList<Cookie>> = HashMap()

    // Reference to SharedPreferences (set in initialize())
    private var sharedPreferences: SharedPreferences? = null

    /**
     * Load saved cookies from SharedPreferences.
     * Call this once during app startup.
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadCookiesFromPrefs()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies.toMutableList()
        saveCookiesToPrefs()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies: MutableList<Cookie>? = cookieStore[url.host]
        if (cookies == null) {
            return emptyList()
        }
        // Filter out expired cookies
        val currentTimeMillis: Long = System.currentTimeMillis()
        val validCookies: List<Cookie> = cookies.filter { cookie: Cookie ->
            cookie.expiresAt > currentTimeMillis
        }
        return validCookies
    }

    /**
     * Manually add a cookie (used when extracting cookies from WebView).
     * Saves to SharedPreferences immediately.
     */
    fun addCookie(url: HttpUrl, cookie: Cookie) {
        val existingCookies: MutableList<Cookie> = cookieStore.getOrPut(url.host) {
            mutableListOf()
        }
        // Remove any existing cookie with the same name
        existingCookies.removeAll { existingCookie: Cookie ->
            existingCookie.name == cookie.name
        }
        existingCookies.add(cookie)
        saveCookiesToPrefs()
    }

    /**
     * Check if there are any saved cookies.
     * Used to quickly decide if the user might be logged in
     * without making a network call.
     */
    fun hasSavedCookies(): Boolean {
        return cookieStore.isNotEmpty() && cookieStore.values.any { cookies: MutableList<Cookie> ->
            cookies.isNotEmpty()
        }
    }

    /**
     * Clear all stored cookies (used during logout).
     * Also clears SharedPreferences.
     */
    fun clearAllCookies() {
        cookieStore.clear()
        val editor: SharedPreferences.Editor? = sharedPreferences?.edit()
        if (editor != null) {
            editor.remove(KEY_COOKIES)
            editor.apply()
        }
    }

    // ── SharedPreferences Persistence ─────────

    /**
     * Save all cookies to SharedPreferences as a single string.
     * Format: "host||name||value||domain||path||expiresAt||secure||httpOnly;;next..."
     */
    private fun saveCookiesToPrefs() {
        val allCookieStrings: MutableList<String> = mutableListOf()

        for (entry in cookieStore.entries) {
            val host: String = entry.key
            val cookies: List<Cookie> = entry.value

            for (cookie: Cookie in cookies) {
                val cookieString: String = listOf(
                    host,
                    cookie.name,
                    cookie.value,
                    cookie.domain,
                    cookie.path,
                    cookie.expiresAt.toString(),
                    cookie.secure.toString(),
                    cookie.httpOnly.toString()
                ).joinToString(separator = "||")

                allCookieStrings.add(cookieString)
            }
        }

        val combinedString: String = allCookieStrings.joinToString(separator = ";;")

        val editor: SharedPreferences.Editor? = sharedPreferences?.edit()
        if (editor != null) {
            editor.putString(KEY_COOKIES, combinedString)
            editor.apply()
        }
    }

    /**
     * Load cookies from SharedPreferences and restore them
     * to the in-memory cookie store.
     */
    private fun loadCookiesFromPrefs() {
        val combinedString: String = sharedPreferences?.getString(KEY_COOKIES, "") ?: ""

        if (combinedString.isEmpty()) {
            return
        }

        val cookieStrings: List<String> = combinedString.split(";;")

        for (cookieString: String in cookieStrings) {
            val parts: List<String> = cookieString.split("||")

            // We need exactly 8 parts: host, name, value, domain, path, expiresAt, secure, httpOnly
            if (parts.size != 8) {
                continue
            }

            val host: String = parts[0]
            val name: String = parts[1]
            val value: String = parts[2]
            val domain: String = parts[3]
            val path: String = parts[4]
            val expiresAt: Long = parts[5].toLongOrNull() ?: 0L
            val secure: Boolean = parts[6].toBoolean()
            val httpOnly: Boolean = parts[7].toBoolean()

            // Skip expired cookies
            if (expiresAt > 0 && expiresAt < System.currentTimeMillis()) {
                continue
            }

            // Rebuild the Cookie object
            val cookieBuilder: Cookie.Builder = Cookie.Builder()
                .name(name)
                .value(value)
                .domain(domain)
                .path(path)
                .expiresAt(expiresAt)

            if (secure) {
                cookieBuilder.secure()
            }
            if (httpOnly) {
                cookieBuilder.httpOnly()
            }

            val cookie: Cookie = cookieBuilder.build()

            val existingCookies: MutableList<Cookie> = cookieStore.getOrPut(host) {
                mutableListOf()
            }
            existingCookies.add(cookie)
        }
    }
}
