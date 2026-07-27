package com.anshul.campuscare.data.network

import android.content.Context
import android.content.SharedPreferences
import com.anshul.campuscare.data.model.User
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL: String = "http://13.203.222.79:3000/"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("campuscare_session", Context.MODE_PRIVATE)
    }

    fun hasSavedSession(): Boolean {
        val token = getAuthToken()
        if (token != null) {
            if (token.isNotEmpty()) {
                return true
            }
        }
        return false
    }

    fun getAuthToken(): String? {
        if (sharedPreferences != null) {
            return sharedPreferences?.getString("auth_token", null)
        }
        return null
    }

    fun saveAuthToken(token: String) {
        if (sharedPreferences != null) {
            val editor = sharedPreferences?.edit()
            if (editor != null) {
                editor.putString("auth_token", token)
                editor.apply()
            }
        }
    }

    fun saveUser(user: User) {
        if (sharedPreferences != null) {
            val editor = sharedPreferences?.edit()
            if (editor != null) {
                editor.putInt("user_id", user.id)
                editor.putString("user_name", user.name)
                editor.putString("user_email", user.email)
                if (user.anonymousUsername != null) {
                    editor.putString("user_anonymous_username", user.anonymousUsername)
                }
                if (user.avatarColor != null) {
                    editor.putString("user_avatar_color", user.avatarColor)
                }
                editor.apply()
            }
        }
    }

    fun getUser(): User? {
        if (sharedPreferences == null) {
            return null
        }
        val id = sharedPreferences?.getInt("user_id", -1) ?: -1
        if (id == -1) {
            return null
        }
        val name = sharedPreferences?.getString("user_name", "") ?: ""
        val email = sharedPreferences?.getString("user_email", "") ?: ""
        val anonymousUsername = sharedPreferences?.getString("user_anonymous_username", null)
        val avatarColor = sharedPreferences?.getString("user_avatar_color", null)

        return User(
            id = id,
            email = email,
            name = name,
            anonymousUsername = anonymousUsername,
            avatarColor = avatarColor,
            createdAt = null
        )
    }

    fun clearSession() {
        if (sharedPreferences != null) {
            val editor = sharedPreferences?.edit()
            if (editor != null) {
                editor.clear()
                editor.apply()
            }
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        val token = getAuthToken()
        if (token != null) {
            if (token.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer " + token)
            }
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
