package com.idat.movietime.network

import com.idat.movietime.model.LoginRequest
import com.idat.movietime.model.LoginResponse
import retrofit2.Response
import retrofit2.http.*
import com.idat.movietime.model.Pelicula

interface MovieTimeApi {

    // FIX: el backend devuelve ApiResponse<LoginResponse> (con wrapper success/message/data),
    //      no LoginResponse directamente. Sin este cambio Gson mapea todo a null.
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<Map<String, String>>

    @GET("peliculas")
    suspend fun getPeliculas(): Response<ApiResponse<List<Pelicula>>>

    @GET("peliculas/cartelera")
    suspend fun getCartelera(): Response<ApiResponse<List<Pelicula>>>

    @GET("peliculas/proximamente")
    suspend fun getProximamente(): Response<ApiResponse<List<Pelicula>>>

    @GET("peliculas/{id}")
    suspend fun getPeliculaById(@Path("id") id: Int): Response<Map<String, Any>>

    @POST("peliculas")
    suspend fun crearPelicula(@Body pelicula: Map<String, Any>): Response<Map<String, Any>>

    @PUT("peliculas/{id}")
    suspend fun actualizarPelicula(
        @Path("id") id: Int,
        @Body datos: Map<String, Any>
    ): Response<Map<String, Any>>

    @DELETE("peliculas/{id}")
    suspend fun eliminarPelicula(@Path("id") id: Int): Response<Void>

    @GET("funciones/publico/hoy")
    suspend fun getFuncionesHoy(): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("funciones/publico/fecha/{fecha}")
    suspend fun getFuncionesByFecha(
        @Path("fecha") fecha: String
    ): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("funciones/pelicula/{peliculaId}")
    suspend fun getFuncionesByPelicula(
        @Path("peliculaId") peliculaId: Long
    ): Response<ApiResponse<List<Map<String, Any>>>>

    @POST("funciones")
    suspend fun crearFuncion(@Body body: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>

    @PATCH("funciones/{id}/estado")
    suspend fun cambiarEstadoFuncion(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("ordenes/cliente/{clienteId}")
    suspend fun getOrdenesByCliente(
        @Path("clienteId") clienteId: Long
    ): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("ordenes/hoy")
    suspend fun getVentasHoy(): Response<ApiResponse<Map<String, Any>>>

    @GET("ordenes/{numero}")
    suspend fun getOrdenByNumero(
        @Path("numero") numero: String
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("ordenes")
    suspend fun crearOrden(@Body body: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>

    @GET("productos/activos")
    suspend fun getProductosActivos(): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("productos/{id}")
    suspend fun getProductoById(
        @Path("id") id: Long
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("productos")
    suspend fun getProductosConfiteria(): Response<ApiResponse<List<com.idat.movietime.ProductoWeb>>>


    @GET("productos/categorias")
    suspend fun getCategorias(): Response<ApiResponse<List<Map<String, Any>>>>

    @POST("checkin/validar")
    suspend fun validarCheckin(@Body body: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>

    @GET("checkin/log/hoy")
    suspend fun getCheckinLogHoy(): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("checkin/funcion/{funcionId}")
    suspend fun getCheckinByFuncion(
        @Path("funcionId") funcionId: Long
    ): Response<ApiResponse<List<Map<String, Any>>>>
}
