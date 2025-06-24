package com.example.mapboxdemo2.date.db

import android.content.Context
import com.example.mapboxdemo2.data.db.AppDatabase
import com.example.mapboxdemo2.date.model.DownloadedMarker
import com.example.mapboxdemo2.date.model.MarkerCategory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun insertTestMarkers(context: Context) {
    val db = AppDatabase.getDatabase(context)
    GlobalScope.launch(Dispatchers.IO) {
        // 既存データをチェック
        if (db.downloadedMarkerDao().getAll().isEmpty()) {
            val categories = MarkerCategory.values()
            val testData = (1..10).map { i ->
                DownloadedMarker(
                    name = "テストマーカー$i",
                    filePath = "ma_dl_%03d.png".format(i),
                    category = categories[(i - 1) % categories.size],
                    isVisible = true,
                    displayOrder = i,
                    memo = "テストデータ $i",
                    downloadedAt = System.currentTimeMillis()
                )
            }
            db.downloadedMarkerDao().insertAll(testData)
        }
    }
}