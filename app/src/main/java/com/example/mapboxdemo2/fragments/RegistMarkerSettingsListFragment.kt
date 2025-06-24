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

class RegistMarkerSettingsListFragment : Fragment() {
    private lateinit var adapter: MarkerSettingsAdapter
    private var markerList: MutableList<DownloadedMarker> = mutableListOf()

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
            // 切り替えイベント仮対応
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
                if (fromPos < toPos) {
                    for (i in fromPos until toPos) {
                        val tmp = markerList[i]
                        markerList[i] = markerList[i + 1]
                        markerList[i + 1] = tmp
                    }
                } else {
                    for (i in fromPos downTo toPos + 1) {
                        val tmp = markerList[i]
                        markerList[i] = markerList[i - 1]
                        markerList[i - 1] = tmp
                    }
                }
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe action
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
}
