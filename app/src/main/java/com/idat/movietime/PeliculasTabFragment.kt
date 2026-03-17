package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idat.movietime.adapter.PeliculasAdapter
import com.idat.movietime.db.PeliculasRepository
import com.idat.movietime.model.Pelicula

class PeliculasTabFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter:      PeliculasAdapter
    private var tipo: String = ""

    companion object {
        private const val ARG_TIPO = "tipo"
        fun newInstance(tipo: String): PeliculasTabFragment {
            val fragment = PeliculasTabFragment()
            fragment.arguments = Bundle().apply { putString(ARG_TIPO, tipo) }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tipo = arguments?.getString(ARG_TIPO) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_peliculas_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val estadoBD = when (tipo) {
            "cartelera"    -> "Activa"
            "proximamente" -> "Inactiva"
            "opera"        -> "Activa"
            else           -> "Activa"
        }

        val peliculas: List<Pelicula> = try {
            val lista = PeliculasRepository(requireContext()).getPeliculasPorEstado(estadoBD)

            lista.ifEmpty { obtenerPeliculasFallback() }
        } catch (e: Exception) {
            obtenerPeliculasFallback()
        }

        adapter = PeliculasAdapter(peliculas) { pelicula ->

            val intent = Intent(requireContext(), DetallePeliculaActivity::class.java).apply {
                putExtra("id_pelicula",   pelicula.id)
                putExtra("titulo",        pelicula.titulo)
                putExtra("duracion_min",  pelicula.duracionMin)
                putExtra("clasificacion", pelicula.clasificacion)
                putExtra("sinopsis",      pelicula.sinopsis)
                putExtra("imagen_url",    pelicula.posterUrl.ifEmpty { pelicula.imagenUrl })
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun obtenerPeliculasFallback(): List<Pelicula> = when (tipo) {
        "cartelera" -> listOf(
            Pelicula(1, "Iron Lung: Océano de Sangre", 2026, "", 108, "R",     "Terror",              "2D", "Basado en el videojuego indie de terror.", "", "Activa"),
            Pelicula(2, "Espía Entre Animales",        2026, "", 95,  "G",     "Animación / Comedia", "3D", "Aventura animada para toda la familia.",   "", "Activa"),
            Pelicula(3, "Avengers: Doomsday",          2026, "", 150, "PG-13", "Acción / Superhéroes","XD", "El universo Marvel enfrenta su mayor amenaza.", "", "Activa"),
            Pelicula(4, "Minecraft: La Película",      2026, "", 110, "PG",    "Aventura / Familia",  "2D", "Cuatro inadaptados en el Overworld.",      "", "Activa")
        )
        "proximamente" -> listOf(
            Pelicula(5, "Superman: Legacy",             2026, "", 130, "PG-13", "Acción",   "XD", "El regreso del Hombre de Acero.",     "", "Inactiva"),
            Pelicula(6, "Jurassic World: Renacimiento", 2026, "", 125, "PG-13", "Aventura", "3D", "Nueva expedición a una isla remota.", "", "Inactiva")
        )
        "opera" -> listOf(
            Pelicula(7, "La Traviata", 2026, "", 180, "TP", "Ópera", "2D", "Ópera de Giuseppe Verdi en el cine.",  "", "Activa"),
            Pelicula(8, "Carmen",      2026, "", 165, "TP", "Ópera", "2D", "La ópera más famosa de Georges Bizet.", "", "Activa")
        )
        else -> emptyList()
    }
}