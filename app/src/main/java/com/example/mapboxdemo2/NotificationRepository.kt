//package com.example.mapboxdemo2
//
//import com.example.mapboxdemo2.adapters.Notification
//
//object NotificationRepository {
//
//    /**
//     * ダミーの通知データを非同期で返す例
//     * コールバック: (List<Notification>) -> Unit
//     */
//    fun fetchLatest(callback: (List<Notification>) -> Unit) {
//        // 疑似的に「1秒後にダミー通知リストを返す」ように Handler を使う例
//        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
//            val dummyList = listOf(
//                Notification("ようこそ!", "アプリへようこそ！最初の通知です😊"),
//                Notification("イベント案内", "今週末にイベントがあります。詳細はこちらから。")
//            )
//            callback(dummyList)
//        }, 1000)
//    }
//}