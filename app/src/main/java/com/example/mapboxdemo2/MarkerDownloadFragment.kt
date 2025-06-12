package com.example.mapboxdemo2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class MarkerDownloadFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_marker_download, container, false)
        val btn = view.findViewById<Button>(R.id.button_dummy_download)
        btn.setOnClickListener {
            Toast.makeText(requireContext(), "ここでマーカーDL処理を実装します😊", Toast.LENGTH_SHORT).show()
        }
        return view
    }
}