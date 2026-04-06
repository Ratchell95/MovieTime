package com.idat.movietime.model

import com.google.gson.annotations.SerializedName

data class Pelicula(
    @SerializedName(value = "idPelicula", alternate = ["id_pelicula", "id"])
    val id: Int = 0,

    @SerializedName("titulo")
    val titulo: String = "",

    @SerializedName(value = "duracionMin", alternate = ["duracion_min"])
    val duracionMin: Int = 0,

    @SerializedName("clasificacion")
    val clasificacion: String = "",

    @SerializedName("genero")
    val genero: String = "",

    @SerializedName("formato")
    val formato: String = "2D",

    @SerializedName("sinopsis")
    val sinopsis: String = "",

    @SerializedName(value = "imagenUrl", alternate = ["imagen_url"])
    val imagenUrl: String = "",

    @SerializedName("estado")
    val estado: String = "Activa",

    val anio: Int = 0,
    val posterUrl: String = "",
    val drawableRes: Int = 0
)