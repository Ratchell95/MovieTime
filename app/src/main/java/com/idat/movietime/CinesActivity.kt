package com.idat.movietime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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

    private lateinit var drawerLayout: DrawerLayout

    private val sedes = listOf(
        Sede("MOVIE TIME\nPREMIUM BASADRE", "CAL. LAS PALMERAS URB. ORRANTIA 0343", "2D",      -12.0922, -77.0324, R.drawable.ic_sede1),
        Sede("MOVIETIME\nIQUITOS",          "Ramón Castilla 610, Iquitos 16001",     "2D",      -3.7491,  -73.2516, R.drawable.ic_sede2),
        Sede("MOVIETIME\nPREMIUM IQUITOS",  "Mall Aventura, Iquitos 16007",          "2D | 3D", -3.7689,  -73.2374, R.drawable.ic_sede3),
        Sede("JAEN",       "Av. Mesones Muro 3002, Jaén",          "2D",      -5.7072,  -78.8052, R.drawable.ic_sede4),
        Sede("CHORRILLOS", "Av. Defensores del Morro 1277",        "2D | XD", -12.1667, -77.0167, R.drawable.ic_sede5),
        Sede("VES1",       "Villa El Salvador, Lima",              "2D",      -12.2142, -76.9364, R.drawable.ic_sede6)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cines)

        drawerLayout = findViewById(R.id.drawerLayout)

        val recycler = findViewById<RecyclerView>(R.id.recyclerCines)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = CinesAdapter(sedes)

        setupBottomNav()
        setupDrawer()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_cines
        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_inicio     -> { startActivity(Intent(this, InicioActivity::class.java)); true }
                R.id.nav_peliculas  -> { startActivity(Intent(this, PeliculasActivity::class.java)); true }
                R.id.nav_cines      -> true
                R.id.nav_confiteria -> { startActivity(Intent(this, ConfiteriaActivity::class.java)); true }
                else -> false
            }
        }
    }

    private fun setupDrawer() {
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
            else drawerLayout.openDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.navCartelera)?.setOnClickListener   { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, PeliculasActivity::class.java)) }
        findViewById<View>(R.id.navEntradas)?.setOnClickListener    { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, HistorialActivity::class.java)) }
        findViewById<View>(R.id.navConfiteria)?.setOnClickListener  { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, ConfiteriaActivity::class.java)) }
        findViewById<View>(R.id.navHistorial)?.setOnClickListener   { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, HistorialActivity::class.java)) }
        findViewById<View>(R.id.navQR)?.setOnClickListener          { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, QRScannerActivity::class.java)) }
        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            com.idat.movietime.network.SessionManager(this).cerrarSesion()
            val intent = Intent(this, SesionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
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

            if (sede.imagenRes != 0) {
                h.ivImagen.setImageResource(sede.imagenRes)
                h.ivImagen.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                h.ivImagen.setImageResource(R.mipmap.ic_icono)
            }

            h.btnMapa.setOnClickListener {
                val uri = Uri.parse("geo:${sede.latitud},${sede.longitud}?q=${Uri.encode(sede.nombre.replace("\n", " "))}")
                val gmaps = Intent(Intent.ACTION_VIEW, uri)
                gmaps.setPackage("com.google.android.apps.maps")

                if (gmaps.resolveActivity(this@CinesActivity.packageManager) != null) {
                    this@CinesActivity.startActivity(gmaps)
                } else {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=${sede.latitud},${sede.longitud}")
                    )
                    this@CinesActivity.startActivity(webIntent)
                }
            }
        }

        override fun getItemCount() = items.size
    }
}