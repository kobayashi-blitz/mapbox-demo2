package com.example.mapboxdemo2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapboxdemo2.R
import com.example.mapboxdemo2.model.MarkerSetting

class MarkerSettingsAdapter(
    private val items: MutableList<MarkerSetting>,
    private val onCheckedChange: (MarkerSetting, Boolean) -> Unit
) : RecyclerView.Adapter<MarkerSettingsAdapter.VH>() {

    private var onItemClickListener: ((MarkerSetting) -> Unit)? = null

    private var dragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null
    fun setOnItemClickListener(listener: (MarkerSetting) -> Unit) {
        onItemClickListener = listener
    }
    fun setDragStartListener(listener: (RecyclerView.ViewHolder) -> Unit) {
        dragStartListener = listener
    }

    fun swap(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) return
        val item = items.removeAt(fromPosition)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.markerThumbnail)
        val name: TextView = view.findViewById(R.id.markerName)
        val toggle: Switch = view.findViewById(R.id.markerToggle)
        val dragHandle: ImageView? = view.findViewById(R.id.dragHandle)

        init {
            dragHandle?.setOnLongClickListener {
                dragStartListener?.invoke(this@VH)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_marker_setting, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.thumbnail.setImageResource(item.iconRes)
        holder.name.text = item.name
        holder.toggle.isChecked = item.enabled
        holder.toggle.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(item, isChecked)
        }

        // 並び替え用ハンドルの取得
        val dragHandle = holder.itemView.findViewById<ImageView?>(R.id.dragHandle)

        // 詳細ダイアログ用クリックリスナー（ハンドル・トグル以外のみ発火）
        holder.itemView.setOnClickListener { v ->
            val isToggle = v.id == holder.toggle.id
            val isHandle = dragHandle != null && v.id == dragHandle.id
            if (!isToggle && !isHandle) {
                onItemClickListener?.invoke(item)
            }
        }

        // トグルスイッチはON/OFFのみ反応、詳細は無視
        holder.toggle.setOnClickListener {
            // 何もしない（ON/OFFのみに反応、詳細表示は無視）
        }
        // 並び替えハンドルも詳細表示は無視
        dragHandle?.setOnClickListener {
            // 並び替え専用。詳細表示は無視
        }
    }

    override fun getItemCount() = items.size
}