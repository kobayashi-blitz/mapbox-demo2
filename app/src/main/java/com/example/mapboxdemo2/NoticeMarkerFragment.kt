package com.example.mapboxdemo2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapboxdemo2.R
import com.example.mapboxdemo2.adapters.NoticeAdapter
import com.example.mapboxdemo2.model.NotificationItem
import com.example.mapboxdemo2.model.NotificationType

class NoticeMarkerFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyLabel: TextView
    private lateinit var adapter: NoticeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notice_tab_marker, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewMarkerNotifications)
        emptyLabel = view.findViewById(R.id.emptyLabelMarker)

        adapter = NoticeAdapter(NotificationType.MARKER) { notificationItem ->
            // ここで詳細ダイアログ表示など
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadMarkerNotifications()
        return view
    }

    private fun loadMarkerNotifications() {
        // 仮のダミーデータ
        val dummyList = listOf(
            NotificationItem(
                title = "友達が新しい場所を登録しました",
                message = "〇〇さんが新しい場所を追加しました！",
                date = "2024/06/10 10:30",
                type = NotificationType.MARKER
            ),
            NotificationItem(
                title = "近くのマーカーが更新されました",
                message = "△△さんが情報を更新しました。",
                date = "2024/06/09 14:18",
                type = NotificationType.MARKER
            )
        )

        adapter.submitList(dummyList)
        emptyLabel.visibility = if (dummyList.isEmpty()) View.VISIBLE else View.GONE
    }
}