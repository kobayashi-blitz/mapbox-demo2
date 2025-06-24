package com.example.mapboxdemo2.model // パッケージ名はmodelのまま

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "markers")
data class MarkerData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val memo: String,
    val iconResId: Int,
    val latitude: Double,
    val longitude: Double,
    val registrationDate: Long
)