package com.example.mapboxdemo2.date.db

import androidx.room.TypeConverter
import com.example.mapboxdemo2.date.model.MarkerCategory

class MarkerCategoryConverter {
    @TypeConverter
    fun toMarkerCategory(value: Int): MarkerCategory = MarkerCategory.fromInt(value)

    @TypeConverter
    fun fromMarkerCategory(category: MarkerCategory): Int = category.value
}