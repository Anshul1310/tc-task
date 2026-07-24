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
 * Simple Bearer Token API Client
 *
 * Saves auth token in SharedPreferences and attaches
 * Authorization: Bearer <token> header to all HTTP requests.
 */
object ApiClient {

    const val BASE_URL: String = "http://65.2.81.129:3000/"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("campuscare_session", Context.MODE_PRIVATE)
    }

    fun hasSavedSession(): Boolean {
        return !getAuthToken().isNullOrEmpty()
    }

    fun clearSession() {
        sharedPreferences?.edit()?.clear()?.apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences?.getString("auth_token", null)
    }

    fun saveAuthToken(token: String) {
        sharedPreferences?.edit()?.putString("auth_token", token)?.apply()
    }

    // ── Simple Auth Interceptor: Attach Authorization Header ──
    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()

        val token = getAuthToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
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
