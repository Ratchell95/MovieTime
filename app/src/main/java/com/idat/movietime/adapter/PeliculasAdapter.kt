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

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivPoster: ImageView = v.findViewById(R.id.ivPoster)
        val tvTitulo: TextView  = v.findViewById(R.id.tvTitulo)  // ID real del XML
        val tvAnio:   TextView  = v.findViewById(R.id.tvAnio)    // ID real del XML
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pelicula, parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        h.tvTitulo.text = p.titulo
        h.tvAnio.text   = "${p.duracionMin / 60}h ${p.duracionMin % 60}m | ${p.clasificacion}"

        val url = p.posterUrl.ifEmpty { p.imagenUrl }
        if (url.isNotEmpty()) {
            Glide.with(h.ivPoster)
                .load(url)
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher)
                .into(h.ivPoster)
        } else {
            h.ivPoster.setImageResource(R.mipmap.ic_launcher)
        }
        h.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = items.size

    fun updatePeliculas(nuevas: List<Pelicula>) {
        items = nuevas
        notifyDataSetChanged()
    }
}