package com.idat.movietime.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/api/"
    private var sessionManager: SessionManager? = null

    fun init(manager: SessionManager) {
        sessionManager = manager
    }
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // <-- CAMBIAR A 30
            .readTimeout(30, TimeUnit.SECONDS)    // <-- CAMBIAR A 30
            .writeTimeout(30, TimeUnit.SECONDS)   // <-- CAMBIAR A 30
            .build()
    }
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val api: MovieTimeApi by lazy {
        retrofit.create(MovieTimeApi::class.java)
    }
}