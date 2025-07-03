package com.example.mapboxdemo2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapboxdemo2.R
import com.example.mapboxdemo2.adapters.MarkerSettingsAdapter
import com.example.mapboxdemo2.date.model.DownloadedMarker
import com.example.mapboxdemo2.data.db.AppDatabase
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import com.example.mapboxdemo2.model.MarkerData
import android.widget.Toast

import android.util.Log

class RegistMarkerSettingsListFragment : Fragment() {
    private lateinit var adapter: MarkerSettingsAdapter
    private var markerList: MutableList<DownloadedMarker> = mutableListOf()
    private var originalMarkerDataList = listOf<MarkerData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_marker_settings_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.markerSettingsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = MarkerSettingsAdapter(markerList) { item, isChecked ->

            // スイッチをONにしようとした時の上限チェック
            if (isChecked && markerList.count { it.isVisible } >= 8) {
                Toast.makeText(requireContext(), "表示できるマーカーは8個までです", Toast.LENGTH_SHORT).show()
                // アダプターに再描画を指示し、スイッチの見た目を強制的に元に戻す
                adapter.notifyItemChanged(markerList.indexOf(item))
                return@MarkerSettingsAdapter // ★ DB更新処理を行わずに、ここで処理を終了する
            }

            // スイッチをOFFにしようとした時の下限チェック
            if (!isChecked && markerList.count { it.isVisible } <= 1) {
                Toast.makeText(requireContext(), "最低1つはマーカーを表示する必要があります", Toast.LENGTH_SHORT).show()
                adapter.notifyItemChanged(markerList.indexOf(item)) // スイッチを元に戻す
                return@MarkerSettingsAdapter // ★ DB更新処理を行わずに、ここで処理を終了する
            }

            // バリデーションに問題がなければ、DBとリストを更新する
            viewLifecycleOwner.lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                val updated = item.copy(isVisible = isChecked)
                db.downloadedMarkerDao().update(updated)
                val idx = markerList.indexOfFirst { it.id == item.id }
                if (idx != -1) {
                    markerList[idx] = updated
                }
            }
        }

        adapter.setOnItemClickListener { item ->
            showMarkerDetailDialog(item)
        }
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val data = db.downloadedMarkerDao().getAll()
            markerList.clear()
            markerList.addAll(data)
            adapter.notifyDataSetChanged()
        }

        // RegistMarkerSettingsListFragment.kt

        adapter.onItemOrderChanged = { newList ->
            viewLifecycleOwner.lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                val updatedList = newList.mapIndexed { index, item -> item.copy(displayOrder = index) }

                db.downloadedMarkerDao().updateAll(updatedList)
                // markerListの順序をupdatedListに合わせて上書き（リスト再生成でなく順番入れ替え推奨）
                for (i in updatedList.indices) {
                    markerList[i] = updatedList[i]
                }
                adapter.notifyDataSetChanged()
            }
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                viewHolder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                java.util.Collections.swap(markerList, fromPos, toPos)
                adapter.notifyItemMoved(fromPos, toPos)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe action
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // ドラッグ終了時にDBの並び順を更新
                updateMarkerOrder()
            }

        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        adapter.setDragStartListener { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }
    }

    private fun showMarkerDetailDialog(item: DownloadedMarker) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_marker_detail, null)
        view.findViewById<TextView>(R.id.markerNameTextView).text = item.name
        view.findViewById<TextView>(R.id.markerIdTextView).text = "ID: ${item.id}"
        view.findViewById<TextView>(R.id.markerEnabledTextView).text = if (item.isVisible) "ON" else "OFF"
        dialog.setContentView(view)
        dialog.show()
    }

    private fun updateMarkerOrder() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            // 新しい順番をdisplayOrderプロパティに再設定
            val updatedList = markerList.mapIndexed { index, item ->
                item.copy(displayOrder = index)
            }
            db.downloadedMarkerDao().updateAll(updatedList)
            updateViewModel(updatedList)
        }
    }

    /**
     * 現在のリストの状態をViewModelに通知するメソッド
     */
    private fun updateViewModel(currentList: List<DownloadedMarker>) {
        // ViewModelの処理は後で実装するため、一旦コメントアウト
        // val wheelMarkers = currentList.filter { it.isVisible }.take(8)
        // sharedViewModel.wheelMarkersLiveData.postValue(wheelMarkers)
    }

}
