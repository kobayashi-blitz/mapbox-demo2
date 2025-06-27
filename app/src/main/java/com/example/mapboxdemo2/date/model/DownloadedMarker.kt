package com.example.mapboxdemo2.date.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mapboxdemo2.date.model.MarkerCategory

@Entity(tableName = "downloaded_markers")
data class DownloadedMarker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val filePath: String,
    val category: MarkerCategory,  // ← ここでEnumを直接使う！
    var isVisible: Boolean = true,
    val displayOrder: Int = 0,
    val memo: String = "",
    val downloadedAt: Long = System.currentTimeMillis(),
    val functionType: String? = null,
    val isUnlocked: Boolean = false,
    val searchKeyword: String? = null,
)