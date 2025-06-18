package com.example.mapboxdemo2.feature

import android.content.Context
import com.mapbox.geojson.Point

interface FunctionMarkerAction {
    fun execute(context: Context, point: Point, params: Map<String, Any>? = null)
}