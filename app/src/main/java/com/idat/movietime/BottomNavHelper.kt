package com.idat.movietime

import android.content.Intent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavHelper {

    fun setup(activity: AppCompatActivity, itemActivo: Int) {

        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            ?: return

        // 1. Marcar ítem activo
        bottomNav.selectedItemId = itemActivo

        // 2. Poner el título del ítem activo en el toolbar
        //    Lo leemos directamente del menú para no duplicar los nombres
        val titulo = bottomNav.menu.findItem(itemActivo)?.title?.toString() ?: ""
        activity.findViewById<TextView>(R.id.tvToolbarTitulo)?.text = titulo

        // 3. Navegar al tocar otro ítem y actualizar el título
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == itemActivo) return@setOnItemSelectedListener true

            // Actualizar título antes de navegar
            activity.findViewById<TextView>(R.id.tvToolbarTitulo)?.text =
                item.title?.toString() ?: ""

            when (item.itemId) {
                R.id.nav_inicio     -> activity.startActivity(Intent(activity, InicioActivity::class.java))
                R.id.nav_peliculas  -> activity.startActivity(Intent(activity, PeliculasActivity::class.java))
                R.id.nav_cines      -> activity.startActivity(Intent(activity, CinesActivity::class.java))
                R.id.nav_confiteria -> activity.startActivity(Intent(activity, ConfiteriaActivity::class.java))
            }
            true
        }
    }
}