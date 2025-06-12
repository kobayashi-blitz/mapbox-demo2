package com.example.mapboxdemo2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapboxdemo2.R
import com.example.mapboxdemo2.adapters.NoticeAdapter
import com.example.mapboxdemo2.model.NotificationItem
import com.example.mapboxdemo2.model.NotificationType

class NoticeAdminFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoticeAdapter
    private lateinit var emptyLabel: TextView
    private lateinit var headerImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notice_tab_admin, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewAdminNotifications)
        emptyLabel = view.findViewById(R.id.emptyLabelAdmin)

        // 仮のダミーデータ
        val dummyNewsList = listOf(
            NotificationItem(
                title = "新機能リリースのお知らせ",
                message = "地図アプリに新機能が追加されました！",
                date = "2025-06-12",
                type = NotificationType.NEWS
            ),
            NotificationItem(
                title = "メンテナンスのお知らせ",
                message = "6/20深夜にシステムメンテナンスを実施します。",
                date = "2025-06-10",
                type = NotificationType.NEWS
            )
        )

        adapter = NoticeAdapter(NotificationType.NEWS) { notificationItem ->
            // タップ時の詳細表示（後で実装）
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        adapter.submitList(dummyNewsList)
        emptyLabel.visibility = if (dummyNewsList.isEmpty()) View.VISIBLE else View.GONE

        // ↓ここ（ヘッダー画像）は不要なので削除
        // headerImage = view.findViewById(R.id.adminHeaderImage)
        // headerImage.setImageResource(R.drawable.news_header)

        return view
    }
}