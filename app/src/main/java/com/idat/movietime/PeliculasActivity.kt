package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PeliculasActivity : AppCompatActivity() {

    private lateinit var bannerViewPager:   ViewPager2
    private lateinit var layoutIndicadores: LinearLayout
    private lateinit var drawerLayout:      DrawerLayout
    private var bannerHelper: BannerHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peliculas)

        drawerLayout      = findViewById(R.id.drawerLayout)
        bannerViewPager   = findViewById(R.id.bannerViewPager)
        layoutIndicadores = findViewById(R.id.layoutIndicadores)

        setupBanners()
        setupTabsPeliculas()
        setupBottomNav()
        setupDrawer()
    }

    private fun setupBanners() {
        val banners = listOf<Any>(R.drawable.ic_banner1, R.drawable.ic_banner2)
        bannerViewPager.adapter = BannerAdapter(banners)
        actualizarIndicadores(banners.size, 0)
        bannerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = actualizarIndicadores(banners.size, position)
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
                val size   = (8 * density).toInt()
                val margin = (4 * density).toInt()
                layoutParams = LayoutParams(size, size).apply { setMargins(margin, 0, margin, 0) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (i == activo) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#88FFFFFF"))
                }
            }
            layoutIndicadores.addView(dot)
        }
    }

    private fun setupTabsPeliculas() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = listOf("Cartelera", "Próximamente", "Ópera / Preventa")[pos]
        }.attach()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_inicio,
                R.id.nav_peliculas  -> true
                R.id.nav_cines      -> { startActivity(Intent(this, CinesActivity::class.java)); true }
                R.id.nav_confiteria -> {

                    startActivity(Intent(this, ConfiteriaActivity::class.java)); true
                }
                else -> false
            }
        }
    }

    private fun setupDrawer() {
        // Botón hamburguesa
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            if (drawerLayout.isDrawerOpen(Gravity.START))
                drawerLayout.closeDrawer(Gravity.START)
            else
                drawerLayout.openDrawer(Gravity.START)
        }

        findViewById<View>(R.id.navCartelera)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
        }

        findViewById<View>(R.id.navEntradas)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            startActivity(Intent(this, HistorialActivity::class.java))
        }
        findViewById<View>(R.id.navConfiteria)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            startActivity(Intent(this, ConfiteriaActivity::class.java))
        }

        findViewById<View>(R.id.navHistorial)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        findViewById<View>(R.id.navQR)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            startActivity(Intent(this, QRScannerActivity::class.java))
        }


        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.START)
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerHelper?.stop()
    }
}