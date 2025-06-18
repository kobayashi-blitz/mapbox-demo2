package com.example.mapboxdemo2.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.fragment.app.FragmentActivity
import com.example.mapboxdemo2.fragments.RegistMarkerSettingsListFragment
import com.example.mapboxdemo2.fragments.SearchMarkerSettingsListFragment
import com.example.mapboxdemo2.fragments.FunctionMarkerSettingsListFragment

class MarkerSettingsTabPagerAdapter(
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {

    // タブ数（登録・検索・機能）
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RegistMarkerSettingsListFragment()
            1 -> SearchMarkerSettingsListFragment()
            2 -> FunctionMarkerSettingsListFragment()
            else -> throw IllegalArgumentException("不正なタブポジション: $position")
        }
    }
}