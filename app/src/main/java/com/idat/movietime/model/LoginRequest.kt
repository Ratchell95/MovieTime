package com.idat.movietime.model

/**
 * FIX: el campo era "email" pero la app envía el número de documento.
 * Renombrado a "documento" para que coincida con lo que espera el backend
 * (LoginRequest.java del servidor también tiene el campo "documento").
 */
data class LoginRequest(
    val documento: String,   // FIX: era "email"
    val password:  String
)