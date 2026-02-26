package com.idat.movietime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idat.movietime.adapter.PeliculasAdapter
import com.idat.movietime.model.Pelicula

class PeliculasTabFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PeliculasAdapter
    private var tipo: String = ""

    companion object {
        private const val ARG_TIPO = "tipo"

        fun newInstance(tipo: String): PeliculasTabFragment {
            val fragment = PeliculasTabFragment()
            val args = Bundle()
            args.putString(ARG_TIPO, tipo)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tipo = arguments?.getString(ARG_TIPO) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_peliculas_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        adapter = PeliculasAdapter(obtenerPeliculas()) { pelicula ->
            Toast.makeText(context, "Seleccionaste: ${pelicula.titulo}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = adapter
    }

    private fun obtenerPeliculas(): List<Pelicula> {
        return when (tipo) {
            "cartelera" -> listOf(
                Pelicula(1, "Película 1", 2024),
                Pelicula(2, "Película 2", 2024),
                Pelicula(3, "Película 3", 2024),
                Pelicula(4, "Película 4", 2024)
            )
            "proximamente" -> listOf(
                Pelicula(5, "Próximamente 1", 2024),
                Pelicula(6, "Próximamente 2", 2024)
            )
            "opera" -> listOf(
                Pelicula(7, "Ópera 1", 2024),
                Pelicula(8, "Ópera 2", 2024)
            )
            else -> emptyList()
        }
    }
}