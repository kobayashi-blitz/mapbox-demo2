package com.example.mapboxdemo2.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.mapboxdemo2.model.MarkerData
import androidx.room.Delete

@Dao
interface MarkerDao {
    /**
     * 新しいマーカーをデータベースに挿入（追加）します。
     * suspendキーワードは、この処理が時間がかかる可能性があり、UIを止めないように
     * 別スレッドで実行する必要があることを示します。
     */
    @Insert
    suspend fun insert(marker: MarkerData)

    /**
     * 保存されている全てのマーカーを、IDの新しい順に取得します。
     */
    @Query("SELECT * FROM markers ORDER BY id DESC")
    suspend fun getAllMarkers(): List<MarkerData>

    @Query("SELECT DISTINCT filePath FROM markers")
    suspend fun getAllUniqueFilePaths(): List<String>

    @Query("SELECT * FROM markers WHERE latitude BETWEEN :south AND :north AND longitude BETWEEN :west AND :east")
    suspend fun getMarkersInBounds(north: Double, south: Double, east: Double, west: Double): List<MarkerData>

    @Query("SELECT * FROM markers WHERE id = :markerId LIMIT 1")
    suspend fun getMarkerById(markerId: Int): MarkerData?

    @Delete
    suspend fun delete(marker: MarkerData)

    @Update
    suspend fun update(marker: MarkerData)
}
