package com.idat.movietime.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.idat.movietime.R
import com.idat.movietime.model.Pelicula

class PeliculasAdapter(
    private var items: List<Pelicula>,
    private val onClick: (Pelicula) -> Unit
) : RecyclerView.Adapter<PeliculasAdapter.VH>() {


    private val drawableMap = mapOf(
        1 to R.drawable.ic_pelicula1,
        2 to R.drawable.ic_pelicula2,
        3 to R.drawable.ic_pelicula3,
        4 to R.drawable.ic_pelicula4,
        5 to R.drawable.ic_pelicula5,
        6 to R.drawable.ic_pelicula6,
        7 to R.drawable.ic_pelicula7,
        8 to R.drawable.ic_pelicula8
    )

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivPoster: ImageView = v.findViewById(R.id.ivPoster)
        val tvTitulo: TextView  = v.findViewById(R.id.tvTitulo)
        val tvAnio:   TextView  = v.findViewById(R.id.tvAnio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pelicula, parent, false))

    ///////////////////////////////////////
    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        h.tvTitulo.text = p.titulo
        h.tvAnio.text   = "${p.duracionMin / 60}h ${p.duracionMin % 60}m | ${p.clasificacion}"
        h.ivPoster.scaleType = ImageView.ScaleType.CENTER_CROP

        // Tus películas originales vinculadas a sus IDs locales
        val drawableMap = mapOf(
            1 to R.drawable.ic_pelicula1, 2 to R.drawable.ic_pelicula2,
            3 to R.drawable.ic_pelicula3, 4 to R.drawable.ic_pelicula4,
            5 to R.drawable.ic_pelicula5, 6 to R.drawable.ic_pelicula6,
            7 to R.drawable.ic_pelicula7, 8 to R.drawable.ic_pelicula8
        )
        val resOriginal = drawableMap[p.id] ?: 0

        when {
            // PRIORIDAD 1: Si desde tu panel web le pusiste una URL (MySQL manda)
            p.imagenUrl.isNotEmpty() -> {
                Glide.with(h.ivPoster.context)
                    .load(p.imagenUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_placeholder)
                    .into(h.ivPoster)
            }

            // PRIORIDAD 2: Si no hay URL en la BD, usa el Drawable original
            resOriginal != 0 -> {
                h.ivPoster.setImageResource(resOriginal)
            }

            // PRIORIDAD 3: Película nueva sin imagen en MySQL (Logo blanco)
            else -> {
                h.ivPoster.setImageResource(R.drawable.ic_movie_placeholder)
            }
        }
        h.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = items.size

    fun updatePeliculas(nuevas: List<Pelicula>) {
        items = nuevas
        notifyDataSetChanged()
    }
}