package com.example.mapboxdemo2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.mapboxdemo2.adapters.MarkerDownloadAdapter
import com.example.mapboxdemo2.date.model.MarkerDownloadItem

class MarkerDownloadFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_marker_download, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMarkerDownload)
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)

        // ダミーデータ作成
        val dummyList = listOf(
            MarkerDownloadItem(R.drawable.marker_rsearch_facilityicon01, "赤いマーカー", "登録"),
            MarkerDownloadItem(R.drawable.marker_rsearch_facilityicon01, "青いマーカー", "検索"),
            MarkerDownloadItem(R.drawable.marker_rsearch_facilityicon01, "ナビマーカー", "機能"),
        )
        val adapter = MarkerDownloadAdapter(dummyList)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener { markerItem ->
            MarkerDownloadDetailDialog(markerItem) {
                // ダウンロード処理（ダミー：トースト表示）
                Toast.makeText(requireContext(), "${it.name} をダウンロードしました", Toast.LENGTH_SHORT).show()
            }.show(parentFragmentManager, "markerDetail")
        }

        return view
    }
}