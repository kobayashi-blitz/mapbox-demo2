package com.example.mapboxdemo2.model

data class MarkerDownloadItem(
    val thumbnailRes: Int,  // 画像リソースID
    val name: String,       // マーカー名
    val category: String    // カテゴリ
)