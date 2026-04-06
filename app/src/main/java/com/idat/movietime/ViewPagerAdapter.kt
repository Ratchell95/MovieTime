package com.idat.movietime

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    /**
     * FIX: los valores deben coincidir exactamente con los que usa
     * PeliculasTabFragment para filtrar la columna `estado` de la tabla `peliculas`.
     *
     * Valores válidos según el CHECK del esquema: 'Activa' | 'Inactiva'
     *
     * Si el fragment filtra por texto libre (p.ej. "cartelera", "proximamente")
     * en lugar de por `estado`, ajusta estos valores según esa lógica.
     * El tercer tab "opera" no existe en el esquema — se corrige a "Inactiva"
     * (próximas/pre-estreno). Renombra según la lógica real de tu app.
     */
    private val tabTitles = listOf(
        "cartelera",
        "proximamente",
        "opera"
    )


    override fun getItemCount(): Int = tabTitles.size

    override fun createFragment(position: Int): Fragment {
        return PeliculasTabFragment.newInstance(tabTitles[position])
    }
}