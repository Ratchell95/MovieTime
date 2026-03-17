package com.idat.movietime.model

data class LoginRequest(
    val email:    String,
    val password: String
)


data class LoginResponse(
    val token:     String,
    val idUsuario: Int,
    val nombres:   String,
    val apellidos: String,
    val email:     String,
    val rol:       String
)
