package com.example.mapboxdemo2.ViewModel

import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point

class MapStateViewModel : ViewModel() {
    /**
     * マップカメラの初期移動が完了したかどうか
     */
    var isInitialCameraMoveDone = false

    // 最後に表示していたカメラの中心座標
    var lastCameraCenter: Point? = null

    // 最後に表示していたカメラのズームレベル
    var lastCameraZoom: Double? = null
}