package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.idat.movietime.adapter.PeliculasAdapter
import com.idat.movietime.db.PeliculasRepository
import com.idat.movietime.model.Pelicula
import com.idat.movietime.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InicioActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerPeliculasInicio: RecyclerView
    private lateinit var layoutProximamente: LinearLayout

    private lateinit var bannerViewPager: ViewPager2
    private var bannerHelper: BannerHelper? = null

    private val drawableMap = mapOf(
        1 to R.drawable.ic_pelicula1, 2 to R.drawable.ic_pelicula2,
        3 to R.drawable.ic_pelicula3, 4 to R.drawable.ic_pelicula4,
        5 to R.drawable.ic_pelicula5, 6 to R.drawable.ic_pelicula6,
        7 to R.drawable.ic_pelicula7, 8 to R.drawable.ic_pelicula8
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        drawerLayout = findViewById(R.id.drawerLayout)
        setupDrawer()
        recyclerPeliculasInicio = findViewById(R.id.recyclerPeliculasInicio)
        layoutProximamente = findViewById(R.id.layoutProximamente)

        bannerViewPager = findViewById(R.id.bannerViewPager)
        recyclerPeliculasInicio.layoutManager = GridLayoutManager(this, 2)

        setupBanner()
        setupBottomNav()
        setupDrawer()
        cargarPeliculas()
    }

    private fun setupBanner() {
        val banners = listOf(
            R.drawable.ic_banner1,
            R.drawable.ic_banner2
        )

        bannerViewPager.adapter = BannerAdapter(banners)
        bannerHelper = BannerHelper(bannerViewPager, banners.size, 3000L)
        bannerHelper?.start()

        findViewById<View>(R.id.btnBannerAnterior)?.setOnClickListener {
            val current = bannerViewPager.currentItem
            if (current > 0) {
                bannerViewPager.currentItem = current - 1
            } else {
                bannerViewPager.currentItem = banners.size - 1
            }
        }

        findViewById<View>(R.id.btnBannerSiguiente)?.setOnClickListener {
            val current = bannerViewPager.currentItem
            if (current < banners.size - 1) {
                bannerViewPager.currentItem = current + 1
            } else {
                bannerViewPager.currentItem = 0
            }
        }
    }

    private fun setupBottomNav() {
        BottomNavHelper.setup(this, R.id.nav_inicio)
    }

    private fun cargarPeliculas() {
        lifecycleScope.launch {
               val peliculasActivas: List<Pelicula> = withContext(Dispatchers.IO) {
                try {
                    PeliculasRepository(this@InicioActivity)
                        .getPeliculasPorEstado("Activa")
                        .ifEmpty { fallbackActivas() }
                } catch (e: Exception) {
                    fallbackActivas()
                }
            }

            val peliculasProximas: List<Pelicula> = withContext(Dispatchers.IO) {
                try {
                    PeliculasRepository(this@InicioActivity)
                        .getPeliculasPorEstado("Inactiva")
                        .ifEmpty { fallbackProximas() }
                } catch (e: Exception) {
                    fallbackProximas()
                }
            }


            recyclerPeliculasInicio.adapter = PeliculasAdapter(peliculasActivas) { pelicula ->
                irADetalle(pelicula)
            }

            layoutProximamente.removeAllViews()
            for (pelicula in peliculasProximas) {
                val imageView = ImageView(this@InicioActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (130 * resources.displayMetrics.density).toInt(), // Ancho
                        (190 * resources.displayMetrics.density).toInt()  // Alto
                    ).apply {
                        setMargins(16, 0, 16, 0)
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP

                    val resId = drawableMap[pelicula.id] ?: R.drawable.ic_pelicula5 // Por defecto 5 para pruebas
                    setImageResource(resId)

                    setOnClickListener { irADetalle(pelicula) }
                }
                layoutProximamente.addView(imageView)
            }
        }
    }

    private fun irADetalle(pelicula: Pelicula) {
        val intent = Intent(this@InicioActivity, DetallePeliculaActivity::class.java).apply {
            putExtra("id_pelicula", pelicula.id)
            putExtra("titulo", pelicula.titulo)
            putExtra("duracion_min", pelicula.duracionMin)
            putExtra("clasificacion", pelicula.clasificacion)
            putExtra("genero", pelicula.genero)
            putExtra("formato", pelicula.formato)
            putExtra("sinopsis", pelicula.sinopsis)
            putExtra("estado", pelicula.estado)
        }
        startActivity(intent)
    }

    private fun setupDrawer() {

        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<View>(R.id.navCartelera)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<View>(R.id.navEntradas)?.setOnClickListener {
            startActivity(Intent(this, MisEntradasActivity::class.java))
        }

        findViewById<View>(R.id.navHistorial)?.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        findViewById<View>(R.id.navConfiteria)?.setOnClickListener {
            startActivity(Intent(this, ConfiteriaActivity::class.java))
        }

        findViewById<View>(R.id.navQR)?.setOnClickListener {
            startActivity(Intent(this, QRScannerActivity::class.java))
        }

        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            SessionManager(this).cerrarSesion()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        bannerHelper?.stop()
    }

    override fun onResume() {
        super.onResume()
        bannerHelper?.start()
    }


    private fun fallbackActivas() = listOf(
        Pelicula(id = 1, titulo = "Iron Lung: Océano de Sangre", anio = 2026, duracionMin = 108, clasificacion = "R", genero = "Terror", formato = "2D", estado = "Activa", drawableRes = R.drawable.ic_pelicula1),
        Pelicula(id = 2, titulo = "Espía Entre Animales", anio = 2026, duracionMin = 95, clasificacion = "G", genero = "Animación", formato = "3D", estado = "Activa", drawableRes = R.drawable.ic_pelicula2),
        Pelicula(id = 3, titulo = "Avengers: Doomsday", anio = 2026, duracionMin = 150, clasificacion = "PG-13", genero = "Acción", formato = "XD", estado = "Activa", drawableRes = R.drawable.ic_pelicula3),
        Pelicula(id = 4, titulo = "Minecraft: La Película", anio = 2026, duracionMin = 110, clasificacion = "PG", genero = "Aventura", formato = "2D", estado = "Activa", drawableRes = R.drawable.ic_pelicula4),

        )

    private fun fallbackProximas() = listOf(
        Pelicula(id = 5, titulo = "Superman: Legacy", anio = 2026, duracionMin = 130, clasificacion = "PG-13", genero = "Acción", formato = "XD", estado = "Inactiva", drawableRes = R.drawable.ic_pelicula5),
        Pelicula(id = 6, titulo = "Jurassic World: Renacimiento", anio = 2026, duracionMin = 125, clasificacion = "PG-13", genero = "Aventura", formato = "3D", estado = "Inactiva", drawableRes = R.drawable.ic_pelicula6)
    )
}