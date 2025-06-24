package com.example.mapboxdemo2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.mapboxdemo2.R
import com.example.mapboxdemo2.adapters.MarkerSettingsAdapter
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.mapboxdemo2.data.db.AppDatabase
import com.example.mapboxdemo2.date.model.DownloadedMarker
import java.util.*

class MarkerSettingsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MarkerSettingsAdapter
    private val markerSettingsList = mutableListOf<DownloadedMarker>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_marker_settings_list, container, false)
        recyclerView = view.findViewById(R.id.markerSettingsRecyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MarkerSettingsAdapter(markerSettingsList) { item, isChecked ->
            // ON/OFFトグルのコールバック例
            item.isVisible = isChecked
            // DBにも反映させる場合は下記のようにする（例: 非同期で更新）
            // viewLifecycleOwner.lifecycleScope.launch {
            //     AppDatabase.getDatabase(requireContext()).downloadedMarkerDao().update(item)
            // }
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // DBからマーカー一覧を取得しリストに反映
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val allMarkers = db.downloadedMarkerDao().getAll()
            markerSettingsList.clear()
            markerSettingsList.addAll(allMarkers)
            adapter.notifyDataSetChanged()
        }

        // 並び替え（ドラッグ＆ドロップ）対応
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                Collections.swap(markerSettingsList, fromPos, toPos)
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // スワイプ削除は未対応
            }
            // ロングタップ以外でドラッグを開始したい場合はここでhandle利用（後述）
            override fun isLongPressDragEnabled() = true
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}