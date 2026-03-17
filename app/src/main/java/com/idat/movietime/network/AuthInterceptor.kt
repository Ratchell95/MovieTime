package com.idat.movietime.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = sessionManager?.getToken()
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        val requestConToken = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()

        return chain.proceed(requestConToken)
    }
}
