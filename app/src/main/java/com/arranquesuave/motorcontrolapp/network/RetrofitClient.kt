package com.arranquesuave.motorcontrolapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    
    // URLs del backend API
    private const val BASE_URL_LOCAL = "http://192.168.1.12:3000/"   // Casa del profesor (local)
    private const val BASE_URL_REMOTE = "http://177.247.175.4:8080/"
    private const val BASE_URL_TEST = "http://httpbin.org/"          // Para testing sin backend
    
    // URL actual (por defecto remoto para funcionar desde tu casa)
    private var currentBaseUrl = BASE_URL_REMOTE
    
    private val logging = HttpLoggingInterceptor().apply { 
        level = HttpLoggingInterceptor.Level.BODY 
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
    
    /**
     * Cambiar URL del backend según el modo de conexión
     */
    fun setBaseUrl(mode: ConnectionMode) {
        currentBaseUrl = when(mode) {
            ConnectionMode.LOCAL -> BASE_URL_LOCAL
            ConnectionMode.REMOTE -> BASE_URL_REMOTE  
            ConnectionMode.TEST -> BASE_URL_TEST
        }
    }
    
    /**
     * Obtener URL actual para debugging
     */
    fun getCurrentUrl(): String = currentBaseUrl
    
    enum class ConnectionMode {
        LOCAL,    // En casa del profesor
        REMOTE,   // Desde Internet (tu casa)
        TEST      // Modo testing
    }
}
