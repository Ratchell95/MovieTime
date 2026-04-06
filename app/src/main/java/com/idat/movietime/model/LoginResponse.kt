package com.idat.movietime.model

data class LoginResponse(
    val token:     String,
    val idUsuario: Int,
    val nombres:   String,
    val apellidos: String,
    val email:     String,
    val rol:       String
)
