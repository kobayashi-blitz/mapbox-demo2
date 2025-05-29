package com.example.mapboxdemo2

import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.search.*
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
// PermissionsManagerクラス自体のインポート
import com.mapbox.android.core.permissions.PermissionsManager

// PermissionsListenerインターフェースのインポート（リスナーを実装する場合）
import com.mapbox.android.core.permissions.PermissionsListener

import com.mapbox.maps.ImageHolder

import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener

import com.mapbox.maps.plugin.PuckBearing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.math.pow
import android.widget.ImageView
import android.widget.FrameLayout
import android.view.ViewGroup
import android.text.TextUtils
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.mapbox.search.common.AsyncOperationTask
import android.widget.LinearLayout
import android.graphics.Rect
import android.view.View

import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin

import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource

import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.sources.addSource

import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.android.gestures.MoveGestureDetector

import com.mapbox.maps.plugin.gestures.addOnMapClickListener

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import android.util.Log

import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.scalebar.ScaleBarPlugin

import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog

import com.mapbox.maps.extension.style.layers.addLayer

class MainActivity : AppCompatActivity(), PermissionsListener {

    // 写真検索用リクエストコード
    private val REQUEST_CODE_PHOTO_SEARCH = 101

    // この位置に入れる！
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PHOTO_SEARCH && resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            try {
                val inputStream = contentResolver.openInputStream(uri)
                inputStream?.use {
                    val exif = androidx.exifinterface.media.ExifInterface(it)
                    val latLong = FloatArray(2)
                    if (exif.getLatLong(latLong)) {
                        // Exifから取得成功
                        val lat = latLong[0].toDouble()
                        val lng = latLong[1].toDouble()
                        val point = com.mapbox.geojson.Point.fromLngLat(lng, lat)
                        animateCameraToPosition(point) {
                            showBubbleMarkerAt(point, "写真の位置")
                        }
                    } else {
                        // --- すべてのEXIFタグと値をダンプ ---
                        for (tag in exif.javaClass.fields) {
                            val tagName = tag.name
                            if (tagName.startsWith("TAG_")) {
                                val tagValue = try {
                                    tag.get(null) as? String
                                } catch (e: Exception) {
                                    null
                                }
                                if (tagValue != null) {
                                    val attr = exif.getAttribute(tagValue)
                                    if (attr != null) {
                                        Log.d("EXIF_DUMP", "$tagName ($tagValue): $attr")
                                    }
                                }
                            }
                        }
                        // --- getLatLongが失敗した場合、手動でGPSタグを解析 ---
                        val latitude = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE)
                        val latRef = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF)
                        val longitude = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE)
                        val lngRef = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF)

                        // 1. EXIFタグ取得後すぐログ出力
                        Log.d("EXIF", "EXIFタグ: latitude=$latitude, latRef=$latRef, longitude=$longitude, lngRef=$lngRef")

                        fun convertToDegree(stringDMS: String, ref: String): Double {
                            val dms = stringDMS.split(",")
                            if (dms.size != 3) return 0.0
                            val deg = dms[0].split("/").let { it[0].toDouble() / it[1].toDouble() }
                            val min = dms[1].split("/").let { it[0].toDouble() / it[1].toDouble() }
                            val sec = dms[2].split("/").let { it[0].toDouble() / it[1].toDouble() }
                            var result = deg + min / 60 + sec / 3600
                            if (ref == "S" || ref == "W") result *= -1
                            return result
                        }

                        if (latitude != null && latRef != null && longitude != null && lngRef != null) {
                            val lat = convertToDegree(latitude, latRef)
                            val lng = convertToDegree(longitude, lngRef)
                            // 2. 変換後直後ログ出力
                            Log.d("EXIF", "変換後: 緯度=$lat 経度=$lng")
                            val point = com.mapbox.geojson.Point.fromLngLat(lng, lat)
                            animateCameraToPosition(point) {
                                showBubbleMarkerAt(point, "写真の位置")
                            }
                        } else {
                            Toast.makeText(this, "位置情報が見つかりませんでした", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "写真の位置取得エラー: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 検索用BottomSheetDialog表示
    private fun showSearchMenuDialog() {
        val activity = this  // ← MainActivityの参照をキャプチャ
        val dialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.dialog_search_menu, null)

        // キーワードEditTextでエンター（検索）押下時の挙動を上書き
        val keywordEditText = view.findViewById<android.widget.EditText>(R.id.keywordEditText)
        keywordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = keywordEditText.text.toString().trim()
                dialog.dismiss()
                activity.performSearch(keyword)
                true // イベント消費で「次へ」移動を防ぐ
            } else {
                false
            }
        }

        // 検索履歴の表示（RecyclerViewによるリスト表示・スワイプ削除対応）
        val historyLabel = view.findViewById<TextView>(R.id.historyLabel)
        val historyRecyclerView = view.findViewById<RecyclerView>(R.id.keywordHistoryRecyclerView)
        val historyList = getSearchHistory().take(5).toMutableList()

        if (historyList.isNotEmpty()) {
            historyLabel.visibility = View.VISIBLE
            historyRecyclerView.visibility = View.VISIBLE
            historyRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
            // 仕切り線を追加
            historyRecyclerView.addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(
                    activity,
                    androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                )
            )
            // Adapter定義（匿名inner class）
            class HistoryAdapter(
                val items: MutableList<String>
            ) : RecyclerView.Adapter<HistoryAdapter.VH>() {
                inner class VH(val v: View) : RecyclerView.ViewHolder(v) {
                    val text = v.findViewById<TextView>(android.R.id.text1)
                }
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                    val v = android.view.LayoutInflater.from(parent.context).inflate(
                        android.R.layout.simple_list_item_1, parent, false
                    )
                    v.findViewById<TextView>(android.R.id.text1).apply {
                        textSize = 15f
                        setTextColor(Color.parseColor("#2268d4"))
                        setPadding(36, 18, 36, 18)
                    }
                    return VH(v)
                }
                override fun getItemCount() = items.size
                override fun onBindViewHolder(holder: VH, position: Int) {
                    holder.text.text = items[position]
                    holder.v.setOnClickListener {
                        dialog.dismiss()
                        activity.performSearch(items[position])
                    }
                }
                fun removeAt(pos: Int) {
                    items.removeAt(pos)
                    notifyItemRemoved(pos)
                    saveSearchHistoryList(items)
                }
            }
            val adapter = HistoryAdapter(historyList)
            historyRecyclerView.adapter = adapter

            // スワイプ削除実装（背景赤＋ゴミ箱アイコン描画）
            val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(object :
                androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                    0,
                    androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT
                ) {
                override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
                override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                    adapter.removeAt(holder.adapterPosition)
                    if (adapter.itemCount == 0) {
                        historyLabel.visibility = View.GONE
                        historyRecyclerView.visibility = View.GONE
                    }
                }

                // --- ここから追加: スワイプ中の背景とアイコン描画 ---
                override fun onChildDraw(
                    c: android.graphics.Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE) {
                        val itemView = viewHolder.itemView
                        val icon = androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.delete_24px)
                        val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2

                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#F44336")
                        }
                        if (dX > 0) {
                            // 右スワイプ
                            c.drawRect(
                                itemView.left.toFloat(), itemView.top.toFloat(),
                                itemView.left + dX, itemView.bottom.toFloat(), paint
                            )
                            icon?.let {
                                val iconTop = itemView.top + iconMargin
                                val iconLeft = itemView.left + iconMargin
                                val iconRight = iconLeft + it.intrinsicWidth
                                val iconBottom = iconTop + it.intrinsicHeight
                                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                                it.draw(c)
                            }
                        } else if (dX < 0) {
                            // 左スワイプ
                            c.drawRect(
                                itemView.right + dX, itemView.top.toFloat(),
                                itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                            )
                            icon?.let {
                                val iconTop = itemView.top + iconMargin
                                val iconRight = itemView.right - iconMargin
                                val iconLeft = iconRight - it.intrinsicWidth
                                val iconBottom = iconTop + it.intrinsicHeight
                                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                                it.draw(c)
                            }
                        }
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            })
            itemTouchHelper.attachToRecyclerView(historyRecyclerView)
        } else {
            historyLabel.visibility = View.GONE
            historyRecyclerView.visibility = View.GONE
        }

        // フリーワード検索
        view.findViewById<android.widget.Button>(R.id.keywordSearchButton).setOnClickListener {
            dialog.dismiss()
            activity.performSearch(view.findViewById<android.widget.EditText>(R.id.keywordEditText).text.toString().trim())
            // 履歴やUI更新はperformSearch内または呼び出し後に反映
        }
        dialog.setContentView(view)
        dialog.show()

        // 写真検索ボタンのクリックイベントを追加
        view.findViewById<View>(R.id.photoSearchButton)?.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_PHOTO_SEARCH)
        }
    }
    // 現在ハイライト中のグリッド
    private var highlightedGridPolygon: com.mapbox.geojson.Polygon? = null
    // グリッドの基準起点（アプリ起動時の画面左上座標を保存）
    private var gridOrigin: Point? = null
    // 最後に表示した検索結果を保持
    private var lastSearchResults: List<SearchResult> = emptyList()

    private lateinit var mapView: MapView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var zoomInButton: FloatingActionButton
    private lateinit var zoomOutButton: FloatingActionButton
    private lateinit var myLocationButton: FloatingActionButton
    private var locationListener: OnIndicatorPositionChangedListener? = null

    private var searchRequestTask: AsyncOperationTask? = null

    private lateinit var searchEngine: SearchEngine

    private var currentLocation: Point? = null
    private var currentBubbleView: ImageView? = null

    private var navigationSteps: MutableList<Pair<Point, String>> = mutableListOf()

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val DEFAULT_ZOOM = 17.0
    private val ZOOM_INCREMENT = 1.0
    private lateinit var permissionsManager: PermissionsManager

    private var followListener: OnIndicatorPositionChangedListener? = null
    private var currentTitleView: View? = null
    private var currentMenuLayout: LinearLayout? = null
    private var currentOverlayView: View? = null
    private var cancelNaviButton: ImageView? = null

    private var arOverlayView: View? = null
    private var arDistanceTextView: TextView? = null
    private var arBackgroundView: View? = null

    private lateinit var sensorManager: SensorManager
    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private var destinationPoint: Point? = null // ゴール地点

    private lateinit var sharedPreferences: SharedPreferences
    private val SEARCH_HISTORY_KEY = "search_history"
    private val MAX_HISTORY_SIZE = 10

    private lateinit var clearButton: ImageButton

    private var isNavigating = false
    // --- スプラッシュ表示のための開始時刻記録用 ---
    private var splashShownTime: Long = 0L

    /**
     * 画面表示範囲にズームレベルに応じた絶対グリッド（5m/50m/500m/5km）を描画します。
     * グリッド線はアプリ起動時の gridOrigin（北西端）から計算し、ピッチはズームレベルで決定します。
     * すべてのグリッドサイズはgridOriginからの絶対位置で揃い、グリッドのズレは起きません。
     */
    private fun drawGridOverlay() {
        val style = mapView.getMapboxMap().getStyle() ?: return

        val zoom = mapView.getMapboxMap().cameraState.zoom

        // グリッドサイズ切り替え: 19.0以上→5m, 16.0以上→50m, 13.0以上→500m, それ未満→5000m
        val gridSizeMeters = when {
            zoom >= 19.0 -> 5.0
            zoom >= 16.0 -> 50.0
            zoom >= 13.0 -> 500.0
            else -> 5000.0
        }
        showOrUpdateGridSizeLabel(gridSizeMeters)

        val sourceId = "grid-source"
        val layerId = "grid-layer"

        // 既存のレイヤー/ソースを一度削除（Mapbox Maps v11 仕様）
        if (style.styleLayerExists(layerId)) style.removeStyleLayer(layerId)
        if (style.styleSourceExists(sourceId)) style.removeStyleSource(sourceId)

        // グリッド非表示条件: ズームが11未満ならグリッドを消して終了
        if (zoom < 11.0) {
            hideGridSizeLabel()
            return
        }

        // 画面中心取得
        val center = mapView.getMapboxMap().cameraState.center
        val latCenter = center.latitude()

        // 画面端の緯度経度（南西、北東）
        val bounds = mapView.getMapboxMap().coordinateBoundsForCamera(
            CameraOptions.Builder()
                .center(center)
                .zoom(mapView.getMapboxMap().cameraState.zoom)
                .bearing(mapView.getMapboxMap().cameraState.bearing)
                .pitch(mapView.getMapboxMap().cameraState.pitch)
                .build()
        )
        val minLat = bounds.southwest.latitude()
        val maxLat = bounds.northeast.latitude()
        val minLng = bounds.southwest.longitude()
        val maxLng = bounds.northeast.longitude()

        // 日本全域をカバーする基準点に統一（lat0=20.0, lng0=122.0）
        val lat0 = 20.0
        val lng0 = 122.0
        val metersPerDegreeLat = 111132.0
        val metersPerDegreeLng = 111320.0 * Math.cos(Math.toRadians(lat0))
        val dLat = gridSizeMeters / metersPerDegreeLat
        val dLng = gridSizeMeters / metersPerDegreeLng

        // グリッドの絶対基準点（lat0, lng0）から一定ピッチで描画
        val features = mutableListOf<com.mapbox.geojson.Feature>()

        // 経度方向（縦線）: 基準lng0からdLngごとに描画
        val minGridX = Math.ceil((minLng - lng0) / dLng).toInt()
        val maxGridX = Math.floor((maxLng - lng0) / dLng).toInt()
        for (n in minGridX..maxGridX) {
            val lng = lng0 + n * dLng
            val line = com.mapbox.geojson.LineString.fromLngLats(
                listOf(
                    com.mapbox.geojson.Point.fromLngLat(lng, minLat),
                    com.mapbox.geojson.Point.fromLngLat(lng, maxLat)
                )
            )
            features.add(com.mapbox.geojson.Feature.fromGeometry(line))
        }

        // 緯度方向（横線）: 基準lat0からdLatごとに描画
        val minGridY = Math.ceil((minLat - lat0) / dLat).toInt()
        val maxGridY = Math.floor((maxLat - lat0) / dLat).toInt()
        for (m in minGridY..maxGridY) {
            val lat = lat0 + m * dLat
            val line = com.mapbox.geojson.LineString.fromLngLats(
                listOf(
                    com.mapbox.geojson.Point.fromLngLat(minLng, lat),
                    com.mapbox.geojson.Point.fromLngLat(maxLng, lat)
                )
            )
            features.add(com.mapbox.geojson.Feature.fromGeometry(line))
        }

        val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeatures(features)
        val source = com.mapbox.maps.extension.style.sources.generated.geoJsonSource(sourceId) {
            featureCollection(featureCollection)
        }
        style.addSource(source)

        val gridLineColor = if (gridSizeMeters == 5.0)
            "rgba(255, 140, 0, 0.18)" // オレンジ（透過18%）
        else
            "rgba(30, 80, 200, 0.18)" // 青

        val layer = com.mapbox.maps.extension.style.layers.generated.lineLayer(layerId, sourceId) {
            lineColor(gridLineColor)
            lineWidth(1.0)
        }
        style.addLayer(layer)

        // --- ハイライトグリッドを追加 ---
        val highlightLayerId = "highlight-layer"
        val highlightSourceId = "highlight-source"
        // 既存のハイライトレイヤー/ソースを削除
        if (style.styleLayerExists(highlightLayerId)) style.removeStyleLayer(highlightLayerId)
        if (style.styleSourceExists(highlightSourceId)) style.removeStyleSource(highlightSourceId)
        highlightedGridPolygon?.let { polygon ->
            val highlightSource = com.mapbox.maps.extension.style.sources.generated.geoJsonSource(highlightSourceId) {
                geometry(polygon)
            }
            style.addSource(highlightSource)
            val highlightLayer = com.mapbox.maps.extension.style.layers.generated.fillLayer(highlightLayerId, highlightSourceId) {
                fillColor("rgba(100,200,255,0.22)") // 薄い青の半透明
                fillOutlineColor("rgba(30,80,200,0.38)")
            }
            style.addLayerAbove(highlightLayer, layerId)
        }
    }

    /**
     * Activity 起動時の初期化処理を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        mapView = findViewById(R.id.mapView)
        zoomInButton = findViewById(R.id.zoomInButton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        myLocationButton = findViewById(R.id.myLocationButton)

        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            ApiType.SEARCH_BOX,
            SearchEngineSettings()
        )

        initializeMap()
        setupButtonListeners()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sharedPreferences = getSharedPreferences("search_prefs", MODE_PRIVATE)

        // --- 検索ダイアログアイコンの表示＆クリックリスナー ---
        val searchDialogButton = findViewById<ImageButton>(R.id.showSearchDialogButton)
        searchDialogButton.setOnClickListener { showSearchMenuDialog() }

        // --- グリッドタップでハイライト ---
        mapView.getMapboxMap().addOnMapClickListener { point: Point ->
            // ★ナビ中はグリッドタップ無効化
            if (isNavigating) return@addOnMapClickListener false
            val style = mapView.getMapboxMap().getStyle() ?: return@addOnMapClickListener false
            val zoom = mapView.getMapboxMap().cameraState.zoom
            if (zoom < 19.0) {
                highlightedGridPolygon = null
                drawGridOverlay()
                return@addOnMapClickListener false
            }
            // グリッド描画と同じ基準（lat0=20.0, lng0=122.0）でグリッドサイズ再計算
            val gridSizeMeters = 5.0
            val lat0 = 20.0
            val lng0 = 122.0
            val metersPerDegreeLat = 111132.0
            val metersPerDegreeLng = 111320.0 * Math.cos(Math.toRadians(lat0))
            val dLat = gridSizeMeters / metersPerDegreeLat
            val dLng = gridSizeMeters / metersPerDegreeLng

            // タップ位置から「このグリッドの左上」を計算
            val gridY = Math.floor((point.latitude() - lat0) / dLat)
            val gridX = Math.floor((point.longitude() - lng0) / dLng)
            val gridLat0 = lat0 + gridY * dLat
            val gridLng0 = lng0 + gridX * dLng
            val gridLat1 = gridLat0 + dLat
            val gridLng1 = gridLng0 + dLng

            // グリッドの中心座標
            val centerLat = (gridLat0 + gridLat1) / 2.0
            val centerLng = (gridLng0 + gridLng1) / 2.0
            val gridCenter = Point.fromLngLat(centerLng, centerLat)

            // グリッドをハイライト
            highlightedGridPolygon = com.mapbox.geojson.Polygon.fromLngLats(
                listOf(
                    listOf(
                        com.mapbox.geojson.Point.fromLngLat(gridLng0, gridLat0),
                        com.mapbox.geojson.Point.fromLngLat(gridLng1, gridLat0),
                        com.mapbox.geojson.Point.fromLngLat(gridLng1, gridLat1),
                        com.mapbox.geojson.Point.fromLngLat(gridLng0, gridLat1),
                        com.mapbox.geojson.Point.fromLngLat(gridLng0, gridLat0)
                    )
                )
            )
            drawGridOverlay()

            // 地図をグリッド中央にアニメーション移動
            animateCameraToPosition(gridCenter) {
                // アニメーション終了後にバブルメニュー表示
                showBubbleMarkerAt(gridCenter, "このグリッド")
            }
            false
        }
    }



    /**
     * Mapbox 地図のスタイル設定とジェスチャー設定を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private var splashImageView: ImageView? = null

    private fun initializeMap() {
        mapView.mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            // 縮尺バーを非表示にする
            mapView.getPlugin<ScaleBarPlugin>(Plugin.MAPBOX_SCALEBAR_PLUGIN_ID)?.updateSettings {
                enabled = false
            }

            mapView.gestures.updateSettings {
                rotateEnabled = false
            }

            // グリッド描画（初回表示時）
            drawGridOverlay()

            // カメラ移動・ズーム変更ごとにグリッドを再描画
            mapView.getMapboxMap().addOnCameraChangeListener {
                drawGridOverlay()
            }

            // --- スプラッシュ画像を最前面に表示 ---
            val rootView = findViewById<FrameLayout>(android.R.id.content)
            // スプラッシュ表示開始時刻を記録
            splashShownTime = System.currentTimeMillis()
            splashImageView = ImageView(this).apply {
                setImageResource(R.drawable.splash)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            rootView.addView(splashImageView)

            if (checkLocationPermission()) {
                enableLocationComponent()
                moveToCurrentLocationOnce() // ← ★ 現在地に移動する処理を追加
                mapView.location.addOnIndicatorPositionChangedListener {
                    currentLocation = it
                }

                mapView.gestures.addOnMoveListener(object : OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {
                        followListener?.let {
                            mapView.location.removeOnIndicatorPositionChangedListener(it)
                            followListener = null
                        }
                    }
                    override fun onMove(detector: MoveGestureDetector): Boolean = false
                    override fun onMoveEnd(detector: MoveGestureDetector) {}
                })

            } else {
                requestLocationPermission()
            }
        }
    }

    /**
     * 位置情報権限が許可されているかどうかを返します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun checkLocationPermission(): Boolean {
        return PermissionsManager.areLocationPermissionsGranted(this)
    }

    // --- MapView/MapboxMap初期化時にズームレベル制限を設定 ---
    // ※ mapView.getMapboxMap().loadStyle の直後や、スタイルロード時コールバック内に追加
    // ズームレベルの下限・上限を設定（min: 都市レベル, max: 建物レベル）
    private fun setMapZoomBoundsOnce() {
        mapView.getMapboxMap().setBounds(
            com.mapbox.maps.CameraBoundsOptions.Builder()
                .minZoom(12.0)
                .maxZoom(19.5)
                .build()
        )
    }

    /**
     * UI ボタンのクリックリスナーをセットアップします。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun setupButtonListeners() {
        zoomInButton.setOnClickListener {
            val zoom = mapView.getMapboxMap().cameraState.zoom + ZOOM_INCREMENT
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().zoom(zoom).build())
        }

        zoomOutButton.setOnClickListener {
            val zoom = mapView.getMapboxMap().cameraState.zoom - ZOOM_INCREMENT
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().zoom(zoom).build())
        }

        myLocationButton.setOnClickListener {
            if (checkLocationPermission()) {
                mapView.location.updateSettings {
                    enabled = true
                }

                // 以前登録していたリスナーを削除
                locationListener?.let {
                    mapView.location.removeOnIndicatorPositionChangedListener(it)
                }

                locationListener = OnIndicatorPositionChangedListener { point ->
                    currentLocation = point
                    val currentZoom = mapView.getMapboxMap().cameraState.zoom
                    val cameraOptions = CameraOptions.Builder()
                        .center(point)
                        .zoom(currentZoom)
                        .build()
                    val animationOptions = MapAnimationOptions.Builder()
                        .duration(1000)
                        .build()
                    mapView.getMapboxMap().flyTo(cameraOptions, animationOptions)

                    // 一度カメラ移動したらリスナー削除
                    locationListener?.let {
                        mapView.location.removeOnIndicatorPositionChangedListener(it)
                    }
                    //  現在地追従を再開
                    startFollowingUser()
                }

                mapView.location.addOnIndicatorPositionChangedListener(locationListener!!)
            } else {
                requestLocationPermission()
            }
        }
    }



    /**
     * 検索クエリを実行し、結果をハンドリングします。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun performSearch(query: String) {
        saveSearchHistory(query)
        if (query.isBlank()) return
        // --- グリッドIDなら確認ダイアログを表示 ---
        if (isGridId(query)) {
            // AlertDialogで確認
            AlertDialog.Builder(this)
                .setMessage("入力内容「$query」はグリッドIDとして認識されました。このグリッドへ移動しますか？")
                .setPositiveButton("はい") { dialog, _ ->
                    // 従来のグリッドIDジャンプ処理
                    val center = pointFromGridId(query)
                    // --- グリッドハイライト処理を追加 ---
                    // 1. グリッド枠の四隅座標を求める
                    val lat0 = 20.0
                    val lng0 = 122.0
                    val gridSizeMeters = 5.0
                    val metersPerDegreeLat = 111132.0
                    val metersPerDegreeLng = 111320.0 * Math.toRadians(lat0).let { Math.cos(it) }
                    val dLat = gridSizeMeters / metersPerDegreeLat
                    val dLng = gridSizeMeters / metersPerDegreeLng

                    val y = Math.floor((center.latitude() - lat0) / dLat)
                    val x = Math.floor((center.longitude() - lng0) / dLng)
                    val gridLat0 = lat0 + y * dLat
                    val gridLng0 = lng0 + x * dLng
                    val gridLat1 = gridLat0 + dLat
                    val gridLng1 = gridLng0 + dLng

                    // 2. ハイライト用Polygon作成
                    highlightedGridPolygon = com.mapbox.geojson.Polygon.fromLngLats(
                        listOf(
                            listOf(
                                com.mapbox.geojson.Point.fromLngLat(gridLng0, gridLat0),
                                com.mapbox.geojson.Point.fromLngLat(gridLng1, gridLat0),
                                com.mapbox.geojson.Point.fromLngLat(gridLng1, gridLat1),
                                com.mapbox.geojson.Point.fromLngLat(gridLng0, gridLat1),
                                com.mapbox.geojson.Point.fromLngLat(gridLng0, gridLat0)
                            )
                        )
                    )
                    drawGridOverlay()
                    dialog.dismiss()
                    animateCameraToPosition(center) {
                        showBubbleMarkerAt(center, "このグリッド")
                    }
                }
                .setNegativeButton("いいえ") { dialog, _ ->
                    // 通常のフリーワード検索（Mapbox検索API）を続行
                    dialog.dismiss()
                    // 前の検索リクエストをキャンセル
                    searchRequestTask?.cancel()

                    val center = mapView.getMapboxMap().cameraState.center
                    val options = SearchOptions.Builder()
                        .proximity(center)
                        .limit(10)
                        .build()

                    searchEngine.search(query, options, object : SearchSelectionCallback {
                        override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                            if (suggestions.isNotEmpty()) {
                                searchEngine.select(suggestions.first(), this)
                            } else {
                                Toast.makeText(this@MainActivity, "見つかりませんでした", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                            showSearchResultsModal(listOf(result))
                        }
                        override fun onResults(
                            suggestion: SearchSuggestion,
                            results: List<SearchResult>,
                            responseInfo: ResponseInfo
                        ) {
                            if (results.isNotEmpty()) {
                                val keyword = query.trim()
                                val filtered = results.filter {
                                    it.name.contains(keyword, ignoreCase = true)
                                }
                                if (filtered.isNotEmpty()) {
                                    val sorted = sortResultsByDistance(filtered)
                                    showSearchResultsModal(sorted)
                                } else {
                                    val sorted = sortResultsByDistance(results)
                                    showSearchResultsModal(sorted)
                                }
                            } else {
                                Toast.makeText(this@MainActivity, "候補が見つかりませんでした", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onError(e: Exception) {
                            Toast.makeText(this@MainActivity, "検索失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                .show()
            return
        }
        // --- ここから従来のフリーワード検索 ---
        // 前の検索リクエストをキャンセル
        searchRequestTask?.cancel()

        val center = mapView.getMapboxMap().cameraState.center

        val options = SearchOptions.Builder()
            .proximity(center)
            .limit(10)
            .build()

        searchEngine.search(query, options, object : SearchSelectionCallback {
            override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                if (suggestions.isNotEmpty()) {
                    searchEngine.select(suggestions.first(), this)
                } else {
                    Toast.makeText(this@MainActivity, "見つかりませんでした", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                showSearchResultsModal(listOf(result))
            }
            override fun onResults(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                if (results.isNotEmpty()) {
                    val keyword = query.trim()
                    val filtered = results.filter {
                        it.name.contains(keyword, ignoreCase = true)
                    }
                    if (filtered.isNotEmpty()) {
                        val sorted = sortResultsByDistance(filtered)
                        showSearchResultsModal(sorted)
                    } else {
                        val sorted = sortResultsByDistance(results)
                        showSearchResultsModal(sorted)
                    }
                } else {
                    Toast.makeText(this@MainActivity, "候補が見つかりませんでした", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onError(e: Exception) {
                Toast.makeText(this@MainActivity, "検索失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * 指定座標にカメラをアニメーション移動します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun animateCameraToPosition(point: Point, onAnimationEnd: (() -> Unit)? = null) {
        val currentZoom = mapView.getMapboxMap().cameraState.zoom
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(currentZoom)
            .build()

        val animationOptions = MapAnimationOptions.Builder()
            .duration(1000)
            .build()

        mapView.getMapboxMap().flyTo(cameraOptions, animationOptions)

        // durationに合わせてバブルを表示する
        mapView.postDelayed({
            onAnimationEnd?.invoke()
        }, 1000)
    }

    /**
     * 現在地表示コンポーネントを有効化します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun enableLocationComponent() {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_oval_puck)!!
        val bitmap = drawableToBitmap(drawable, 64)

        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true

            locationPuck = LocationPuck2D(
                topImage = null,
                bearingImage = ImageHolder.from(bitmap), // 回転用画像
                shadowImage = null,
                scaleExpression = null
            )
        }

        mapView.location.puckBearing = PuckBearing.HEADING
    }

    /**
     * 現在地に一度だけカメラ移動します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun moveToCurrentLocationOnce() {
        // リスナーをあとで remove するため lateinit で宣言
        lateinit var listener: OnIndicatorPositionChangedListener

        listener = OnIndicatorPositionChangedListener { point ->
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(DEFAULT_ZOOM)
                    .build()
            )
            mapView.location.removeOnIndicatorPositionChangedListener(listener)
            // --- スプラッシュ画像を消す ---
            splashImageView?.let {
                val elapsed = System.currentTimeMillis() - splashShownTime
                val remaining = 1500L - elapsed
                if (remaining > 0) {
                    it.postDelayed({
                        val rootView = findViewById<FrameLayout>(android.R.id.content)
                        rootView.removeView(it)
                        splashImageView = null
                    }, remaining)
                } else {
                    val rootView = findViewById<FrameLayout>(android.R.id.content)
                    rootView.removeView(it)
                    splashImageView = null
                }
            }
        }

        mapView.location.addOnIndicatorPositionChangedListener(listener)
    }

    /**
     * 現在地追従リスナーをセットアップします。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun startFollowingUser() {
        followListener?.let {
            mapView.location.removeOnIndicatorPositionChangedListener(it)
        }

        followListener = OnIndicatorPositionChangedListener { point ->
            val currentZoom = mapView.getMapboxMap().cameraState.zoom
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(currentZoom)
                    .build()
            )

            // 案内ロジック
            if (navigationSteps.isNotEmpty()) {
                val (stepPoint, instruction) = navigationSteps.first()
                val distance = distanceBetween(point, stepPoint)

                if (distance < 30) {
                    Toast.makeText(this, "📣 $instruction", Toast.LENGTH_LONG).show()
                    navigationSteps.removeAt(0)

                    if (navigationSteps.isEmpty()) {
                        Toast.makeText(this, "🎉 目的地に到着しました！", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        mapView.location.addOnIndicatorPositionChangedListener(followListener!!)
    }

    /**
     * 表示中のバブルビューをすべて削除します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun removeBubbleViews() {
        currentBubbleView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentTitleView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentMenuLayout?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentOverlayView?.let { (it.parent as? ViewGroup)?.removeView(it) }

        currentBubbleView = null
        currentTitleView = null
        currentMenuLayout = null
        currentOverlayView = null
    }



    /**
     * 位置情報権限のリクエストを行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun requestLocationPermission() {
        permissionsManager = PermissionsManager(this)
        permissionsManager.requestLocationPermissions(this)
    }

    /**
     * API からルートデータを取得し、地図上にルート線を描画します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun drawRouteLine(origin: Point, destination: Point) {
        val accessToken = getString(R.string.mapbox_access_token)
        val url = "https://api.mapbox.com/directions/v5/mapbox/walking/" +
                "${origin.longitude()},${origin.latitude()};" +
                "${destination.longitude()},${destination.latitude()}" +
                "?geometries=geojson&steps=true&language=ja&access_token=$accessToken"

        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().readText()

                val json = org.json.JSONObject(response)

                // ルート線の描画
                val geometry = json.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")

                val points = mutableListOf<Point>()
                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    points.add(Point.fromLngLat(coord.getDouble(0), coord.getDouble(1)))
                }

                val routeLine = com.mapbox.geojson.LineString.fromLngLats(points)
                val routeFeature = com.mapbox.geojson.Feature.fromGeometry(routeLine)
                val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeatures(arrayOf(routeFeature))

                // ステップ案内（instruction + 座標）
                val stepsArray = json.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)
                    .getJSONArray("steps")

                navigationSteps.clear()
                for (i in 0 until stepsArray.length()) {
                    val step = stepsArray.getJSONObject(i)
                    val maneuver = step.getJSONObject("maneuver")
                    val instruction = maneuver.getString("instruction")
                    val location = maneuver.getJSONArray("location")
                    val stepPoint = Point.fromLngLat(location.getDouble(0), location.getDouble(1))
                    navigationSteps.add(Pair(stepPoint, instruction))
                }

                runOnUiThread {
                    isNavigating = true  // ← ナビ開始時にセット
                    // --- 検索ダイアログアイコン（showSearchDialogButton）を無効化・グレーアウト ---
                    val searchDialogButton = findViewById<ImageButton>(R.id.showSearchDialogButton)
                    searchDialogButton.isEnabled = false
                    searchDialogButton.alpha = 0.5f

                    destinationPoint = destination
                    // バブルを削除
                    removeBubbleViews()

                    val sourceId = "route-source"
                    val layerId = "route-layer"
                    val style = mapView.getMapboxMap().getStyle() ?: return@runOnUiThread

                    if (!style.styleSourceExists(sourceId)) {
                        val source = geoJsonSource(sourceId) {
                            featureCollection(featureCollection)
                        }
                        style.addSource(source)

                        val layer = lineLayer(layerId, sourceId) {
                            lineColor("#4264fb")
                            lineWidth(6.0)
                            lineCap(LineCap.ROUND)
                            lineJoin(LineJoin.ROUND)
                        }

                        style.addLayerBelow(layer, "road-label")
                    } else {
                        val source = style.getSourceAs<GeoJsonSource>(sourceId)
                        source?.featureCollection(featureCollection)
                    }

                    // ナビ開始（追従と案内も開始）
                    if (followListener == null) {
                        startFollowingUser()
                    }

                    showCancelNaviButton()
                    showArDirectionOverlay() // 疑似AR画像表示
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "ルート取得失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /**
     * 権限リクエストの結果を処理します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // permissionsをArray<String>型にキャスト
        permissionsManager.onRequestPermissionsResult(requestCode, permissions as Array<String>, grantResults)
    }

    /**
     * 権限説明が必要な場合の UI 表示を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, "位置情報の許可が必要です", Toast.LENGTH_LONG).show()
    }

    /**
     * 権限許可結果に応じた処理を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent()
        } else {
            Snackbar.make(mapView, "位置情報の許可が必要です", Snackbar.LENGTH_INDEFINITE)
                .setAction("設定") {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                }.show()
        }
    }

    /**
     * Activity の onStart ライフサイクル処理を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    /**
     * Activity の onStop ライフサイクル処理を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    /**
     * メモリ不足時の処理を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    /**
     * Activity の onDestroy ライフサイクル処理を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    /**
     * Drawable オブジェクトを Bitmap に変換します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * 2 つの座標間の距離（メートル）を計算して返します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun distanceBetween(p1: Point, p2: Point): Double {
        val R = 6371000.0 // 地球の半径（メートル）
        val lat1 = Math.toRadians(p1.latitude())
        val lat2 = Math.toRadians(p2.latitude())
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(p2.longitude() - p1.longitude())
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    /**
     * 検索結果を現在地との距離でソートして返します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun sortResultsByDistance(results: List<SearchResult>): List<SearchResult> {
        val current = currentLocation
        return if (current != null) {
            results.sortedBy { distanceBetween(current, it.coordinate) }
        } else results
    }

    /**
     * 検索結果を BottomSheet ダイアログで表示します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun showSearchResultsModal(results: List<SearchResult>) {
        // 検索結果を保存
        lastSearchResults = results
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_search_results, null)
        val listView = view.findViewById<android.widget.ListView>(R.id.searchResultsListView)

        val current = currentLocation

        val displayList = results.map { result ->
            val distance = if (current != null) {
                val d = distanceBetween(current, result.coordinate)
                "（${d.toInt()}m）"
            } else {
                "（距離不明）"
            }
            "${result.name} $distance"
        }

        listView.adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayList
        )

        listView.setOnItemClickListener { _, _, position, _ ->
            dialog.dismiss()
            val selected = results[position]

            // 距離を取得（currentLocation がある場合）
            val distance = currentLocation?.let {
                distanceBetween(it, selected.coordinate).toInt()
            }

            // 距離つきタイトルを作成
            val label = if (distance != null) {
                "${selected.name}（${distance}m）"
            } else {
                selected.name
            }

            // カメラ移動 → アニメーション後にバブル表示
            animateCameraToPosition(selected.coordinate) {
                showBubbleMarkerAt(selected.coordinate, label)
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    /**
     * 指定座標にバブルマーカーを表示します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun showBubbleMarkerAt(point: Point, title: String = "") {
        // バブル表示時は現在地追従をOFF
        followListener?.let {
            mapView.location.removeOnIndicatorPositionChangedListener(it)
            followListener = null
        }
        // 既存のバブルを削除
        currentBubbleView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }

        // スクリーンのサイズを取得
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // 吹き出し画像のサイズ（px単位）
        val bubbleWidth = (screenWidth * 0.8).toInt()
        val aspectRatio = 250f / 460f
        val bubbleHeight = (bubbleWidth * aspectRatio).toInt()

        // マージン計算
        val leftMargin = (screenWidth / 2) - (bubbleWidth / 2)
        val topMargin = (screenHeight / 2) - bubbleHeight - 25

        // 吹き出し画像を作成
        val imageView = ImageView(this).apply {
            setImageResource(R.drawable.bubble_marker_01)
            layoutParams = FrameLayout.LayoutParams(
                bubbleWidth,
                bubbleHeight
            ).apply {
                this.leftMargin = leftMargin
                this.topMargin = topMargin
            }
        }

        // 距離テキストとグリッドIDを横並びで表示するレイアウト
        val hanazome = ResourcesCompat.getFont(this, R.font.hanazome)
        val distanceText: String? = if (currentLocation != null) {
            val dist = distanceBetween(currentLocation!!, point).toInt()
            "現在地から約${dist}m"
        } else {
            null
        }
        val titleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                bubbleWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                this.topMargin = topMargin + 20
            }
        }
        // 距離テキスト
        val distanceTextView = TextView(this).apply {
            text = distanceText ?: ""
            typeface = hanazome
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(20, 10, 5, 10)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            visibility = if (distanceText != null) View.VISIBLE else View.GONE
        }
        // グリッドIDテキスト（カッコ付きでコピー可）
        val gridId = gridIdFromPoint(point)
        val gridIdTextView = TextView(this).apply {
            text = "（$gridId）"
            typeface = hanazome
            textSize = 15f
            setTextColor(Color.parseColor("#448aff"))
            setPadding(5, 10, 20, 10)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GridID", gridId))
                Toast.makeText(context, "グリッドIDをコピーしました", Toast.LENGTH_SHORT).show()
            }
            visibility = if (distanceText != null) View.VISIBLE else View.GONE
        }
        titleLayout.addView(distanceTextView)
        titleLayout.addView(gridIdTextView)

        val menuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = topMargin + 110  // タイトルより下
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val size = 64.dpToPx()
            val margin = 8.dpToPx()

            fun createMenuButton(drawableId: Int, onClick: () -> Unit): ImageView {
                return ImageView(this@MainActivity).apply {
                    setImageResource(drawableId)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(margin, 0, margin, 0)
                    }
                    setOnClickListener { onClick() }
                }
            }

            addView(createMenuButton(R.drawable.location_menu_regist) {
                Toast.makeText(context, "場所を登録", Toast.LENGTH_SHORT).show()
            })

            addView(createMenuButton(R.drawable.location_menu_navi) {
                // 目的地にナビする
                currentLocation?.let { origin ->
                    drawRouteLine(origin, point)
                } ?: run {
                    Toast.makeText(context, "現在地が取得できません", Toast.LENGTH_SHORT).show()
                }
            })

            // --- 共有ボタン: クリップボードやBottomSheetDialogを使わず、直接共有Intentを呼び出す ---
            addView(createMenuButton(R.drawable.location_menu_share) {
                val id = gridIdFromPoint(point)
                val shareUrl = "https://nakamarker.com/g/$id"
                val shareText = "ナカマーカーが場所情報をお伝えします！ $shareUrl ※リンクタップでアプリが起動するよ！"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                startActivity(Intent.createChooser(shareIntent, "アプリで共有"))
            })
        }

        // overlayView を作成（画面全体を覆う透明ビュー）
        val overlayView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }




        // ルートに追加
        val rootView = findViewById<FrameLayout>(android.R.id.content)
        rootView.addView(imageView)
        rootView.addView(titleLayout)
        rootView.addView(menuLayout)
        rootView.addView(overlayView)

        overlayView.setOnTouchListener { _, event ->
            val bubbleRect = Rect()
            val titleRect = Rect()
            val menuRect = Rect()

            imageView.getGlobalVisibleRect(bubbleRect)
            titleLayout.getGlobalVisibleRect(titleRect)
            menuLayout.getGlobalVisibleRect(menuRect)

            // バブル領域の結合
            bubbleRect.union(titleRect)
            bubbleRect.union(menuRect)

            // タッチが外だった場合にすべて削除
            if (!bubbleRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                (imageView.parent as? ViewGroup)?.removeView(imageView)
                (titleLayout.parent as? ViewGroup)?.removeView(titleLayout)
                (menuLayout.parent as? ViewGroup)?.removeView(menuLayout)
                (overlayView.parent as? ViewGroup)?.removeView(overlayView)
                currentBubbleView = null
                // --- グリッドハイライトもクリア ---
                highlightedGridPolygon = null
                drawGridOverlay()
                return@setOnTouchListener true
            }
            false
        }

        currentBubbleView = imageView
        currentTitleView = titleLayout
        currentMenuLayout = menuLayout
        currentOverlayView = overlayView

    }

    /**
     * ナビキャンセルボタンを表示します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun showCancelNaviButton() {
        if (cancelNaviButton != null) return

        val size = 124.dpToPx()  // ← 任意のサイズ、必要なら調整

        cancelNaviButton = ImageView(this).apply {
            setImageResource(R.drawable.navi_cancel)
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 80.dpToPx()
            }

            setOnClickListener {
                stopNavigation()
            }
        }

        val rootView = findViewById<FrameLayout>(android.R.id.content)
        rootView.addView(cancelNaviButton)
    }

    /**
     * ナビキャンセルボタンを非表示にします。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun hideCancelNaviButton() {
        cancelNaviButton?.let { (it.parent as? ViewGroup)?.removeView(it) }
        cancelNaviButton = null
    }

    /**
     * AR オーバーレイ画像を非表示にします。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun hideArDirectionOverlay() {
        arOverlayView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        arOverlayView = null
        arDistanceTextView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        arDistanceTextView = null
        arBackgroundView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        arBackgroundView = null
    }

    /**
     * AR オーバーレイ画像を表示します。
     *
     *  - 背景ビューはキャラクター画像の下端まで自動調整（onLayoutで高さ調整）。
     *  - arBackgroundView, arDistanceTextView, arOverlayViewの順でaddView。
     *  - 距離テキストやキャラクターの背景はarBackgroundViewのみでカバー。
     *  - arBackgroundViewの背景色は20%透過白。
     *  - 不要なsetBackgroundColor(Color.TRANSPARENT)は削除。
     */
    private fun showArDirectionOverlay() {
        // Always hide any existing overlay before adding new ones
        hideArDirectionOverlay()

        val rootView = findViewById<FrameLayout>(android.R.id.content)

        // 1. Distance TextView（背景なし、中央、topMargin 24dp）
        arDistanceTextView = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.BLACK)
            // setBackgroundColor(Color.TRANSPARENT) // ← 不要なので削除
            gravity = Gravity.CENTER
            setPadding(12, 4, 12, 4)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 24.dpToPx()
            }
        }

        // 2. キャラクター画像 ImageView
        arOverlayView = ImageView(this).apply {
            setImageResource(R.drawable.ar_navi)
            layoutParams = FrameLayout.LayoutParams(
                160.dpToPx(), 90.dpToPx()
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 56.dpToPx()
            }
        }

        // 3. 背景View（キャラクター画像の最下部まで高さ自動調整）
        arBackgroundView = object : View(this) {}.apply {
            setBackgroundColor(android.graphics.Color.argb(51, 255, 255, 255)) // 20%透過白
            // MATCH_PARENT x WRAP_CONTENT, gravity TOP, bottomMargin 0
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
                topMargin = 0
                bottomMargin = 0
            }
            // 高さをキャラクター画像の最下部まで自動調整
            // onLayout後にキャラクター画像のbottomまで高さを調整
            val adjustHeight = {
                // arOverlayViewがまだrootViewに追加されていなければスキップ
                val overlay = arOverlayView
                if (overlay != null && overlay.parent != null) {
                    // ルート座標系でキャラクター画像のbottomを取得
                    val loc = IntArray(2)
                    overlay.getLocationOnScreen(loc)
                    val overlayTop = loc[1]
                    val overlayHeight = overlay.height
                    val rootLoc = IntArray(2)
                    rootView.getLocationOnScreen(rootLoc)
                    val rootTop = rootLoc[1]
                    val bottom = overlayTop + overlayHeight - rootTop
                    // 背景Viewをbottomまで伸ばす
                    val lp = this.layoutParams
                    if (lp is FrameLayout.LayoutParams) {
                        if (this.height != bottom) {
                            lp.height = bottom
                            this.layoutParams = lp
                        }
                    }
                }
            }
            // キャラクター画像のレイアウト後に呼ぶ
            post {
                adjustHeight()
            }
        }

        // addView順: 背景 → 距離テキスト → キャラクター
        rootView.addView(arBackgroundView)
        rootView.addView(arDistanceTextView)
        rootView.addView(arOverlayView)

        // キャラクター画像のレイアウト後に背景高さを調整
        arOverlayView?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // arBackgroundViewがnullなら何もしない
            arBackgroundView?.let { bgView ->
                val overlay = arOverlayView
                if (overlay != null && overlay.parent != null) {
                    // ルート座標系でキャラクター画像のbottomを取得
                    val loc = IntArray(2)
                    overlay.getLocationOnScreen(loc)
                    val overlayTop = loc[1]
                    val overlayHeight = overlay.height
                    val rootLoc = IntArray(2)
                    rootView.getLocationOnScreen(rootLoc)
                    val rootTop = rootLoc[1]
                    val bottom = overlayTop + overlayHeight - rootTop
                    val lp = bgView.layoutParams
                    if (lp is FrameLayout.LayoutParams) {
                        if (bgView.height != bottom) {
                            lp.height = bottom
                            bgView.layoutParams = lp
                        }
                    }
                }
            }
        }
    }


    /**
     * ナビゲーションを終了し、表示をリセットします。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun stopNavigation() {
        isNavigating = false  // ← ナビ終了時に解除
        navigationSteps.clear()
        hideCancelNaviButton()
        hideArDirectionOverlay() // 疑似AR画像非表示

        // ルート線を消す
        val style = mapView.getMapboxMap().getStyle()
        style?.let {
            it.removeStyleLayer("route-layer")
            it.removeStyleSource("route-source")
        }

        // --- 検索ダイアログアイコン（showSearchDialogButton）を再度有効化・通常表示に ---
        val searchDialogButton = findViewById<ImageButton>(R.id.showSearchDialogButton)
        searchDialogButton.isEnabled = true
        searchDialogButton.alpha = 1.0f

        highlightedGridPolygon = null
        drawGridOverlay()

        Toast.makeText(this, "ナビゲーションを終了しました", Toast.LENGTH_SHORT).show()

    }

private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                }
            }

            if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat() // 方位角（北を0°とする）
                updateCharacterPositionBasedOnBearing(azimuth)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    // グリッドサイズ表示用TextView
    private var gridSizeLabel: TextView? = null

    // グリッドサイズラベルの表示・更新
    private fun showOrUpdateGridSizeLabel(gridSizeMeters: Double) {
        val rootView = findViewById<FrameLayout>(android.R.id.content)
        if (gridSizeLabel == null) {
            gridSizeLabel = TextView(this).apply {
                textSize = 15f
                setTextColor(Color.BLACK)
                // 背景色は使わず、白いシャドウだけで見やすさをアップ
                setPadding(8, 4, 8, 4)
                setShadowLayer(5f, 0f, 0f, Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.START
                ).apply {
                    topMargin = 8.dpToPx()
                    leftMargin = 8.dpToPx()
                }
            }
            rootView.addView(gridSizeLabel)
        }
        gridSizeLabel?.text = "□ = ${gridSizeMeters.toInt()}m"
    }

    // グリッド非表示時はラベルも消す
    private fun hideGridSizeLabel() {
        gridSizeLabel?.let { label ->
            val rootView = findViewById<FrameLayout>(android.R.id.content)
            rootView.removeView(label)
            gridSizeLabel = null
        }
    }

    // dp→px変換
    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun bearingToTarget(from: Point, to: Point): Double {
        val lat1 = Math.toRadians(from.latitude())
        val lon1 = Math.toRadians(from.longitude())
        val lat2 = Math.toRadians(to.latitude())
        val lon2 = Math.toRadians(to.longitude())

        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360
    }

    override fun onResume() {
        super.onResume()
        mapView.onStart()

        // --- MapboxMapのズーム制限をスタイルロード完了時に一度だけ設定 ---
        // 既存の loadStyle コールバック内で呼ぶのが理想だが、なければここで一度呼ぶ
        // ただし、複数回呼ばれないように工夫する必要がある

        //     ...他の初期化...
        // }
        // ここでは onResume 時にも念のため実行しておく（重複しても問題は起きにくい）
        setMapZoomBoundsOnce()

        sensorManager.registerListener(
            sensorEventListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            sensorEventListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI
        )

    }



    override fun onPause() {
        super.onPause()
        mapView.onStop()

        sensorManager.unregisterListener(sensorEventListener)
    }

    private fun saveSearchHistory(query: String) {
        if (query.isBlank()) return

        val history = getSearchHistory().toMutableList()
        history.remove(query) // 重複削除
        history.add(0, query) // 新しい順に先頭へ

        if (history.size > MAX_HISTORY_SIZE) {
            history.removeLast()
        }

        val joined = history.joinToString("||")
        sharedPreferences.edit().putString(SEARCH_HISTORY_KEY, joined).apply()
    }

    private fun getSearchHistory(): List<String> {
        val stored = sharedPreferences.getString(SEARCH_HISTORY_KEY, null)
        return stored?.split("||") ?: emptyList()
    }

    // 検索履歴ダイアログ（スワイプ削除対応、最大5件、クリックで検索、削除で更新）
    private fun showSearchHistoryDialog() {
        val historyList = getSearchHistory().take(5).toMutableList()
        if (historyList.isEmpty()) {
            Toast.makeText(this, "検索履歴はありません", Toast.LENGTH_SHORT).show()
            return
        }

        val frameLayout = FrameLayout(this)
        val recyclerView = androidx.recyclerview.widget.RecyclerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            setHasFixedSize(true)
            addItemDecoration(
                androidx.recyclerview.widget.DividerItemDecoration(context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL)
            )
        }
        frameLayout.addView(recyclerView)
        var alertDialog: AlertDialog? = null

        // Adapter（onDeleteラムダを廃止し、removeAtで一括処理）
        class HistoryAdapter(
            val items: MutableList<String>,
            val onClick: (String) -> Unit
        ) : androidx.recyclerview.widget.RecyclerView.Adapter<HistoryAdapter.VH>() {
            inner class VH(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
                val text: TextView = view.findViewById(android.R.id.text1)
            }
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                val v = android.view.LayoutInflater.from(parent.context).inflate(
                    android.R.layout.simple_list_item_1, parent, false
                )
                v.findViewById<TextView>(android.R.id.text1).apply {
                    textSize = 16f
                    setPadding(48, 32, 48, 32)
                }
                return VH(v)
            }
            override fun getItemCount() = items.size
            override fun onBindViewHolder(holder: VH, position: Int) {
                holder.text.text = items[position]
                holder.view.setOnClickListener {
                    onClick(items[position])
                }
            }
            fun removeAt(position: Int) {
                items.removeAt(position)
                notifyItemRemoved(position)
                saveSearchHistoryList(items)
            }
        }

        val adapter = HistoryAdapter(historyList) { selectedQuery ->
            performSearch(selectedQuery)
            alertDialog?.dismiss()
        }
        recyclerView.adapter = adapter

        // スワイプ削除
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            override fun onMove(rv: androidx.recyclerview.widget.RecyclerView, vh: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder) = false
            override fun onSwiped(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                adapter.removeAt(holder.adapterPosition)
                if (historyList.isEmpty()) {
                    alertDialog?.dismiss()
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Build dialog
        alertDialog = AlertDialog.Builder(this)
            .setTitle("検索履歴")
            .setView(frameLayout)
            .setNegativeButton("閉じる", null)
            .create()
        alertDialog.show()
    }

    // Listで上書き保存するユーティリティ
    private fun saveSearchHistoryList(list: List<String>) {
        val joined = list.joinToString("||")
        sharedPreferences.edit().putString(SEARCH_HISTORY_KEY, joined).apply()
    }

    /**
     * 現在の方向と目的地ベアリングを比較し、キャラクター位置を更新します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun updateCharacterPositionBasedOnBearing(azimuth: Float) {

        Log.d("AR_DEBUG", "方位角: $azimuth")

        if (currentLocation == null || destinationPoint == null || arOverlayView == null) return

        // 現在地から目的地へのベアリング（北=0°）
        val targetBearing = bearingToTarget(currentLocation!!, destinationPoint!!)

        // 方位差を -180〜180 に正規化（右に30°ずれていれば +30°、左なら -30°）
        var angleDiff = (targetBearing - azimuth + 540) % 360 - 180

        // 表示角度の閾値（±30°以内に目的地があるときのみ表示）
        val visibleThreshold = 30

        if (Math.abs(angleDiff) > visibleThreshold) {
            // 閾値を超えたら非表示
            arOverlayView?.visibility = View.INVISIBLE
        } else {
            // 表示 & スライド量調整
            arOverlayView?.visibility = View.VISIBLE

            val maxTranslationPx = 100.dpToPx()  // 最大スライド量（左右に最大100dpまで）
            val translationRatio = angleDiff / visibleThreshold
            val translationX = translationRatio * maxTranslationPx

            arOverlayView?.translationX = translationX.toFloat()
        }

        // 距離表示の更新
        if (currentLocation != null && destinationPoint != null) {
            val distance = distanceBetween(currentLocation!!, destinationPoint!!).toInt()
            arDistanceTextView?.text = "目的地まで ${distance}m"
            arDistanceTextView?.visibility = View.VISIBLE
        } else {
            arDistanceTextView?.visibility = View.GONE
        }
    }

}

    /**
     * グリッド中心座標(Point)から短縮グリッドIDを生成する（5mグリッド単位・起点固定）
     */
    private fun encodeBase62(n: Long): String {
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var num = n
        if (num == 0L) return "0"
        val sb = StringBuilder()
        while (num > 0) {
            sb.append(chars[(num % 62).toInt()])
            num /= 62
        }
        return sb.reverse().toString()
    }

    /**
     * グリッド中心座標(Point)から短縮グリッドIDを生成する（5mグリッド単位・起点固定）
     */
    private fun gridIdFromPoint(point: Point): String {
        // グリッド基準（日本全域カバー、経度122〜154, 緯度20〜46）
        val lat0 = 20.0
        val lng0 = 122.0
        val gridSizeMeters = 5.0
        val metersPerDegreeLat = 111132.0
        val metersPerDegreeLng = 111320.0 * Math.cos(Math.toRadians(lat0))
        val dLat = gridSizeMeters / metersPerDegreeLat
        val dLng = gridSizeMeters / metersPerDegreeLng

        val y = Math.floor((point.latitude() - lat0) / dLat).toLong()
        val x = Math.floor((point.longitude() - lng0) / dLng).toLong()

        return encodeBase62(y) + encodeBase62(x)
    }


    /**
     * グリッドID → 中心座標へ逆変換（5mグリッド単位・起点固定）
     */
    private fun decodeBase62(s: String): Long {
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var num = 0L
        for (c in s) {
            num = num * 62 + chars.indexOf(c)
        }
        return num
    }


// 検索用BottomSheetDialog表示
// ↓ MainActivityクラス内へ移動
    /**
     * 入力がグリッドIDかどうか判定
     */
    private fun isGridId(input: String): Boolean {
        // 日本国内用（6桁〜8桁、英数字）
        return input.matches(Regex("^[0-9a-zA-Z]{6,8}$"))
    }

    private fun pointFromGridId(id: String): Point {
        // グリッド基準（日本全域カバー、経度122〜154, 緯度20〜46）
        val lat0 = 20.0
        val lng0 = 122.0
        val gridSizeMeters = 5.0
        val metersPerDegreeLat = 111132.0
        val metersPerDegreeLng = 111320.0 * Math.cos(Math.toRadians(lat0)) // ← 基準緯度で固定
        val dLat = gridSizeMeters / metersPerDegreeLat
        val dLng = gridSizeMeters / metersPerDegreeLng

        // IDを2分割して復元
        val split = id.length / 2
        val y = decodeBase62(id.substring(0, split))
        val x = decodeBase62(id.substring(split))
        val lat = lat0 + y * dLat + dLat / 2.0
        val lng = lng0 + x * dLng + dLng / 2.0
        return Point.fromLngLat(lng, lat)
    }