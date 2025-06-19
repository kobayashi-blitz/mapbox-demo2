package com.example.mapboxdemo2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    var splashAlreadyShown: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 起動時は MapFragment を表示
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MapFragment())
            .commit()

        // in MainActivity.kt / onCreate()

        // BottomNavigationView のタブ切り替え設定
        val navView: BottomNavigationView = findViewById(R.id.bottomNavigation)

        // ↓↓↓↓ 既存の setOnNavigationItemSelectedListener を、以下の setOnItemSelectedListener に置き換えてください ↓↓↓↓

        navView.setOnItemSelectedListener { item ->
            // 現在表示されているFragmentを取得します
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

            // 【条件】
            // 1. 表示中の画面がマップ（MapFragment）で、
            // 2. ナビゲーションの実行中で、
            // 3. これから押されたボタンがマップ以外である
            if (currentFragment is MapFragment && currentFragment.isNavigating && item.itemId != R.id.nav_map) {
                // 条件に一致した場合、確認ダイアログを表示します
                AlertDialog.Builder(this)
                    .setTitle("ナビゲーションの終了")
                    .setMessage("画面を切り替えるとナビゲーションが終了します。よろしいですか？")
                    .setPositiveButton("はい") { dialog, which -> // ★修正点：引数を明確にしました
                        // 「はい」が押されたら、ナビゲーションを停止します
                        currentFragment.stopNavigation()
                        // その後、改めてユーザーが選択した画面への遷移を「実行」します
                        item.isChecked = true
                        performFragmentTransaction(item.itemId)
                    }
                    .setNegativeButton("いいえ", null)
                    .show()

                return@setOnItemSelectedListener false
            } else {
                // 通常時はこちらが実行されます
                item.isChecked = true
                performFragmentTransaction(item.itemId)
                return@setOnItemSelectedListener true
            }
        }
    }

    // Fragmentを切り替える処理を別のメソッドに分離します
    private fun performFragmentTransaction(itemId: Int) {
        // アイコンのアニメーション (この部分は元のコードと同じです)
        val navView: BottomNavigationView = findViewById(R.id.bottomNavigation)
        for (i in 0 until navView.menu.size()) {
            val menuItem = navView.menu.getItem(i)
            val iconView = navView.findViewById<android.view.View>(menuItem.itemId)
            iconView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(100)?.start()
        }
        val selectedIconView = navView.findViewById<android.view.View>(itemId)
        selectedIconView?.animate()?.scaleX(1.3f)?.scaleY(1.3f)?.setDuration(150)?.start()

        // Fragmentの切り替え
        val fragment = when (itemId) {
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
        }
    }
}