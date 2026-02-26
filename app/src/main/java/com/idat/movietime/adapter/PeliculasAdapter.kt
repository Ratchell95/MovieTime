package com.idat.movietime.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.idat.movietime.R
import com.idat.movietime.model.Pelicula

class PeliculasAdapter(
    private var peliculas: List<Pelicula>,
    private val onItemClick: (Pelicula) -> Unit
) : RecyclerView.Adapter<PeliculasAdapter.PeliculaViewHolder>() {

    class PeliculaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivPoster)
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvAnio: TextView = view.findViewById(R.id.tvAnio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeliculaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pelicula, parent, false)
        return PeliculaViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeliculaViewHolder, position: Int) {
        val pelicula = peliculas[position]
        holder.tvTitulo.text = pelicula.titulo
        holder.tvAnio.text = pelicula.anio.toString()

        holder.itemView.setOnClickListener {
            onItemClick(pelicula)
        }
    }

    override fun getItemCount() = peliculas.size

    fun updatePeliculas(nuevasPeliculas: List<Pelicula>) {
        peliculas = nuevasPeliculas
        notifyDataSetChanged()
    }
}