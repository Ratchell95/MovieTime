package com.idat.movietime

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.GravityCompat
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PeliculasActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peliculas)

        drawerLayout = findViewById(R.id.drawerLayout)


        setupTabs()
        setupBottomNav()
        setupDrawer()
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = listOf("Cartelera", "Próximamente", "Ópera / Preventa")[pos]
        }.attach()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_peliculas
        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_inicio     -> { startActivity(Intent(this, InicioActivity::class.java)); true }
                R.id.nav_peliculas  -> true
                R.id.nav_cines      -> { startActivity(Intent(this, CinesActivity::class.java)); true }
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
        findViewById<View>(R.id.navCartelera)?.setOnClickListener   { drawerLayout.closeDrawer(GravityCompat.START) }

        findViewById<View>(R.id.navEntradas)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // DEBE DECIR MisEntradasActivity::class.java
            startActivity(Intent(this, MisEntradasActivity::class.java))
        }
        findViewById<View>(R.id.navConfiteria)?.setOnClickListener  { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, ConfiteriaActivity::class.java)) }
        findViewById<View>(R.id.navHistorial)?.setOnClickListener   { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, HistorialActivity::class.java)) }
        findViewById<View>(R.id.navQR)?.setOnClickListener         { drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, QRScannerActivity::class.java)) }


        findViewById<View>(R.id.navCerrarSesion)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            com.idat.movietime.network.SessionManager(this).cerrarSesion()

            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}