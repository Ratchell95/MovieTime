package com.idat.movietime

import android.os.Handler
import android.os.Looper
import androidx.viewpager2.widget.ViewPager2

class BannerHelper(
    private val viewPager: ViewPager2,
    private val count: Int,
    private val intervalMs: Long = 4000L
) {
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable { siguiente() }

    fun start() {
        if (count > 1) handler.postDelayed(runnable, intervalMs)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
    }

    private fun siguiente() {
        val next = (viewPager.currentItem + 1) % count
        viewPager.setCurrentItem(next, true)
        handler.postDelayed(runnable, intervalMs)
    }
}
