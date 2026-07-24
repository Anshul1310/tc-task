package com.anshul.campuscare.data.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Simple API Client
 *
 * Saves session token/cookie directly in SharedPreferences
 * without complex custom cookie jars or serializers.
 */
object ApiClient {

    const val BASE_URL: String = "http://65.2.81.129:3000/"

    private var sharedPreferences: SharedPreferences? = null

    /**
     * Call once in MainActivity onCreate
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("campuscare_session", Context.MODE_PRIVATE)
    }

    fun hasSavedSession(): Boolean {
        return !sharedPreferences?.getString("session_cookie", null).isNullOrEmpty()
    }

    fun clearSession() {
        sharedPreferences?.edit()?.clear()?.apply()
    }

    fun getSessionCookie(): String? {
        return sharedPreferences?.getString("session_cookie", null)
    }

    fun saveSessionCookie(cookie: String) {
        sharedPreferences?.edit()?.putString("session_cookie", cookie)?.apply()
    }

    // ── Simple Interceptor: Attach & Save Session Cookie ──
    private val sessionInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()

        // Attach cookie to outgoing request
        val cookie = getSessionCookie()
        if (!cookie.isNullOrEmpty()) {
            requestBuilder.addHeader("Cookie", cookie)
        }

        val response: Response = chain.proceed(requestBuilder.build())

        // Save Set-Cookie header from response
        val setCookieHeader = response.header("Set-Cookie")
        if (!setCookieHeader.isNullOrEmpty()) {
            // Store raw cookie (e.g. connect.sid=s%3A...)
            val cookieValue = setCookieHeader.split(";")[0]
            saveSessionCookie(cookieValue)
        }

        response
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(sessionInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
