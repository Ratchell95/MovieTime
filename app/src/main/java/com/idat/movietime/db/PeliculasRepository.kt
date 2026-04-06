package com.idat.movietime.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.idat.movietime.model.Pelicula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PeliculasRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    /**
     * Devuelve las películas filtradas por estado.
     *
     * Es una suspend function: se debe llamar desde un coroutine scope.
     * Internamente cambia al dispatcher IO para no bloquear el UI thread.
     *
     * Uso desde un ViewModel:
     *   viewModelScope.launch {
     *       val lista = repository.getPeliculasPorEstado("Activa")
     *       _peliculas.value = lista
     *   }
     */
    suspend fun getPeliculasPorEstado(estado: String): List<Pelicula> =
        withContext(Dispatchers.IO) {
            val lista = mutableListOf<Pelicula>()

            dbHelper.readableDatabase.rawQuery(
                """SELECT id_pelicula, titulo, duracion_min, clasificacion,
                          genero, formato, sinopsis, imagen_url, estado
                   FROM peliculas
                   WHERE estado = ?""",
                arrayOf(estado)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    lista.add(
                        Pelicula(
                            id            = cursor.getInt(cursor.getColumnIndexOrThrow("id_pelicula")),
                            titulo        = cursor.getString(cursor.getColumnIndexOrThrow("titulo"))        ?: "",
                            duracionMin   = cursor.getInt(cursor.getColumnIndexOrThrow("duracion_min")),
                            clasificacion = cursor.getString(cursor.getColumnIndexOrThrow("clasificacion")) ?: "",
                            genero        = cursor.getString(cursor.getColumnIndexOrThrow("genero"))        ?: "",
                            formato       = cursor.getString(cursor.getColumnIndexOrThrow("formato"))       ?: "2D",
                            sinopsis      = cursor.getString(cursor.getColumnIndexOrThrow("sinopsis"))      ?: "",
                            imagenUrl     = cursor.getString(cursor.getColumnIndexOrThrow("imagen_url"))    ?: "",
                            estado        = cursor.getString(cursor.getColumnIndexOrThrow("estado"))        ?: "Activa"
                        )
                    )
                }
            }
            lista
        }

    /**
     * Inserta una película en la BD.
     * También es suspend para no bloquear el UI thread.
     *
     * Retorna el rowId de la fila insertada, o -1 si hubo conflicto/error.
     */
    suspend fun insertarPelicula(p: Pelicula): Long =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("titulo",        p.titulo)
                put("duracion_min",  p.duracionMin)
                put("clasificacion", p.clasificacion)
                put("genero",        p.genero)
                put("formato",       p.formato)
                put("sinopsis",      p.sinopsis)
                put("imagen_url",    p.imagenUrl)
                put("estado",        p.estado)
            }
            dbHelper.writableDatabase.insertWithOnConflict(
                DatabaseHelper.TABLE_PELICULAS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
}