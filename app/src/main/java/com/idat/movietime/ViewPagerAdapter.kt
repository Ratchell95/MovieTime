package com.idat.movietime

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val tabTitles = listOf("cartelera", "proximamente", "opera")

    override fun getItemCount(): Int = tabTitles.size

    override fun createFragment(position: Int): Fragment {
        return PeliculasTabFragment.newInstance(tabTitles[position])
    }
}