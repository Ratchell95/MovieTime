package com.idat.movietime.network

/**
 * FIX: agregado campo "errors" para que coincida con ApiResponse.java del backend.
 * Sin este campo, cuando el servidor devuelve errores de validación (un Map<String,String>)
 * Gson lanzaría una excepción al intentar deserializarlo.
 *
 * FIX: "message" es nullable (String?) porque ApiResponse.ok(data) no incluye
 * el campo message cuando es null (@JsonInclude NON_NULL en el servidor).
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,       // FIX: era non-null, puede no venir en respuestas exitosas
    val data:    T?,
    val errors:  Any? = null    // FIX: campo nuevo — coincide con ApiResponse.java del backend
)