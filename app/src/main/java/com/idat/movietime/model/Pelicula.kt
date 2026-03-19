package com.idat.movietime.model

data class Pelicula(
    val id:            Int    = 0,
    val titulo:        String = "",
    val anio:          Int    = 0,
    val posterUrl:     String = "",
    val duracionMin:   Int    = 0,
    val clasificacion: String = "",
    val genero:        String = "",
    val formato:       String = "2D",
    val sinopsis:      String = "",
    val imagenUrl:     String = "",
    val estado:        String = "Activa",
    val drawableRes:   Int    = 0
)