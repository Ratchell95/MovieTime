package com.idat.movietime

import com.google.gson.annotations.SerializedName

data class ProductoWeb(
    @SerializedName("idProducto")      val idProducto:       Int    = 0,
    @SerializedName("nombre")          val nombre:           String = "",
    @SerializedName("precio")          val precio:           Double = 0.0,
    @SerializedName("stockActual")     val stockActual:      Int    = 0,
    @SerializedName("stockMinimo")     val stockMinimo:      Int    = 0,
    @SerializedName("estado")          val estado:           String = "Activo",
    @SerializedName("imagenUrl")       val imagenUrl:        String? = null,
    @SerializedName("categoria")       val categoria:        CategoriaWeb? = null,
    @SerializedName("nombreCategoria") val nombreCategoria:  String? = null
) {
    fun getNombreCat(): String =
        nombreCategoria ?: categoria?.nombre ?: "Otros"
}

data class CategoriaWeb(
    @SerializedName("idCategoria") val idCategoria: Int    = 0,
    @SerializedName("nombre")      val nombre:      String = ""
)