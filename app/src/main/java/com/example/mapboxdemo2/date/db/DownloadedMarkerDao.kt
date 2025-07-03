package com.example.mapboxdemo2.data.db

import androidx.room.*
import com.example.mapboxdemo2.date.model.DownloadedMarker
@Dao
interface DownloadedMarkerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(marker: DownloadedMarker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(markers: List<DownloadedMarker>)

    @Query("SELECT * FROM downloaded_markers ORDER BY displayOrder ASC")
    suspend fun getAll(): List<DownloadedMarker>

    @Query("SELECT * FROM downloaded_markers WHERE isVisible = 1 ORDER BY displayOrder ASC")
    suspend fun getVisible(): List<DownloadedMarker>

    @Update
    suspend fun update(marker: DownloadedMarker)

    @Delete
    suspend fun delete(marker: DownloadedMarker)

    @Query("SELECT * FROM downloaded_markers WHERE category = 0 ORDER BY displayOrder ASC LIMIT :limit")
    suspend fun getLatestMarkers(limit: Int): List<DownloadedMarker>

    // 必要に応じてカテゴリ検索
    @Query("SELECT * FROM downloaded_markers WHERE category = :category ORDER BY displayOrder ASC")
    suspend fun getByCategory(category: Int): List<DownloadedMarker>
    // 並び替え
    @Update
    suspend fun updateAll(markers: List<DownloadedMarker>)

    @Query("SELECT * FROM downloaded_markers ORDER BY displayOrder ASC")
    suspend fun getAllMarkers(): List<DownloadedMarker>

    // DownloadedMarkerDao
    @Dao
    interface DownloadedMarkerDao {
        @Query("SELECT * FROM downloaded_markers")
        suspend fun getAllDownloadedMarkers(): List<DownloadedMarker>
    }

    @Query("SELECT * FROM downloaded_markers")
    fun getAllDownloadedMarkers(): List<DownloadedMarker>
}