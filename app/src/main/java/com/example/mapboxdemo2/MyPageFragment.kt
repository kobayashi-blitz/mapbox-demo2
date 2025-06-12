package com.example.mapboxdemo2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mypage, container, false)
        val btn = view.findViewById<Button>(R.id.button_dummy_mypage)
        btn.setOnClickListener {
            Toast.makeText(requireContext(), "ここでマイページ操作👤", Toast.LENGTH_SHORT).show()
        }
        return view
    }
}