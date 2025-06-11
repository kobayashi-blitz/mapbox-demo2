package com.example.mapboxdemo2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    var splashAlreadyShown: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 起動時は MapFragment を表示
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MapFragment())
            .commit()

        // BottomNavigationView のタブ切り替え設定
        val navView: BottomNavigationView = findViewById(R.id.bottomNavigation)
        navView.setOnNavigationItemSelectedListener { item ->
            // すべてのアイコンを元の大きさに戻す
            for (i in 0 until navView.menu.size()) {
                val menuItem = navView.menu.getItem(i)
                val iconView = navView.findViewById<android.view.View>(menuItem.itemId)
                iconView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(100)?.start()
            }
            // 選択されたアイコンだけ拡大
            val selectedIconView = navView.findViewById<android.view.View>(item.itemId)
            selectedIconView?.animate()?.scaleX(1.3f)?.scaleY(1.3f)?.setDuration(150)?.start()

            val fragment = when (item.itemId) {
                R.id.nav_map        -> MapFragment()
                R.id.nav_markerdl   -> MarkerDownloadFragment()
                R.id.nav_settings   -> MarkerSettingsFragment()
                R.id.nav_notice     -> NoticeFragment()
                R.id.nav_mypage     -> MyPageFragment()
                else                -> null
            }
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, it)
                    .commit()
                true
            } ?: false
        }
    }
}