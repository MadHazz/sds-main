package com.DevCiplak.advdisplay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.DevCiplak.advdisplay.R
import com.DevCiplak.advdisplay.data.PlaylistCache
import com.DevCiplak.advdisplay.model.MediaPage
import com.bumptech.glide.Glide

class SliderAdapter(private val pages: List<MediaPage>, private val cache: PlaylistCache) : RecyclerView.Adapter<SliderAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.slider_item, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.images.forEach { Glide.with(it).clear(it); it.setImageDrawable(null) }
        val page = pages[position]
        val panels = page.templateId == "3"
        holder.single.visibility = if (panels) View.GONE else View.VISIBLE
        holder.panels.visibility = if (panels) View.VISIBLE else View.GONE
        page.media.forEach { item ->
            val target = if (!panels) holder.single else when (item.slot) {
                "1" -> holder.left
                "2" -> holder.top
                else -> holder.right
            }
            Glide.with(target).load(cache.fileFor(item)).into(target)
        }
    }

    override fun onViewRecycled(holder: Holder) {
        holder.images.forEach { Glide.with(it).clear(it) }
    }

    override fun getItemCount() = pages.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val single: ImageView = view.findViewById(R.id.image_view)
        val top: ImageView = view.findViewById(R.id.topImg)
        val left: ImageView = view.findViewById(R.id.leftImg)
        val right: ImageView = view.findViewById(R.id.rightImg)
        val panels: View = view.findViewById(R.id.dmb)
        val images = listOf(single, top, left, right)
    }
}
