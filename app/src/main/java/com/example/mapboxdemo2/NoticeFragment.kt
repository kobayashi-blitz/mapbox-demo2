package com.example.mapboxdemo2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.example.mapboxdemo2.adapters.NoticeTabPagerAdapter

class NoticeFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notice, container, false)
        tabLayout = view.findViewById(R.id.noticeTabLayout)
        viewPager = view.findViewById(R.id.noticeViewPager)

        // PagerAdapterをセット
        val adapter = NoticeTabPagerAdapter(this)
        viewPager.adapter = adapter

        // TabLayoutとViewPager2を連携
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "お知らせ"
                1 -> "マーカー通知"
                else -> ""
            }
        }.attach()

        return view
    }
}