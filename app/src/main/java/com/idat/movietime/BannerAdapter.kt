package com.idat.movietime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BannerAdapter(private val banners: List<Any>) :
    RecyclerView.Adapter<BannerAdapter.BannerVH>() {

    inner class BannerVH(v: View) : RecyclerView.ViewHolder(v) {
        val imageView: ImageView = v.findViewById(R.id.ivBanner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
        BannerVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_banner, parent, false)
        )

    override fun onBindViewHolder(h: BannerVH, pos: Int) {
        when (val item = banners[pos]) {
            is Int    -> h.imageView.setImageResource(item)
            is String -> Glide.with(h.imageView)
                .load(item)
                .centerCrop()
                .into(h.imageView)
        }
    }

    override fun getItemCount() = banners.size
}
