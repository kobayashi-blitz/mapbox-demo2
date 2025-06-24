package com.example.mapboxdemo2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mapboxdemo2.model.MarkerData
import com.example.mapboxdemo2.date.model.DownloadedMarker
import com.example.mapboxdemo2.date.db.MarkerCategoryConverter
import androidx.room.TypeConverters

@TypeConverters(MarkerCategoryConverter::class)
@Database(entities = [MarkerData::class, DownloadedMarker::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // このデータベースがどのDAO（命令書）を持つかを定義
    abstract fun markerDao(): MarkerDao
    abstract fun downloadedMarkerDao(): DownloadedMarkerDao

    // アプリ全体でデータベースのインスタンスが一つだけになるようにするお決まりの書き方
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marker_database"
                )
                    .fallbackToDestructiveMigration() // ← todo:強制的にDBを作り直す
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}