package com.example.mapboxdemo2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapboxdemo2.R
import com.example.mapboxdemo2.date.model.MarkerDownloadItem

class MarkerDownloadAdapter(
    private val items: List<MarkerDownloadItem>
) : RecyclerView.Adapter<MarkerDownloadAdapter.VH>() {

    private var onItemClickListener: ((MarkerDownloadItem) -> Unit)? = null

    fun setOnItemClickListener(listener: (MarkerDownloadItem) -> Unit) {
        onItemClickListener = listener
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.markerThumbnail)
        val name: TextView = view.findViewById(R.id.markerName)
        val category: TextView = view.findViewById(R.id.markerCategory)

        init {
            view.setOnClickListener {
                val item = items[adapterPosition]
                onItemClickListener?.invoke(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_marker_download, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.thumbnail.setImageResource(item.thumbnailRes)
        holder.name.text = item.name
        holder.category.text = "カテゴリ: ${item.category}"
    }

    override fun getItemCount() = items.size
}
