package com.example.mapboxdemo2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapboxdemo2.R
import com.example.mapboxdemo2.date.model.NotificationItem
import com.example.mapboxdemo2.date.model.NotificationType

class NoticeAdapter(
    private val type: NotificationType,
    private val onClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NoticeAdapter.NotificationViewHolder>() {

    private var items: MutableList<NotificationItem> = mutableListOf()

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val notificationTitle: TextView = view.findViewById(R.id.notificationTitle)
        val notificationMessage: TextView = view.findViewById(R.id.notificationMessage)
        val notificationDate: TextView = view.findViewById(R.id.notificationDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(v)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]
        holder.notificationTitle.text = item.title
        holder.notificationMessage.text = item.message
        holder.notificationDate.text = item.date

        // 必要ならtypeでデザイン変更（例：色分けなど）
        // if (item.type == NotificationType.MARKER) {...}

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    // submitListにリネーム
    fun submitList(newItems: List<NotificationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}