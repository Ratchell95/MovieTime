package com.idat.movietime.model

data class Pelicula(
    val id: Int,
    val titulo: String,
    val anio: Int,
    val posterUrl: String = ""
)