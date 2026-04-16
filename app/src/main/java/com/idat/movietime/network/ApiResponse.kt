package com.idat.movietime.network

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data:    T?,
    val errors:  Any? = null
)