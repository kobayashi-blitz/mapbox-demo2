package com.example.mapboxdemo2.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapboxdemo2.date.model.MarkerSetting

class MarkerWheelViewModel : ViewModel() {
    // ホイールに表示するマーカー設定のリストを保持・監視するためのLiveData
    val wheelMarkersLiveData = MutableLiveData<List<MarkerSetting>>()
}