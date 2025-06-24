package com.example.mapboxdemo2.date.model

enum class MarkerCategory(val value: Int) {
    REGISTER(0),
    SEARCH(1),
    FUNCTION(2),
    OTHER(3);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}