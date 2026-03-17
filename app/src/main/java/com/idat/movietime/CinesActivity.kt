package com.idat.movietime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

data class Sede(
    val nombre:    String,
    val direccion: String,
    val formato:   String,
    val latitud:   Double,
    val longitud:  Double,
    val imagenRes: Int = 0
)

class CinesActivity : AppCompatActivity() {

    private val sedes = listOf(
        Sede(
            nombre    = "MOVIE TIME PREMIUM BASADRE",
            direccion = "CAL. LAS PALMERAS URB. ORRANTIA 0343",
            formato   = "2D",
            latitud   = -12.0922,
            longitud  = -77.0324
        ),
        Sede(
            nombre    = "MOVIETIME IQUITOS",
            direccion = "Ramón Castilla 610, Iquitos 16001",
            formato   = "2D",
            latitud   = -3.7491,
            longitud  = -73.2516
        ),
        Sede(
            nombre    = "MOVIETIME PREMIUM IQUITOS",
            direccion = "Mall Aventura, Iquitos 16007",
            formato   = "2D | 3D",
            latitud   = -3.7689,
            longitud  = -73.2374
        ),
        Sede(
            nombre    = "JAEN",
            direccion = "Av. Manuel Antonio Mesones Muro 3002, Jaén 06801",
            formato   = "2D",
            latitud   = -5.7072,
            longitud  = -78.8052
        ),
        Sede(
            nombre    = "CHORRILLOS",
            direccion = "Av. Defensores del Morro 1277, Chorrillos",
            formato   = "2D | XD",
            latitud   = -12.1667,
            longitud  = -77.0167
        ),
        Sede(
            nombre    = "VES1",
            direccion = "Villa El Salvador, Lima",
            formato   = "2D",
            latitud   = -12.2142,
            longitud  = -76.9364
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cines)

        val recycler = findViewById<RecyclerView>(R.id.recyclerCines)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = CinesAdapter(sedes)

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_cines
        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_inicio     -> { startActivity(Intent(this, PeliculasActivity::class.java)); true }
                R.id.nav_peliculas  -> { startActivity(Intent(this, PeliculasActivity::class.java)); true }
                R.id.nav_cines      -> true
                R.id.nav_confiteria -> { startActivity(Intent(this, ConfiteriaActivity::class.java)); true }
                else -> false
            }
        }
    }

    inner class CinesAdapter(private val items: List<Sede>) :
        RecyclerView.Adapter<CinesAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val ivImagen:  ImageView = v.findViewById(R.id.ivImagenCine)
            val tvNombre:  TextView  = v.findViewById(R.id.tvNombreCine)
            val tvDir:     TextView  = v.findViewById(R.id.tvDireccionCine)
            val tvFormato: TextView  = v.findViewById(R.id.tvFormato)
            val btnMapa:   Button    = v.findViewById(R.id.btnVerMapa)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cine, parent, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val sede = items[pos]
            h.tvNombre.text  = sede.nombre
            h.tvDir.text     = sede.direccion
            h.tvFormato.text = sede.formato

            h.btnMapa.setOnClickListener {
                val uri = Uri.parse("geo:${sede.latitud},${sede.longitud}?q=${Uri.encode(sede.nombre)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Fallback: abrir en navegador
                    val webUri = Uri.parse("https://maps.google.com/?q=${sede.latitud},${sede.longitud}")
                    startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
