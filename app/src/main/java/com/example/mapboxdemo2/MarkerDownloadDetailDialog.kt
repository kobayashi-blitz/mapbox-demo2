package com.example.mapboxdemo2

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.mapboxdemo2.model.MarkerDownloadItem

class MarkerDownloadDetailDialog(
    private val markerItem: MarkerDownloadItem,
    private val onDownload: ((MarkerDownloadItem) -> Unit)? = null
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val dialog = super.onCreateDialog(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_marker_download_detail, null)

        // サムネイル画像・マーカー名・カテゴリ名
        view.findViewById<ImageView>(R.id.markerThumbnail).setImageResource(markerItem.thumbnailRes)
        view.findViewById<TextView>(R.id.markerName).text = markerItem.name
        view.findViewById<TextView>(R.id.markerCategory).text = "カテゴリ: ${markerItem.category}"

        // ダウンロードボタン
        view.findViewById<Button>(R.id.downloadButton).setOnClickListener {
            onDownload?.invoke(markerItem)
            dismiss()
        }

        dialog.setContentView(view)
        return dialog
    }
}