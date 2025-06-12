package com.example.mapboxdemo2.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mapboxdemo2.fragments.NoticeAdminFragment
import com.example.mapboxdemo2.fragments.NoticeMarkerFragment

class NoticeTabPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NoticeAdminFragment()
            1 -> NoticeMarkerFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}