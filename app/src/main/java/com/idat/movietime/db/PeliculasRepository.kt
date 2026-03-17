package com.idat.movietime.db

import android.content.Context
import com.idat.movietime.model.Pelicula

class PeliculasRepository(private val context: Context) {

    private val dbHelper = MovieTimeDatabaseHelper(context)

    fun getPeliculasPorEstado(estado: String): List<Pelicula> {
        val lista   = mutableListOf<Pelicula>()
        val db      = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            """SELECT id_pelicula, titulo, duracion_min, clasificacion,
                      genero, formato, sinopsis, imagen_url, estado
               FROM peliculas
               WHERE estado = ?""",
            arrayOf(estado)
        )

        while (cursor.moveToNext()) {
            lista.add(
                Pelicula(
                    id            = cursor.getInt(cursor.getColumnIndexOrThrow("id_pelicula")),
                    titulo        = cursor.getString(cursor.getColumnIndexOrThrow("titulo")) ?: "",
                    duracionMin   = cursor.getInt(cursor.getColumnIndexOrThrow("duracion_min")),
                    clasificacion = cursor.getString(cursor.getColumnIndexOrThrow("clasificacion")) ?: "",
                    genero        = cursor.getString(cursor.getColumnIndexOrThrow("genero")) ?: "",
                    formato       = cursor.getString(cursor.getColumnIndexOrThrow("formato")) ?: "2D",
                    sinopsis      = cursor.getString(cursor.getColumnIndexOrThrow("sinopsis")) ?: "",
                    imagenUrl     = cursor.getString(cursor.getColumnIndexOrThrow("imagen_url")) ?: "",
                    estado        = cursor.getString(cursor.getColumnIndexOrThrow("estado")) ?: "Activa"
                )
            )
        }
        cursor.close()
        return lista
    }

    fun insertarPelicula(p: Pelicula): Long {
        val db = dbHelper.writableDatabase
        val values = android.content.ContentValues().apply {
            put("titulo",        p.titulo)
            put("duracion_min",  p.duracionMin)
            put("clasificacion", p.clasificacion)
            put("genero",        p.genero)
            put("formato",       p.formato)
            put("sinopsis",      p.sinopsis)
            put("imagen_url",    p.imagenUrl)
            put("estado",        p.estado)
        }
        return db.insertWithOnConflict(
            MovieTimeDatabaseHelper.TABLE_PELICULAS,
            null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
        )
    }
}
