package com.arranquesuave.motorcontrolapp.network

import com.arranquesuave.motorcontrolapp.auth.network.AuthApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL_LOCAL  = "http://192.168.1.24:3000/"
    private const val BASE_URL_REMOTE = "http://177.247.175.4:8080/"
    private const val BASE_URL_TEST   = "http://httpbin.org/"

    private var currentBaseUrl = BASE_URL_REMOTE

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private fun buildRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(currentBaseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy { buildRetrofit().create(AuthApi::class.java) }
    val motorApi: MotorApi by lazy { buildRetrofit().create(MotorApi::class.java) }

    fun setBaseUrl(mode: ConnectionMode) {
        currentBaseUrl = when (mode) {
            ConnectionMode.LOCAL  -> BASE_URL_LOCAL
            ConnectionMode.REMOTE -> BASE_URL_REMOTE
            ConnectionMode.TEST   -> BASE_URL_TEST
        }
    }

    fun getCurrentUrl(): String = currentBaseUrl

    enum class ConnectionMode { LOCAL, REMOTE, TEST }
}
