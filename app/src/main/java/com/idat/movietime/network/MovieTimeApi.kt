package com.idat.movietime.network

import com.idat.movietime.model.LoginRequest
import com.idat.movietime.model.LoginResponse
import com.idat.movietime.model.Pelicula
import com.idat.movietime.model.Funcion
import com.idat.movietime.model.Butaca
import com.idat.movietime.model.Venta
import com.idat.movietime.model.Producto
import com.idat.movietime.model.Promocion
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieTimeApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("peliculas")
    suspend fun getPeliculas(): Response<List<Pelicula>>

    @GET("peliculas/cartelera")
    suspend fun getPeliculasCartelera(): Response<List<Pelicula>>

    @GET("peliculas/proximamente")
    suspend fun getPeliculasProximamente(): Response<List<Pelicula>>

    @GET("peliculas/{id}")
    suspend fun getPeliculaById(
        @Path("id") id: Int
    ): Response<Pelicula>

    @POST("peliculas")
    suspend fun crearPelicula(
        @Body pelicula: Pelicula
    ): Response<Pelicula>

    @PUT("peliculas/{id}")
    suspend fun actualizarPelicula(
        @Path("id") id: Int,
        @Body pelicula: Pelicula
    ): Response<Pelicula>

    @DELETE("peliculas/{id}")
    suspend fun eliminarPelicula(
        @Path("id") id: Int
    ): Response<Unit>

    @GET("funciones")
    suspend fun getFunciones(): Response<List<Funcion>>

    @GET("funciones/pelicula/{idPelicula}")
    suspend fun getFuncionesByPelicula(
        @Path("idPelicula") idPelicula: Int
    ): Response<List<Funcion>>

    @GET("funciones/{id}/butacas")
    suspend fun getButacasByFuncion(
        @Path("id") idFuncion: Int
    ): Response<List<Butaca>>


    @POST("ventas")
    suspend fun crearVenta(
        @Body venta: Venta
    ): Response<Venta>

    @GET("ventas/cliente/{idCliente}")
    suspend fun getVentasByCliente(
        @Path("idCliente") idCliente: Int
    ): Response<List<Venta>>

    @GET("ventas/{id}")
    suspend fun getVentaById(
        @Path("id") id: Int
    ): Response<Venta>

    @POST("qr/validar")
    suspend fun validarQR(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("productos")
    suspend fun getProductos(): Response<List<Producto>>

    @GET("productos/{id}")
    suspend fun getProductoById(
        @Path("id") id: Int
    ): Response<Producto>

    @POST("productos")
    suspend fun crearProducto(
        @Body producto: Producto
    ): Response<Producto>

    @PUT("productos/{id}")
    suspend fun actualizarProducto(
        @Path("id") id: Int,
        @Body producto: Producto
    ): Response<Producto>

    @GET("promociones/activas")
    suspend fun getPromocionesActivas(): Response<List<Promocion>>

    @GET("promociones/{id}")
    suspend fun getPromocionById(
        @Path("id") id: Int
    ): Response<Promocion>


    @GET("reportes")
    suspend fun getReportes(
        @Query("fechaInicio")   fechaInicio: String,
        @Query("fechaFin")      fechaFin: String,
        @Query("tipoReporte")   tipoReporte: String,
        @Query("formatoExport") formato: String
    ): Response<ByteArray>
}
