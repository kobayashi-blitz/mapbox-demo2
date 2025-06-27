package com.example.mapboxdemo2.date.model

data class MarkerSetting(
    val id: Int,
    var name: String,
    var filePath: String,     // ★ iconResからfilePathに変更
    var displayOrder: Int,
    var enabled: Boolean
)