package com.example.mapboxdemo2.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.mapboxdemo2.R
import com.example.mapboxdemo2.date.model.NotificationItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotificationDetailDialog(
    private val item: NotificationItem
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_notification_detail, container, false)
        view.findViewById<TextView>(R.id.detailTitle).text = item.title
        view.findViewById<TextView>(R.id.detailMessage).text = item.message
        view.findViewById<TextView>(R.id.detailDate).text = item.date
        return view
    }

    companion object {
        fun show(fragmentManager: androidx.fragment.app.FragmentManager, item: NotificationItem) {
            NotificationDetailDialog(item).show(fragmentManager, "NotificationDetailDialog")
        }
    }
}