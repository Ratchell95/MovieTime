package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.idat.movietime.adapter.PeliculasAdapter
import com.idat.movietime.db.PeliculasRepository
import com.idat.movietime.model.Pelicula

class InicioActivity : AppCompatActivity() {

    private lateinit var bannerViewPager:   ViewPager2
    private lateinit var layoutIndicadores: LinearLayout
    private lateinit var drawerLayout:      DrawerLayout  // ✅ añadido
    private var bannerHelper: BannerHelper? = null

    private val drawableMap = mapOf(
        1 to R.drawable.ic_pelicula1, 2 to R.drawable.ic_pelicula2,
        3 to R.drawable.ic_pelicula3, 4 to R.drawable.ic_pelicula4,
        5 to R.drawable.ic_pelicula5, 6 to R.drawable.ic_pelicula6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        drawerLayout      = findViewById(R.id.drawerLayout)   // ✅
        bannerViewPager   = findViewById(R.id.bannerViewPager)
        layoutIndicadores = findViewById(R.id.layoutIndicadores)

        setupBanners()
        setupPeliculasCartelera()
        setupProximamente()
        setupBottomNav()
        setupDrawer()   // ✅

        findViewById<TextView>(R.id.tvVerTodas)?.setOnClickListener {
            startActivity(Intent(this, PeliculasActivity::class.java))
        }
    }

    private fun setupBanners() {
        val banners = listOf<Any>(R.drawable.ic_banner1, R.drawable.ic_banner2)
        bannerViewPager.adapter = BannerAdapter(banners)
        actualizarIndicadores(banners.size, 0)
        bannerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pos: Int) = actualizarIndicadores(banners.size, pos)
        })
        findViewById<TextView>(R.id.btnBannerAnterior)?.setOnClickListener {
            val prev = if (bannerViewPager.currentItem > 0) bannerViewPager.currentItem - 1 else banners.size - 1
            bannerViewPager.setCurrentItem(prev, true)
        }
        findViewById<TextView>(R.id.btnBannerSiguiente)?.setOnClickListener {
            bannerViewPager.setCurrentItem((bannerViewPager.currentItem + 1) % banners.size, true)
        }
        bannerHelper = BannerHelper(bannerViewPager, banners.size, 4000L)
        bannerHelper?.start()
    }

    private fun actualizarIndicadores(total: Int, activo: Int) {
        layoutIndicadores.removeAllViews()
        val density = resources.displayMetrics.density
        for (i in 0 until total) {
            val dot = View(this).apply {
                val size = (8 * density).toInt(); val margin = (4 * density).toInt()
                layoutParams = LayoutParams(size, size).apply { setMargins(margin, 0, margin, 0) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (i == activo) android.graphics.Color.WHITE
                    else android.graphics.Color.parseColor("#88FFFFFF"))
                }
            }
            layoutIndicadores.addView(dot)
        }
    }

    private fun setupPeliculasCartelera() {
        val recycler = findViewById<RecyclerView>(R.id.recyclerPeliculasInicio)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.isNestedScrollingEnabled = false
        val peliculas: List<Pelicula> = try {
            PeliculasRepository(this).getPeliculasPorEstado("Activa").ifEmpty { fallback() }
        } catch (e: Exception) { fallback() }
        recycler.adapter = PeliculasAdapter(peliculas) { pelicula ->
            startActivity(Intent(this, DetallePeliculaActivity::class.java).apply {
                putExtra("id_pelicula",   pelicula.id)
                putExtra("titulo",        pelicula.titulo)
                putExtra("duracion_min",  pelicula.duracionMin)
                putExtra("clasificacion", pelicula.clasificacion)
                putExtra("sinopsis",      pelicula.sinopsis)
                putExtra("imagen_url",    pelicula.posterUrl.ifEmpty { pelicula.imagenUrl })
                putExtra("drawable_res",  drawableMap[pelicula.id] ?: 0)
            })
        }
    }

    private fun setupProximamente() {
        val container = findViewById<LinearLayout>(R.id.layoutProximamente) ?: return
        val lista = listOf(R.drawable.ic_pelicula5, R.drawable.ic_pelicula6)
        val density = resources.displayMetrics.density
        lista.forEach { res ->
            val iv = ImageView(this).apply {
                val w = (140 * density).toInt(); val h = (200 * density).toInt(); val m = (8 * density).toInt()
                layoutParams = LayoutParams(w, h).apply { setMargins(m, 0, m, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(res)
            }
            container.addView(iv)
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_inicio     -> true
                R.id.nav_peliculas  -> { startActivity(Intent(this, PeliculasActivity::class.java)); true }
                R.id.nav_cines      -> { startActivity(Intent(this, CinesActivity::class.java)); true }
                R.id.nav_confiteria -> { startActivity(Intent(this, ConfiteriaActivity::class.java)); true }
                else -> false
            }
        }
    }

    // ✅ Drawer — igual que PeliculasActivity
    private fun setupDrawer() {
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            if (drawerLayout.isDrawerOpen(Gravity.START)) drawerLayout.closeDrawer(Gravity.START)
            else drawerLayout.openDrawer(Gravity.START)
        }
        findViewById<View>(R.id.navCartelera)?.setOnClickListener   { drawerLayout.closeDrawer(Gravity.START); startActivity(Intent(this, PeliculasActivity::class.java)) }
        findViewById<View>(R.id.navEntradas)?.setOnClickListener    { drawerLayout.closeDrawer(Gravity.START); startActivity(Intent(this, HistorialActivity::class.java)) }
        findViewById<View>(R.id.navConfiteria)?.setOnClickListener  { drawerLayout.closeDrawer(Gravity.START); startActivity(Intent(this, ConfiteriaActivity::class.java)) }
        findViewById<View>(R.id.navHistorial)?.setOnClickListener   { drawerLayout.closeDrawer(Gravity.START); startActivity(Intent(this, HistorialActivity::class.java)) }
        findViewById<View>(R.id.navQR)?.setOnClickListener         { drawerLayout.closeDrawer(Gravity.START); startActivity(Intent(this, QRScannerActivity::class.java)) }
        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun fallback() = listOf(
        Pelicula(1, "Iron Lung: Océano de Sangre", 2026, "", 108, "R",     "Terror",    "2D", "", "", "Activa", R.drawable.ic_pelicula1),
        Pelicula(2, "Espía Entre Animales",        2026, "", 95,  "G",     "Animación", "3D", "", "", "Activa", R.drawable.ic_pelicula2),
        Pelicula(3, "Avengers: Doomsday",          2026, "", 150, "PG-13", "Acción",    "XD", "", "", "Activa", R.drawable.ic_pelicula3),
        Pelicula(4, "Minecraft: La Película",      2026, "", 110, "PG",    "Aventura",  "2D", "", "", "Activa", R.drawable.ic_pelicula4)
    )

    override fun onDestroy() { super.onDestroy(); bannerHelper?.stop() }
}