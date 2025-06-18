package com.example.mapboxdemo2.feature

import android.content.Context
import com.mapbox.geojson.Point
import android.widget.Toast

class FunctionNavigation(
    private val startNavigation: (origin: Point, dest: Point) -> Unit
) : FunctionMarkerAction {
    override fun execute(context: Context, point: Point, params: Map<String, Any>?) {
        val origin = params?.get("origin") as? Point
        if (origin != null) {
            startNavigation(origin, point)
        } else {
            Toast.makeText(context, "現在地が取得できません", Toast.LENGTH_SHORT).show()
        }
    }
}