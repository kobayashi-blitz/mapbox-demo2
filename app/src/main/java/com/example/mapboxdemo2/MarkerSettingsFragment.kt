package com.example.mapboxdemo2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.mapboxdemo2.adapters.MarkerSettingsTabPagerAdapter

class MarkerSettingsFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_marker_settings, container, false)
        tabLayout = view.findViewById(R.id.marker_settings_tab_layout)
        viewPager = view.findViewById(R.id.marker_settings_view_pager)

        // ViewPager2用のAdapterをセット
        val adapter = MarkerSettingsTabPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        // TabLayoutとViewPager2の連携
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "登録マーカー"
                1 -> "検索マーカー"
                2 -> "機能マーカー"
                else -> ""
            }
        }.attach()

        return view
    }
}