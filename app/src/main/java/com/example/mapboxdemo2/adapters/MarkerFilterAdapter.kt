package com.example.mapboxdemo2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapboxdemo2.R

class MarkerFilterAdapter(
    private val markerImages: List<String>, // filePathリスト
    private val selectedList: MutableSet<String>,
    private val onCheckedChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<MarkerFilterAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val markerImage: ImageView = view.findViewById(R.id.markerImageView)
        val checkBox: CheckBox = view.findViewById(R.id.markerCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marker_filter, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = markerImages.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filePath = markerImages[position]
        val context = holder.markerImage.context
        val resId = context.resources.getIdentifier(filePath.removeSuffix(".png"), "drawable", context.packageName)
        holder.markerImage.setImageResource(resId)

        // setOnCheckedChangeListenerを一度外してから状態セット
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedList.contains(filePath)

        // リスナー再セット（外部Setを同期）
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedList.add(filePath)
            } else {
                selectedList.remove(filePath)
            }
            // 新しいSetでコールバック（.toSet()で不変化して渡す）
            onCheckedChanged(selectedList.toSet())
        }

        // 画像タップでもチェック切り替え
        holder.markerImage.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }
}