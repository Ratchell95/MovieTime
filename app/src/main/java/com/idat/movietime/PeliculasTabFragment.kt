package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idat.movietime.adapter.PeliculasAdapter
import com.idat.movietime.db.PeliculasRepository
import com.idat.movietime.model.Pelicula
import kotlinx.coroutines.launch
import com.idat.movietime.network.RetrofitClient

class PeliculasTabFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PeliculasAdapter
    private var tipo: String = ""

    // 🔑 FIX 1: Guard para no recargar si ya hay datos cargados
    private var yaSeCargaronDatos = false

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
        // 🔑 FIX 2: Retener el fragment para que no se destruya al cambiar de tab
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_peliculas_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)


        if (yaSeCargaronDatos && ::adapter.isInitialized) {
            recyclerView.adapter = adapter
            return
        }

        val estadoBD = when (tipo) {
            "proximamente" -> "Inactiva"
            else           -> "Activa"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            var peliculasParaMostrar: List<Pelicula> = emptyList()

            try {
                val response = when (tipo) {
                    "proximamente" -> RetrofitClient.api.getProximamente()
                    else           -> RetrofitClient.api.getPeliculas()
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    peliculasParaMostrar = response.body()?.data ?: emptyList()
                    // Sincroniza con SQLite en background (ya está en Dispatchers.IO por el repository)
                    val repository = PeliculasRepository(requireContext())
                    peliculasParaMostrar.forEach { p -> repository.insertarPelicula(p) }
                } else {
                    peliculasParaMostrar = cargarDesdeSQLite(estadoBD)
                }
            } catch (e: Exception) {
                peliculasParaMostrar = cargarDesdeSQLite(estadoBD)
            }

            // Filtrar por tab DESPUÉS de obtener los datos
            peliculasParaMostrar = when (tipo) {
                "opera" -> peliculasParaMostrar.filter {
                    it.genero.contains("Opera", ignoreCase = true) ||
                            it.genero.contains("Ópera", ignoreCase = true)
                }
                "cartelera" -> peliculasParaMostrar.filter {
                    !it.genero.contains("Opera", ignoreCase = true) &&
                            !it.genero.contains("Ópera", ignoreCase = true)
                }
                else -> peliculasParaMostrar
            }

            adapter = PeliculasAdapter(peliculasParaMostrar) { pelicula ->
                val intent = Intent(requireContext(), DetallePeliculaActivity::class.java).apply {
                    putExtra("id_pelicula",   pelicula.id)
                    putExtra("titulo",        pelicula.titulo)
                    putExtra("duracion_min",  pelicula.duracionMin)
                    putExtra("clasificacion", pelicula.clasificacion)
                    putExtra("sinopsis",      pelicula.sinopsis)
                    putExtra("imagen_url",    pelicula.imagenUrl)
                }
                startActivity(intent)
            }
            recyclerView.adapter = adapter
            yaSeCargaronDatos = true
        }
    }

    private suspend fun cargarDesdeSQLite(estadoBD: String): List<Pelicula> {
        return try {
            if (tipo == "opera") {
                obtenerPeliculasFallback()
            } else {
                val lista = PeliculasRepository(requireContext()).getPeliculasPorEstado(estadoBD)
                lista.ifEmpty { obtenerPeliculasFallback() }
            }
        } catch (e: Exception) {
            obtenerPeliculasFallback()
        }
    }

    private fun obtenerPeliculasFallback(): List<Pelicula> = when (tipo) {
        "cartelera" -> listOf(
            Pelicula(id = 1, titulo = "Iron Lung: Océano de Sangre", anio = 2026, duracionMin = 108, clasificacion = "R", genero = "Terror", formato = "2D", sinopsis = "Basado en el videojuego indie de terror.", estado = "Activa"),
            Pelicula(id = 2, titulo = "Espía Entre Animales", anio = 2026, duracionMin = 95, clasificacion = "G", genero = "Animación / Comedia", formato = "3D", sinopsis = "Aventura animada para toda la familia.", estado = "Activa"),
            Pelicula(id = 3, titulo = "Avengers: Doomsday", anio = 2026, duracionMin = 150, clasificacion = "PG-13", genero = "Acción / Superhéroes", formato = "XD", sinopsis = "El universo Marvel enfrenta su mayor amenaza.", estado = "Activa"),
            Pelicula(id = 4, titulo = "Minecraft: La Película", anio = 2026, duracionMin = 110, clasificacion = "PG", genero = "Aventura / Familia", formato = "2D", sinopsis = "Cuatro inadaptados en el Overworld.", estado = "Activa")
        )
        "proximamente" -> listOf(
            Pelicula(id = 5, titulo = "Superman: Legacy", anio = 2026, duracionMin = 130, clasificacion = "PG-13", genero = "Acción", formato = "XD", sinopsis = "El regreso del Hombre de Acero.", estado = "Inactiva"),
            Pelicula(id = 6, titulo = "Jurassic World: Renacimiento", anio = 2026, duracionMin = 125, clasificacion = "PG-13", genero = "Aventura", formato = "3D", sinopsis = "Nueva expedición a una isla remota.", estado = "Inactiva")
        )
        "opera" -> listOf(
            Pelicula(id = 7, titulo = "La Traviata", anio = 2026, duracionMin = 180, clasificacion = "TP", genero = "Ópera", formato = "2D", sinopsis = "Ópera de Giuseppe Verdi en el cine.", estado = "Activa"),
            Pelicula(id = 8, titulo = "Carmen", anio = 2026, duracionMin = 165, clasificacion = "TP", genero = "Ópera", formato = "2D", sinopsis = "La ópera más famosa de Georges Bizet.", estado = "Activa")
        )
        else -> emptyList()
    }
}