package com.example.mapboxdemo2

import android.view.MotionEvent

import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.DynamicAnimation

import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
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
import android.widget.Toast
import com.example.mapboxdemo2.feature.FunctionNavigation
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
import androidx.fragment.app.Fragment
import android.app.Activity

import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

import android.view.ViewOutlineProvider
import android.graphics.Outline
import kotlin.math.roundToInt
import android.graphics.Point as AndroidPoint // ★修正: android.graphics.Point に別名 'AndroidPoint' を付けてインポート

import android.widget.ProgressBar
import android.widget.Button

import android.view.LayoutInflater

import com.example.mapboxdemo2.data.db.AppDatabase

import androidx.lifecycle.lifecycleScope
import com.example.mapboxdemo2.model.MarkerData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

import androidx.appcompat.content.res.AppCompatResources
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import android.widget.ListView
import android.widget.ArrayAdapter


class MapFragment : Fragment(R.layout.fragment_map), PermissionsListener {

    private val MARKER_SOURCE_ID = "marker-source"
    private val MARKER_LAYER_ID = "marker-layer"
    private val MARKER_ICON_PROPERTY = "icon_id"
    private val MARKER_NAME_PROPERTY = "marker_name"
    private var currentMapFeatures = mutableListOf<Feature>()

    // ドロップしたマーカーの座標を保持
    private var droppedMarkerLatLng: Point? = null

    // 地図上の現在のマーカーアイコンView（バブルメニュー用）
    private var currentMarkerView: ImageView? = null

    // ホイールアイコンのドラッグ中フラグ
    private var isDraggingIcon = false // ホイールアイコンのドラッグ中フラグ

    // 写真検索用リクエストコード
    private val REQUEST_CODE_PHOTO_SEARCH = 101

    // ドロップされたマーカーの位置を画面上に再配置する
    private fun updateMarkerViewPosition() {
        val markerView = currentMarkerView ?: return
        val latLng = droppedMarkerLatLng ?: return
        val screenPoint = mapView.getMapboxMap().pixelForCoordinate(latLng)
        val markerSize = 48.dpToPx()
        (markerView.layoutParams as? FrameLayout.LayoutParams)?.apply {
            this.leftMargin = (screenPoint.x - markerSize / 2).toInt()
            this.topMargin = (screenPoint.y - markerSize / 2).toInt()
        }
        markerView.requestLayout()
    }

    // ★追加: 透明なドラッグシャドウビルダー
    private class TransparentDragShadowBuilder(view: View) : View.DragShadowBuilder(view) {
        override fun onDrawShadow(canvas: Canvas) {
            // 何も描画しない（透明にする）
        }

        override fun onProvideShadowMetrics(outMetrics: AndroidPoint, outShadowSize: AndroidPoint) { // ★修正: ここを AndroidPoint に変更
            val width: Int = view.width
            val height: Int = view.height

            outMetrics.x = width / 2
            outMetrics.y = height / 2
            outShadowSize.x = width
            outShadowSize.y = height
        }
    }

    // この位置に入れる！
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PHOTO_SEARCH && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data ?: return
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                inputStream?.use {
                    val exif = androidx.exifinterface.media.ExifInterface(it)
                    val latLong = FloatArray(2)
                    if (exif.getLatLong(latLong)) {
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
                            Toast.makeText(requireContext(), "位置情報が見つかりませんでした", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "写真の位置取得エラー: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 検索用BottomSheetDialog表示
    private fun showSearchMenuDialog() {
        val context = requireContext()
        val activity = requireActivity()
        val dialog = BottomSheetDialog(context)
        val view = activity.layoutInflater.inflate(R.layout.dialog_search_menu, null)

        // キーワードEditTextでエンター（検索）押下時の挙動を上書き
        val keywordEditText = view.findViewById<android.widget.EditText>(R.id.keywordEditText)
        keywordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = keywordEditText.text.toString().trim()
                dialog.dismiss()
                performSearch(keyword)
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
                        performSearch(items[position])
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
            performSearch(view.findViewById<android.widget.EditText>(R.id.keywordEditText).text.toString().trim())
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

    /**
     * 検索マーカー設置時に近隣の駅を検索してリスト表示
     */
    // 282行目からの searchNearbyStations メソッド全体を置き換え
    private fun searchNearbyStations(point: Point) {
        showLoading(true) // ★通信開始前にローディング表示

        val options = SearchOptions.Builder()
            .proximity(point)
            .limit(10)
            .build()

        searchEngine.search("駅", options, object : SearchSelectionCallback {
            override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                if (suggestions.isNotEmpty()) {
                    searchEngine.select(suggestions.first(), this)
                } else {
                    showLoading(false) // ★失敗したのでローディング非表示
                    showError() // ★エラー表示
                }
            }
            override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                showLoading(false) // ★成功したのでローディング非表示
                showSearchResultsModal(listOf(result))
            }
            override fun onResults(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                showLoading(false) // ★成功したのでローディング非表示
                val stationResults = results.filter { it.name.contains("駅") }
                if (stationResults.isNotEmpty()) {
                    val sorted = stationResults.sortedBy { distanceBetween(point, it.coordinate) }
                    showSearchResultsModal(sorted)
                } else {
                    showError() // ★結果が0件でもエラー表示
                }
            }
            override fun onError(e: Exception) {
                showLoading(false) // ★失敗したのでローディング非表示
                showError() // ★エラー表示
                Log.e("SearchError", "駅検索失敗", e)
            }
        })
    }
    // 現在ハイライト中のグリッド
    private var highlightedGridPolygon: com.mapbox.geojson.Polygon? = null
    // グリッドの基準起点（アプリ起動時の画面左上座標を保存）
    private var gridOrigin: Point? = null
    // 最後に表示した検索結果を保持
    private var lastSearchResults: List<SearchResult> = emptyList()

    private lateinit var overlayContainer: FrameLayout
    private lateinit var mapRootContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var errorCloseButton: Button

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var showLoadingRunnable: Runnable? = null

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

    // --- marker wheel (circleContainer) 用プロパティ ---
    private var centerX = 0f
    private var centerY = 0f
    private var initialAngle = 0f
    private var totalRotation = 0f
    private var allowedDirection = 0
    private lateinit var icons: List<ImageView>

    // 追加: ドラッグされたアイコンのfilePathを保持
    private var selectedWheelFilePath: String? = null

    // 各モードごとの初期アイコンリスト（将来DB/APIのデータでここを差し替える想定）
    // 例: wheelIconResListForRegisterは「登録」モード用、wheelIconResListForSearchは「検索」モード用、wheelIconResListForFunctionは「機能」モード用
    // 今後はこのリストをDBやAPIからユーザーごとに動的に取得し、差し替えて運用できる構造です
    private var wheelIconResListForRegister = listOf(
        R.drawable.baseline_map_24,
        R.drawable.baseline_notifications_24,
        R.drawable.baseline_person_18,
        R.drawable.baseline_shopping_cart_24,
        R.drawable.baseline_question_answer_24,
        R.drawable.baseline_notifications_24,
        R.drawable.baseline_person_18,
        R.drawable.baseline_shopping_cart_24
    )
    private var wheelIconResListForSearch = List(8) { R.drawable.marker_rsearch_facilityicon01 }
    private var wheelIconResListForFunction = List(8) { R.drawable.marker_rsearch_facilityicon02 }

    var isNavigating = false

    private val database: AppDatabase by lazy { AppDatabase.getDatabase(requireContext().applicationContext) }
    // --- marker wheel (circleContainer) 用プロパティ追加 ---
    private val viewAnnotationManager: com.mapbox.maps.viewannotation.ViewAnnotationManager by lazy { mapView.viewAnnotationManager }
    private var isWheelEngaged = false
    private var lastAngle = 0f
    private var lastTime = 0L
    private var velocity = 0f
    private var rotationStartAngle = 0f
    private var rotationStartRotation = 0f
    // --- スプラッシュ表示のための開始時刻記録用 ---
    private var splashShownTime: Long = 0L

    // --- marker mode switching properties ---
    private var markerModeIndex = 0
    // 既存: markerModes, markerIcons（今後廃止予定）
    private val markerModes = listOf("register", "search", "function")
    private val markerIcons = listOf(
        R.drawable.wheel_register,
        R.drawable.wheel_search,
        R.drawable.wheel_function
    )

    private var isGridTapAnimationInProgress = false

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

// --- marker wheel (circleContainer) 用プロパティ ---

    private fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        return x >= 0 && x <= view.width && y >= 0 && y <= view.height
    }

    private fun startSpringAnimation(view: View, currentRotation: Float) {
        val animation = SpringAnimation(view, SpringAnimation.ROTATION, currentRotation).apply {
            spring = SpringForce(currentRotation).apply {
                stiffness = SpringForce.STIFFNESS_LOW
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
        }
        animation.start()
    }

    /**
     * Activity 起動時の初期化処理を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ←★この直後に下記を追記
        val searchMarkerButton = view.findViewById<ImageButton>(R.id.searchMarkerButton)
        searchMarkerButton.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        searchMarkerButton.clipToOutline = true

        // ViewからfindViewById
        mapView = view.findViewById(R.id.mapViewFragment)
        zoomInButton = view.findViewById(R.id.zoomInButton)
        zoomOutButton = view.findViewById(R.id.zoomOutButton)
        myLocationButton = view.findViewById(R.id.myLocationButton)

        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            ApiType.SEARCH_BOX,
            SearchEngineSettings()
        )

        initializeMap()
        setupButtonListeners()
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sharedPreferences =
            requireContext().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

        // 検索ダイアログアイコンの表示＆クリックリスナー
        val searchDialogButton = view.findViewById<ImageButton>(R.id.showSearchDialogButton)
        searchDialogButton.setOnClickListener { showSearchMenuDialog() }

        // --- marker wheel (circleContainer) 移植ここから ---
        val circleContainer = view.findViewById<View>(R.id.circleContainer)

        // 中心座標のセット
        circleContainer?.post {
            centerX = circleContainer.width / 2f
            centerY = circleContainer.height / 2f
            circleContainer.rotation = 0f
            totalRotation = 0f
            rotationStartRotation = 0f
        }

        icons = listOf(
            view.findViewById<ImageView>(R.id.icon1),
            view.findViewById<ImageView>(R.id.icon2),
            view.findViewById<ImageView>(R.id.icon3),
            view.findViewById<ImageView>(R.id.icon4),
            view.findViewById<ImageView>(R.id.icon5),
            view.findViewById<ImageView>(R.id.icon6),
            view.findViewById<ImageView>(R.id.icon7),
            view.findViewById<ImageView>(R.id.icon8)
        )

        // 中央ボタンの初期化
        val centerButton = view.findViewById<ImageView>(R.id.searchMarkerButton)
        centerButton.setImageResource(markerIcons[markerModeIndex])
        // 中央ボタン画像セット直後にwheelの周囲アイコンも初期化（初回起動時にも正しい画像で揃う）
        updateWheelIcons(markerModeIndex)
        centerButton.setOnClickListener {
            markerModeIndex = (markerModeIndex + 1) % markerIcons.size
            centerButton.setImageResource(markerIcons[markerModeIndex])
            updateWheelIcons(markerModeIndex)
        }

        // --- ホイールアイコン長押しドラッグ対応 ---
        icons.forEach { icon ->
            icon.setOnLongClickListener { v ->
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                // ドラッグ元アイコンのfilePathを保存
                selectedWheelFilePath = v.getTag(R.id.tag_file_path) as? String
                val item = android.content.ClipData.Item(v.tag as? CharSequence)
                val dragData = android.content.ClipData(
                    v.tag as? CharSequence,
                    arrayOf(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN),
                    item
                )
                // ★修正: 透明なDragShadowBuilderを使用
                val shadowBuilder = TransparentDragShadowBuilder(v) // ここを変更
                isDraggingIcon = true
                // localStateにView(v)を渡す
                v.startDragAndDrop(dragData, shadowBuilder, v, 0)
                true
            }
        }

        circleContainer?.setOnTouchListener { v, event ->
            // ★中心座標が初期化されていない場合は何もしない
            if (centerX == 0f || centerY == 0f) return@setOnTouchListener true

            // ドラッグ中はホイール回転を禁止
            if (isDraggingIcon) {
                return@setOnTouchListener false
            }

            // 現在のタッチ位置から角度を計算
            val currentAngle = Math.toDegrees(
                Math.atan2(
                    (event.y - centerY).toDouble(),
                    (event.x - centerX).toDouble()
                )
            ).toFloat()

            // アイコン上からのスワイプ等でイベントが横取りされ、ACTION_DOWNを拾えなかった場合、
            // 最初のACTION_MOVEで強制的に初期化を行う
            if (event.action == MotionEvent.ACTION_MOVE && !isWheelEngaged) {
                isWheelEngaged = true // ホイール操作開始とみなす

                // 擬似的なDOWN処理（回転の初期化）
                FlingAnimation(v, DynamicAnimation.ROTATION).cancel()
                rotationStartAngle = currentAngle
                rotationStartRotation = v.rotation // この瞬間の回転角度を基準にする
                velocity = 0f
                lastAngle = currentAngle
                lastTime = System.currentTimeMillis()
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isWheelEngaged = true
                    FlingAnimation(v, DynamicAnimation.ROTATION).cancel()

                    // 【基準を保存】回転の基準となる開始時の角度とコンテナの回転量を保存
                    rotationStartAngle = currentAngle
                    rotationStartRotation = v.rotation

                    // 惰性回転用の変数を初期化
                    velocity = 0f
                    lastAngle = currentAngle
                    lastTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isWheelEngaged) return@setOnTouchListener false

                    // 【基準からの差分を計算】ACTION_DOWN時からの角度の変化量を計算
                    var angleDelta = currentAngle - rotationStartAngle

                    // 角度が-180/180度をまたぐ際のジャンプを補正
                    if (angleDelta > 180) angleDelta -= 360
                    if (angleDelta < -180) angleDelta += 360

                    // ★ 1. 角度変化のしきい値処理（小さすぎる動きは無視）
                    if (kotlin.math.abs(angleDelta) < 3f) return@setOnTouchListener true

                    // 【新しい角度を算出】差分を足し込むのではなく、開始状態からの変化で算出
                    var newRotation = rotationStartRotation + angleDelta

                    // ★ 2. スナップ（丸め）5度単位
                    newRotation = (newRotation / 3f).roundToInt() * 3f

                    // ★ 3. ローパスフィルタ（0.7:前回, 0.3:新規）
                    newRotation = totalRotation * 0.8f + newRotation * 0.2f

                    // ★ 4. 小数点以下1桁で丸め
                    newRotation = (newRotation * 10).roundToInt() / 10f

                    // 【回転を適用】コンテナとアイコンに回転を適用
                    v.rotation = newRotation
                    icons.forEach { icon -> icon.rotation = -newRotation }
                    totalRotation = newRotation

                    // 惰性回転のための角速度を計算
                    val now = System.currentTimeMillis()
                    var instantDelta = currentAngle - lastAngle
                    if (instantDelta > 180) instantDelta -= 360
                    if (instantDelta < -180) instantDelta += 360
                    val timeDelta = (now - lastTime).coerceAtLeast(1)
                    // velocity = instantDelta / timeDelta
                    velocity = velocity * 0.6f + (instantDelta / timeDelta) * 0.4f

                    lastAngle = currentAngle
                    lastTime = now
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isWheelEngaged) return@setOnTouchListener false
                    isWheelEngaged = false

                    // 惰性回転を開始
                    val fling = FlingAnimation(v, DynamicAnimation.ROTATION).apply {
                        setStartVelocity(velocity * 1000)
                        friction = 1.8f
                        addUpdateListener { _, value, _ ->
                            icons.forEach { icon -> icon.rotation = -value }
                            totalRotation = value
                        }
                    }
                    fling.start()
                    true
                }

                else -> false
            }
        }


        // --- ドラッグ＆ドロップ: マップへのドロップでバブル表示 ---
        mapView.setOnDragListener { v, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> {
                    // 仮マーカー作成（まだ置いていないので画面座標で表示）
                    // ★維持: ここで currentMarkerView を生成し、rootViewに追加する
                    val draggedIconView = event.localState as? ImageView // 長押しされた元のアイコン
                    val drawable = draggedIconView?.drawable
                    val markerSize = 48.dpToPx()
                    val rootView = requireActivity().findViewById<FrameLayout>(android.R.id.content)

                    // 既存マーカーViewがあれば消す（念のため）
                    currentMarkerView?.let { (it.parent as? ViewGroup)?.removeView(it) }

                    // 仮マーカーView生成
                    val markerView = ImageView(requireContext()).apply {
                        drawable?.let { setImageDrawable(it) } // ドラッグされたアイコンの画像を設定
                        layoutParams = FrameLayout.LayoutParams(markerSize, markerSize)
                    }
                    mapRootContainer.addView(markerView)
                    currentMarkerView = markerView // このViewがドラッグ中の上部のアイコンとなる
                    true
                }

                android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                    // ドラッグ中：指より少し上に仮マーカーを表示
                    // ★維持: currentMarkerView の位置をオフセット付きで更新する
                    val markerView = currentMarkerView ?: return@setOnDragListener false
                    val markerSize = 48.dpToPx()
                    val dragOffset = 86 // 指より上
                    val x = event.x
                    val y = event.y
                    (markerView.layoutParams as? FrameLayout.LayoutParams)?.apply {
                        leftMargin = (x - markerSize / 2).toInt()
                        topMargin = (y - markerSize / 2 - dragOffset).toInt()
                    }
                    markerView.requestLayout()
                    true
                }

                android.view.DragEvent.ACTION_DROP -> {
                    // 86pxオフセットを適用して地図座標へ変換
                    val x = event.x
                    val y = event.y - 86  // DRAG_LOCATIONの視覚的オフセットと一致させる
                    val markerPoint = mapView.getMapboxMap().coordinateForPixel(
                        com.mapbox.maps.ScreenCoordinate(x.toDouble(), y.toDouble())
                    )
                    droppedMarkerLatLng = markerPoint

                    // --- 検索モード時は近隣の駅を検索 ---
                    // 検索モードの場合のみ、近隣の駅を検索してリスト表示
                    if (markerModeIndex == 1) {
                        searchNearbyStations(markerPoint)
                    }

                    // --- 機能マーカーモード時は FunctionNavigation を呼び出す ---
                    if (markerModeIndex == 2) {
                        val iconIndex = icons.indexOfFirst { it.isPressed }
                        val action =
                            com.example.mapboxdemo2.feature.FunctionNavigation { origin, dest ->
                                drawRouteLine(origin, dest)
                            }
                        currentLocation?.let { origin ->
                            action.execute(requireContext(), markerPoint, mapOf("origin" to origin))
                        } ?: run {
                            Toast.makeText(
                                requireContext(),
                                "現在地が取得できません",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        isDraggingIcon = false
                        return@setOnDragListener true
                    }

                    // ★維持: ドロップ後もマーカーViewの画面上位置を維持（位置をリセットしない）
                    // updateMarkerViewPosition() は呼ばない（DRAG_LOCATIONで設定された位置をそのまま使う）
                    // もしドロップ後に地図の中心に吸い寄せたい場合はここで updateMarkerViewPosition() を呼ぶ

                    // 検索モード時はバブルメニューを表示しない
                    if (markerModeIndex != 1) {
                        animateCameraToPosition(markerPoint) {
                            showBubbleMarkerAt(markerPoint, "この場所")
                        }
                    } else {
                        // 検索マーカー時はリスト表示のみでOK
                        animateCameraToPosition(markerPoint)
                    }
                    isDraggingIcon = false
                    true
                }

                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    // ドラッグが終了した場合の処理
                    // ★修正: ドロップされずに終わった場合のみ currentMarkerView を削除する
                    // ドロップされた場合は currentMarkerView はそのまま地図上のマーカーとして残す
                    if (event.result == false) { // ドロップが成功しなかった場合
                        currentMarkerView?.let { (it.parent as? ViewGroup)?.removeView(it) }
                        currentMarkerView = null // 参照をクリア
                    }
                    isDraggingIcon = false
                    true
                }

                else -> true
            }
        }
        // --- marker wheel (circleContainer) 移植ここまで ---

        // オーバーレイUIの紐付けとリスナー設定
        mapRootContainer = view.findViewById(R.id.mapRootContainer)
        overlayContainer = view.findViewById<FrameLayout>(R.id.overlayContainer)
        progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        errorLayout = view.findViewById<LinearLayout>(R.id.errorLayout)
        errorCloseButton = view.findViewById<Button>(R.id.errorCloseButton)

        errorCloseButton.setOnClickListener {
            overlayContainer.visibility = View.GONE
            // 中途半端に残ったマーカーを消す
            currentMarkerView?.let { (it.parent as? ViewGroup)?.removeView(it) }
            currentMarkerView = null
        }


        // --- グリッドタップでハイライト ---
        // ↓↓↓↓ このブロック全体を置き換えます ↓↓↓↓
        mapView.getMapboxMap().addOnMapClickListener { point ->
            val zoom = mapView.getMapboxMap().cameraState.zoom

            if (zoom >= 19.0) {
                // 【5mグリッドの場合】ハイライトとバブルメニュー表示
                handle5mGridTap(point)
            } else if (zoom >= 11.0) {
                // 【50m以上のグリッドの場合】マーカー一覧表示
                handleLargeGridTap(point)
            }
            // 上記以外（ズームアウト時など）は何もしない

            false // クリックイベントを消費
        }
    }


    /**
     * ホイールUIの周囲アイコン画像をモードごとに切り替える。
     * カテゴリ0（登録）はDBから最大8件の登録マーカー画像を使う。
     * カテゴリ1・2は従来のハードコーディングリストでセット。
     */
    private fun updateWheelIcons(mode: Int) {
        when (mode) {
            0 -> {
                updateRegisterWheelIconsFromDb()
            }
            1 -> {
                setIcons(wheelIconResListForSearch)
            }
            2 -> {
                setIcons(wheelIconResListForFunction)
            }
            else -> {
                setIcons(wheelIconResListForRegister)
            }
        }
    }

    /**
     * DBから「ON（isVisible=1）」のマーカーを最大8件取得し、icons配列に反映。
     * 件数が8未満のときは残りをデフォルト画像で埋める。
     * filePathもImageViewのtagにセット。
     */
    private fun updateRegisterWheelIconsFromDb() {
        lifecycleScope.launch {
            // DBからONのマーカーを最大8件取得
            val markerList = withContext(Dispatchers.IO) {
                database.downloadedMarkerDao().getVisible().take(8)
            }
            // filePathからdrawableリソースIDを取得し、filePathもセット
            val iconResList = markerList.map { getDrawableResIdByFilePath(requireContext(), it.filePath) }
            val filePathList = markerList.map { it.filePath }
            // 8件に満たない場合はデフォルトで埋める
            val filledList = iconResList.toMutableList()
            val filledFilePathList = filePathList.toMutableList()
            while (filledList.size < 8) {
                filledList.add(R.drawable.baseline_map_24)
                filledFilePathList.add("") // 空文字
            }
            // icons配列に画像とfilePathをセット
            icons.forEachIndexed { i, iv ->
                iv.setImageResource(filledList.getOrElse(i) { R.drawable.baseline_map_24 })
                iv.setTag(R.id.tag_file_path, filledFilePathList.getOrElse(i) { "" })
            }
        }
    }

    /**
     * filePathからdrawableリソースIDを取得する
     */
    private fun getDrawableResIdByFilePath(context: Context, filePath: String): Int {
        val name = filePath.substringBeforeLast('.') // 拡張子除去
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }

    /**
     * icons配列（ImageView）に、与えられたリソースIDリストを順に反映する
     * （filePathは空でセット）
     */
    private fun setIcons(iconResList: List<Int>) {
        icons.forEachIndexed { i, iv ->
            iv.setImageResource(iconResList.getOrElse(i) { R.drawable.baseline_map_24 })
            // filePathは使わないので空文字をセット
            iv.setTag(R.id.tag_file_path, "")
        }
    }



    /**
     * Mapbox 地図のスタイル設定とジェスチャー設定を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private var splashImageView: ImageView? = null

    private fun initializeMap() {
        mapView.mapboxMap.loadStyleUri("mapbox://styles/blitz-k/cmbercg9l004a01sn6jk01cc9") { style ->

            registerMarkerIcons(style) // プロジェクト内の画像をスタイルに登録
            setupSymbolLayer(style)    // SymbolLayerを初期化

            loadAndDisplayMarkers()

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
                if (isGridTapAnimationInProgress) return@addOnCameraChangeListener
                drawGridOverlay()
                updateMarkerViewPosition()

            }

            val mainActivity = requireActivity() as? MainActivity
            if (mainActivity != null && !mainActivity.splashAlreadyShown) {
                // --- スプラッシュ画像を最前面に表示 ---
                mainActivity.splashAlreadyShown = true
                val rootView = requireActivity().findViewById<FrameLayout>(android.R.id.content)
                // スプラッシュ表示開始時刻を記録
                splashShownTime = System.currentTimeMillis()
                splashImageView = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.splash)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                rootView.addView(splashImageView)
            }

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
     */
    private fun checkLocationPermission(): Boolean {
        return PermissionsManager.areLocationPermissionsGranted(requireContext())
    }

    // --- MapView/MapboxMap初期化時にズームレベル制限を設定 ---
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
            AlertDialog.Builder(requireContext())
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
                        isGridTapAnimationInProgress = false
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
                                Toast.makeText(requireContext(), "見つかりませんでした", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(requireContext(), "候補が見つかりませんでした", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onError(e: Exception) {
                            Toast.makeText(requireContext(), "検索失敗: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "見つかりませんでした", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "候補が見つかりませんでした", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onError(e: Exception) {
                Toast.makeText(requireContext(), "検索失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    /**
     * 指定座標にカメラをアニメーション移動します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun animateCameraToPosition(point: Point, zoomLevel: Double? = null, onAnimationEnd: (() -> Unit)? = null) {
        val targetZoom = zoomLevel ?: mapView.getMapboxMap().cameraState.zoom
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(targetZoom)
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
     */
    private fun enableLocationComponent() {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_oval_puck)!!
        val bitmap = drawableToBitmap(drawable, 64)

        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true

            locationPuck = LocationPuck2D(
                topImage     = ImageHolder.from(bitmap),
                bearingImage = null,
                shadowImage  = null,
                scaleExpression = null
            )
        }

        mapView.location.puckBearing = PuckBearing.HEADING
    }

    /**
     * 現在地に一度だけカメラ移動します。
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
                        val rootView = requireActivity().findViewById<FrameLayout>(android.R.id.content)
                        rootView.removeView(it)
                        splashImageView = null
                    }, remaining)
                } else {
                    val rootView = requireActivity().findViewById<FrameLayout>(android.R.id.content)
                    rootView.removeView(it)
                    splashImageView = null
                }
            }
        }

        mapView.location.addOnIndicatorPositionChangedListener(listener)
    }

    /**
     * 現在地追従リスナーをセットアップします。
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
                    Toast.makeText(requireContext(), "📣 $instruction", Toast.LENGTH_LONG).show()
                    navigationSteps.removeAt(0)

                    if (navigationSteps.isEmpty()) {
                        Toast.makeText(requireContext(), "🎉 目的地に到着しました！", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        mapView.location.addOnIndicatorPositionChangedListener(followListener!!)
    }

    /**
     * 表示中のバブルビューをすべて削除します。
     * バブルメニューを閉じた際、マーカーアイコンも同時に非表示にする。
     */
    private fun removeBubbleViews(keepMarkerIcon: Boolean = false, clearHighlight: Boolean = true) {
        currentBubbleView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentTitleView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentMenuLayout?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentOverlayView?.let { (it.parent as? ViewGroup)?.removeView(it) }

        currentBubbleView = null
        currentTitleView = null
        currentMenuLayout = null
        currentOverlayView = null

        if (!keepMarkerIcon) {
            currentMarkerView?.let { (it.parent as? ViewGroup)?.removeView(it) }
            currentMarkerView = null
        }

        // clearHighlightがtrueの場合のみ、ハイライトを削除
        if (clearHighlight) {
            highlightedGridPolygon = null
            drawGridOverlay()
        }
    }


    /**
     * 位置情報権限のリクエストを行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun requestLocationPermission() {
        permissionsManager = PermissionsManager(this)
        permissionsManager.requestLocationPermissions(requireActivity())
    }

    /**
     * API からルートデータを取得し、地図上にルート線を描画します。
     */
    // 1339行目からの drawRouteLine メソッド全体を置き換え
    private fun drawRouteLine(origin: Point, destination: Point) {
        // UIスレッドでローディング表示
        requireActivity().runOnUiThread { showLoading(true) }

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

                // ... (geometryやstepsArrayのパース処理はそのまま) ...
                val geometry = json.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val points = mutableListOf<Point>()
                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    points.add(Point.fromLngLat(coord.getDouble(0), coord.getDouble(1)))
                }
                val routeLine = com.mapbox.geojson.LineString.fromLngLats(points)
                val routeFeature = com.mapbox.geojson.Feature.fromGeometry(routeLine)
                val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeatures(arrayOf(routeFeature))
                val stepsArray = json.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
                navigationSteps.clear()
                for (i in 0 until stepsArray.length()) {
                    val step = stepsArray.getJSONObject(i)
                    val maneuver = step.getJSONObject("maneuver")
                    val instruction = maneuver.getString("instruction")
                    val location = maneuver.getJSONArray("location")
                    val stepPoint = Point.fromLngLat(location.getDouble(0), location.getDouble(1))
                    navigationSteps.add(Pair(stepPoint, instruction))
                }

                // 成功時のUI処理
                requireActivity().runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    showLoading(false) // ★成功したのでローディング非表示

                    // ... (既存のナビゲーション開始処理) ...
                    isNavigating = true
                    requireActivity().runOnUiThread {
                        view?.findViewById<View>(R.id.circleContainer)?.visibility = View.GONE
                        view?.findViewById<View>(R.id.searchMarkerButton)?.visibility = View.GONE
                    }
                    val searchDialogButton = requireActivity().findViewById<ImageButton>(R.id.showSearchDialogButton)
                    searchDialogButton.isEnabled = false
                    searchDialogButton.alpha = 0.5f
                    destinationPoint = destination
                    removeBubbleViews()
                    val sourceId = "route-source"
                    val layerId = "route-layer"
                    val style = mapView.getMapboxMap().getStyle() ?: return@runOnUiThread
                    if (!style.styleSourceExists(sourceId)) {
                        val source = geoJsonSource(sourceId) { featureCollection(featureCollection) }
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
                    if (followListener == null) {
                        startFollowingUser()
                    }
                    showCancelNaviButton()
                    showArDirectionOverlay()
                }

            } catch (e: Exception) {
                // 失敗時のUI処理
                requireActivity().runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    showLoading(false) // ★失敗したのでローディング非表示
                    showError() // ★エラー表示
                    Log.e("RouteError", "ルート取得失敗", e)
                }
            }
        }.start()
    }

    /**
     * 権限リクエストの結果を処理します。
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions as Array<String>, grantResults)
    }

    /**
     * 権限説明が必要な場合の UI 表示を行います。
     */
    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(requireContext(), "位置情報の許可が必要です", Toast.LENGTH_LONG).show()
    }

    /**
     * 権限許可結果に応じた処理を行います。
     */
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent()
            moveToCurrentLocationOnce()

            // ★★ タイムアウト処理を追加（3秒後に位置取得できなければエラーとする）
            splashImageView?.postDelayed({
                if (splashImageView != null) {
                    // スプラッシュ画像を消してエラー表示
                    val rootView = requireActivity().findViewById<FrameLayout>(android.R.id.content)
                    rootView.removeView(splashImageView)
                    splashImageView = null
                    Toast.makeText(requireContext(), "現在地が取得できませんでした", Toast.LENGTH_LONG).show()
                }
            }, 3000L)
        } else {
            Snackbar.make(mapView, "位置情報の許可が必要です", Snackbar.LENGTH_INDEFINITE)
                .setAction("設定") {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
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

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawable.draw(canvas)
        return bitmap
    }

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

    private fun sortResultsByDistance(results: List<SearchResult>): List<SearchResult> {
        val current = currentLocation
        return if (current != null) {
            results.sortedBy { distanceBetween(current, it.coordinate) }
        } else results
    }

    private fun showSearchResultsModal(results: List<SearchResult>) {
        // 検索結果を保存
        lastSearchResults = results
        // runOnUiThreadでないが、UI操作なのでクラッシュ防止ガード追加
        if (!isAdded) return
        val dialog = BottomSheetDialog(requireContext())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_search_results, null)
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
            requireContext(),
            android.R.layout.simple_list_item_1,
            displayList
        )

        listView.setOnItemClickListener { _, _, position, _ ->
            if (!isAdded) return@setOnItemClickListener
            dialog.dismiss()
            // ★ダイアログを閉じた時にもマーカーを消す
            currentMarkerView?.let { (it.parent as? ViewGroup)?.removeView(it) }
            currentMarkerView = null
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
        // ★ダイアログ閉じた時にもマーカーを消す
        dialog.setOnDismissListener {
            currentMarkerView?.let { (it.parent as? ViewGroup)?.removeView(it) }
            currentMarkerView = null
        }
        dialog.show()
    }

    // showBubbleMarkerAt メソッド全体を置き換え
    private fun showBubbleMarkerAt(point: Point, title: String = "", showDetailInsteadOfRegister: Boolean = false) {
        followListener?.let {
            mapView.location.removeOnIndicatorPositionChangedListener(it)
            followListener = null
        }
        // 以前のバブルを消す
        // 古いバブルを消すが、ハイライトは消さないようにする
        removeBubbleViews(keepMarkerIcon = true, clearHighlight = false)

        val displayMetrics = requireContext().resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val bubbleWidth = (screenWidth * 0.8).toInt()
        val aspectRatio = 250f / 460f
        val bubbleHeight = (bubbleWidth * aspectRatio).toInt()

        val leftMargin = (screenWidth / 2) - (bubbleWidth / 2)
        val topMargin = (screenHeight / 2) - bubbleHeight - 25

        val imageView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.bubble_marker_01)
            layoutParams = FrameLayout.LayoutParams(
                bubbleWidth,
                bubbleHeight
            ).apply {
                this.leftMargin = leftMargin
                this.topMargin = topMargin
            }
        }

        val hanazome = ResourcesCompat.getFont(requireContext(), R.font.hanazome)
        val distanceText: String? = if (currentLocation != null) {
            val dist = distanceBetween(currentLocation!!, point).toInt()
            "現在地から約${dist}m"
        } else {
            null
        }
        val titleLayout = LinearLayout(requireContext()).apply {
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
        val distanceTextView = TextView(requireContext()).apply {
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
        val gridId = gridIdFromPoint(point)
        val gridIdTextView = TextView(requireContext()).apply {
            text = "（$gridId）"
            typeface = hanazome
            textSize = 15f
            setTextColor(Color.parseColor("#448aff"))
            setPadding(5, 10, 20, 10)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GridID", gridId))
                Toast.makeText(context, "グリッドIDをコピーしました", Toast.LENGTH_SHORT).show()
            }
            visibility = if (distanceText != null) View.VISIBLE else View.GONE
        }
        titleLayout.addView(distanceTextView)
        titleLayout.addView(gridIdTextView)

        val menuLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = topMargin + 110
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val size = 64.dpToPx()
            val margin = 8.dpToPx()

            fun createMenuButton(drawableId: Int, onClick: () -> Unit): ImageView {
                return ImageView(requireContext()).apply {
                    setImageResource(drawableId)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(margin, 0, margin, 0)
                    }
                    setOnClickListener { onClick() }
                }
            }

            val labelRes = if (showDetailInsteadOfRegister) R.drawable.location_menu_info else R.drawable.location_menu_regist
            addView(createMenuButton(labelRes) {
                if (showDetailInsteadOfRegister) {
                    // 詳細ダイアログを表示する処理を呼ぶ
                    // ★もしpointからid取得できる場合はidで呼ぶ。現状Pointのみ→一旦Point→id検索も可
                    lifecycleScope.launch {
                        val marker = withContext(Dispatchers.IO) {
                            // 10m以内で一番近いマーカーを取得
                            database.markerDao().getAllMarkers().minByOrNull {
                                distanceBetween(point, Point.fromLngLat(it.longitude, it.latitude))
                            }
                        }
                        if (marker != null) {
                            showMarkerDetailDialog(marker.id)
                        } else {
                            Toast.makeText(requireContext(), "この位置の登録マーカー情報がありません", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val defaultName = title.takeIf { it != "この場所" && it != "このグリッド" } ?: "マーカー"
                    val filePath = selectedWheelFilePath ?: "baseline_map_24.png"
                    showRegisterMarkerDialog(
                        defaultName = defaultName,
                        filePath = filePath,
                        lat = point.latitude(),
                        lng = point.longitude()
                    )
                    selectedWheelFilePath = null
                }
            })

            addView(createMenuButton(R.drawable.location_menu_navi) {
                currentLocation?.let { origin ->
                    drawRouteLine(origin, point)
                } ?: run {
                    Toast.makeText(context, "現在地が取得できません", Toast.LENGTH_SHORT).show()
                }
            })

            addView(createMenuButton(R.drawable.location_menu_share) {
                val id = gridIdFromPoint(point)
                val shareUrl = "https://nakamarker.com/g/$id"
                val shareText = "ナカマーカーが場所情報をお伝えします！ $shareUrl ※リンクタップでアプリが起動するよ！"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                requireActivity().startActivity(Intent.createChooser(shareIntent, "アプリで共有"))
            })
        }

        val overlayView = View(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        val rootView = requireActivity().findViewById<FrameLayout>(android.R.id.content)
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

            bubbleRect.union(titleRect)
            bubbleRect.union(menuRect)

            if (!bubbleRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                removeBubbleViews()
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

        cancelNaviButton = ImageView(requireContext()).apply {
            setImageResource(R.drawable.navi_cancel)
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 80.dpToPx()
            }

            setOnClickListener {
                stopNavigation()
            }
        }

        val rootView = requireActivity().findViewById<FrameLayout>(android.R.id.content)
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

        val rootView = requireActivity().findViewById<FrameLayout>(android.R.id.content)

        // 1. Distance TextView（背景なし、中央、topMargin 24dp）
        arDistanceTextView = TextView(requireContext()).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.BLACK)
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
        arOverlayView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ar_navi)
            layoutParams = FrameLayout.LayoutParams(
                160.dpToPx(), 90.dpToPx()
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 56.dpToPx()
            }
        }

        // 3. 背景View（キャラクター画像の最下部まで高さ自動調整）
        arBackgroundView = object : View(requireContext()) {}.apply {
            setBackgroundColor(android.graphics.Color.argb(51, 255, 255, 255)) // 20%透過白
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
    fun stopNavigation() {
        requireActivity().runOnUiThread {
            view?.findViewById<View>(R.id.circleContainer)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.searchMarkerButton)?.visibility = View.VISIBLE
        }
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
        val searchDialogButton = requireActivity().findViewById<ImageButton>(R.id.showSearchDialogButton)
        searchDialogButton.isEnabled = true
        searchDialogButton.alpha = 1.0f

        highlightedGridPolygon = null
        drawGridOverlay()

        Toast.makeText(requireContext(), "ナビゲーションを終了しました", Toast.LENGTH_SHORT).show()

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
// グリッドサイズラベルの表示・更新
    private fun showOrUpdateGridSizeLabel(gridSizeMeters: Double) {
        val rootView = view?.findViewById<FrameLayout>(R.id.mapRootContainer) ?: return
        if (gridSizeLabel == null) {
            gridSizeLabel = TextView(requireContext()).apply {
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
            val rootView = view?.findViewById<FrameLayout>(R.id.mapRootContainer)
            rootView?.removeView(label)
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
        // アプリが非表示になった際、もしナビゲーション中であれば自動的に終了させます。
        // (ダイアログでの確認ができない操作に対する安全策)
        if (isNavigating) {
            Log.d("NavLifecycle", "onPause() - ナビゲーションを自動終了")
            stopNavigation()
        }
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
            Toast.makeText(requireContext(), "検索履歴はありません", Toast.LENGTH_SHORT).show()
            return
        }

        val frameLayout = FrameLayout(requireContext())
        val recyclerView = androidx.recyclerview.widget.RecyclerView(requireContext()).apply {
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
        alertDialog = AlertDialog.Builder(requireContext())
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

    /**
     * ローディング表示を切り替えます（1.5秒の遅延表示対応版）
     */
    private fun showLoading(isLoading: Boolean) {
        // 既に実行予約されている表示処理があればキャンセル
        showLoadingRunnable?.let { handler.removeCallbacks(it) }
        showLoadingRunnable = null

        if (isLoading) {
            // ★ローディング表示(true)が呼ばれたら、1.5秒後に実行する処理を予約
            showLoadingRunnable = Runnable {
                overlayContainer.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
                overlayContainer.bringToFront() // ★★★ この行を追記 ★★★
            }
            handler.postDelayed(showLoadingRunnable!!, 1500L) // 1500ミリ秒 = 1.5秒

        } else {
            // ★ローディング非表示(false)が呼ばれたら、即座に非表示にする
            overlayContainer.visibility = View.GONE
            progressBar.visibility = View.GONE
        }
    }

    /**
     * エラー表示を切り替えます
     */
    private fun showError() {
        overlayContainer.visibility = View.VISIBLE
        errorLayout.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        overlayContainer.bringToFront() // ★★★ この行を追記 ★★★
    }

    // このメソッドをMapFragmentクラスの中に追加
    private fun showRegisterMarkerDialog(
        defaultName: String,
        filePath: String,
        lat: Double,
        lng: Double
    ) {
        // 1. BottomSheetDialogのインスタンスを作成
        val dialog = BottomSheetDialog(requireContext())

        // 2. レイアウトファイルを読み込む
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_marker_register, null)

        // 3. レイアウト内のUI部品を取得
        val imageViewMarker = dialogView.findViewById<ImageView>(R.id.imageView_marker)
        val editTextName = dialogView.findViewById<EditText>(R.id.editText_name)
        val editTextMemo = dialogView.findViewById<EditText>(R.id.editText_memo)
        val registerButton = dialogView.findViewById<Button>(R.id.button_register)
        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)

        // 4. 初期値を設定
        val iconResId = if (filePath.isNullOrEmpty()) R.drawable.baseline_map_24 else getDrawableResIdByFilePath(requireContext(), filePath)
        imageViewMarker.setImageResource(iconResId)
        editTextName.setText(defaultName)

        // 5. 「登録」ボタンが押された時の処理
        registerButton.setOnClickListener {
            val name = editTextName.text.toString()
            val memo = editTextMemo.text.toString()

            // MarkerDataを保存する処理
            val markerData = MarkerData(
                name = name,
                memo = memo,
                filePath = filePath,
                latitude = lat,
                longitude = lng,
                registrationDate = System.currentTimeMillis()
            )
            saveMarker(markerData)
            removeBubbleViews(keepMarkerIcon = false)
            // 処理が終わったらダイアログを閉じる
            dialog.dismiss()
        }

        // 6. 「キャンセル」ボタンが押された時の処理
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // 7. 作成したビューをダイアログにセットして表示
        dialog.setContentView(dialogView)
        dialog.show()
    }

    // --- 仮の保存関数（未定義の場合のみ） ---
    private fun saveMarker(markerData: com.example.mapboxdemo2.model.MarkerData) {
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. DBへの挿入（バックグラウンド処理）
            database.markerDao().insert(markerData)

            // 2. DBへの書き込み完了後、メインスレッドに処理を切り替える
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "'${markerData.name}' を保存しました", Toast.LENGTH_SHORT).show()

                // 3. メインスレッドから、マーカーの再描画を指示する
                addSingleMarkerToMap(markerData)
            }
        }
    }

    /**
     * データベースから全てのマーカーを読み込み、地図上に表示します。
     */

    /**
     * データベースから全てのマーカーを読み込み、地図上に表示します。
     * この処理はUIスレッドから呼ばれることを前提とします。
     */
    // ↓↓↓↓ このメソッド全体を、正しいコードに置き換えます ↓↓↓↓
    private fun loadAndDisplayMarkers() {
        lifecycleScope.launch {
            val markers = withContext(Dispatchers.IO) {
                database.markerDao().getAllMarkers()
            }

            // DBから読み込んだデータで、クラスが保持するFeatureリストを更新
            currentMapFeatures = markers.map { markerData ->
                val point = Point.fromLngLat(markerData.longitude, markerData.latitude)
                val properties = JsonObject().apply {
                    addProperty(MARKER_ICON_PROPERTY, markerData.filePath.substringBeforeLast('.'))
                    addProperty(MARKER_NAME_PROPERTY, markerData.name)
                }
                Feature.fromGeometry(point, properties)
            }.toMutableList()

            // 地図のデータソースを、更新されたリストで上書き
            mapView.getMapboxMap().getStyle()?.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID)?.let {
                it.featureCollection(FeatureCollection.fromFeatures(currentMapFeatures))
            }
        }
    }



    /**
     * drawableにあるマーカー画像を、Mapboxが認識できるIDでスタイルに登録する
     */
    private fun registerMarkerIcons(style: com.mapbox.maps.Style) {
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. DBから、重複しないfilePathのリストを取得
            val imageFilePaths = database.markerDao().getAllUniqueFilePaths()

            // 2. メインスレッドで、画像登録処理を実行
            withContext(Dispatchers.Main) {
                imageFilePaths.forEach { filePath ->
                    // 空のファイルパスは無視する
                    if (filePath.isEmpty()) return@forEach

                    try {
                        val imageName = filePath.substringBeforeLast('.')
                        val drawable = AppCompatResources.getDrawable(requireContext(),
                            resources.getIdentifier(imageName, "drawable", requireContext().packageName))

                        drawable?.let {
                            val bitmap = Bitmap.createBitmap(
                                it.intrinsicWidth,
                                it.intrinsicHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(bitmap)
                            it.setBounds(0, 0, canvas.width, canvas.height)
                            it.draw(canvas)
                            style.addImage(imageName, bitmap)
                        }
                    } catch (e: Exception) {
                        Log.e("RegisterIconError", "Failed to load image: $filePath", e)
                    }
                }
            }
        }
    }

    /**
     * マーカー表示用のSymbolLayerを初期化する
     */
    private fun setupSymbolLayer(style: com.mapbox.maps.Style) {
        style.addSource(geoJsonSource(MARKER_SOURCE_ID))
        style.addLayer(
            symbolLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID) {
                iconImage("{$MARKER_ICON_PROPERTY}") // {}でプロパティ名を囲む
                iconSize(0.3)
                iconAllowOverlap(true)
                iconIgnorePlacement(true)
            }
        )
    }



    /**
     * グリッドタップ時の処理
     */
    private fun handleGridTap(point: Point) {
        if (isGridTapAnimationInProgress) return
        val zoom = mapView.getMapboxMap().cameraState.zoom

        // 5mグリッド(ズームレベル19以上)の場合は、これまで通りハイライトとバブルメニュー表示
        if (zoom >= 19.0) {
            val gridCenter = calculateGridCenter(point, 5.0)
            highlightedGridPolygon = createGridPolygon(gridCenter, 5.0)

            isGridTapAnimationInProgress = true
            animateCameraToPosition(gridCenter) {
                showBubbleMarkerAt(gridCenter, "このグリッド")
                isGridTapAnimationInProgress = false
            }
            return
        }

        // 50m以上のグリッドが表示されるズームレベルか判定
        if (zoom < 13.0) {
            // ズームレベルが足りない場合はハイライトを消すだけ
            highlightedGridPolygon = null
            drawGridOverlay()
            return
        }

        // ズームレベルに応じたグリッドサイズを決定
        val gridSizeMeters = when {
            zoom >= 16.0 -> 50.0
            else -> 500.0 // 5kmグリッドも一旦500mとして扱う（お好みで調整）
        }

        // タップされたグリッドの四隅の座標を計算
        val gridBounds = calculateGridBounds(point, gridSizeMeters)

        // DBに範囲内のマーカーを問い合わせる
        lifecycleScope.launch(Dispatchers.IO) {
            val markersInGrid = database.markerDao().getMarkersInBounds(
                north = gridBounds.north,
                south = gridBounds.south,
                east = gridBounds.east,
                west = gridBounds.west
            )

            withContext(Dispatchers.Main) {
                if (markersInGrid.isNotEmpty()) {
                    // マーカーが見つかったら、一覧表示ダイアログを出す
                    showMarkersInGridDialog(markersInGrid)
                } else {
                    Toast.makeText(requireContext(), "このグリッドに登録マーカーはありません", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ...クラスの最後の部分...

    // ↓↓↓↓ ここから3つのメソッドを丸ごと追記 ↓↓↓↓

    /**
     * グリッド計算用の四隅の座標を保持するデータクラス
     */
    private data class GridBounds(val north: Double, val south: Double, val east: Double, val west: Double)

    /**
     * タップされた座標とグリッドサイズから、そのグリッドの四隅の座標を計算する
     */
    private fun calculateGridBounds(point: Point, gridSizeMeters: Double): GridBounds {
        val lat0 = 20.0
        val lng0 = 122.0
        val metersPerDegreeLat = 111132.0
        val metersPerDegreeLng = 111320.0 * Math.cos(Math.toRadians(lat0))
        val dLat = gridSizeMeters / metersPerDegreeLat
        val dLng = gridSizeMeters / metersPerDegreeLng
        val gridY = Math.floor((point.latitude() - lat0) / dLat)
        val gridX = Math.floor((point.longitude() - lng0) / dLng)

        val south = lat0 + gridY * dLat
        val north = south + dLat
        val west = lng0 + gridX * dLng
        val east = west + dLng

        return GridBounds(north, south, east, west)
    }

    /**
     * グリッド内のマーカーリストをボトムシートに表示する
     */

    private fun showMarkersInGridDialog(markers: List<MarkerData>) {
        val dialog = BottomSheetDialog(requireContext())

        // 1. 全体を包む親のレイアウトを作成 (縦並びのLinearLayout)
        val containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 32, 0, 32) // 上下に余白を追加
        }

        // 2. タイトル用のTextViewを作成
        val titleView = TextView(requireContext()).apply {
            text = "グリッド範囲登録一覧"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 32) // タイトルの下に余白
        }

        // 3. 親レイアウトにタイトルを追加
        containerLayout.addView(titleView)

        // 4. 区切り線を追加
        val divider = View(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 2) // 高さ2pxの線
            setBackgroundColor(Color.LTGRAY)
        }
        containerLayout.addView(divider)


        // 5. これまで通り、ListViewを作成
        val listView = ListView(requireContext())
        val sortedMarkers = if (currentLocation != null) {
            markers.sortedBy { distanceBetween(currentLocation!!, Point.fromLngLat(it.longitude, it.latitude)) }
        } else {
            markers
        }
        val items = sortedMarkers.map {
            val distanceStr = currentLocation?.let { loc ->
                "(${distanceBetween(loc, Point.fromLngLat(it.longitude, it.latitude)).toInt()}m)"
            } ?: ""
            "${it.name}\n${it.memo} $distanceStr"
        }
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)

        // ★★★ リストの項目がタップされた時の処理を追加 ★★★
        listView.setOnItemClickListener { _, _, position, _ ->
            // 1. タップされたマーカーの情報を取得
            val selectedMarker = sortedMarkers[position]
            val targetPoint = Point.fromLngLat(selectedMarker.longitude, selectedMarker.latitude)

            // 2. ダイアログを閉じる
            dialog.dismiss()

            // 3. マーカーの位置にズームレベル17.0で移動する
            animateCameraToPosition(targetPoint, zoomLevel = 17.0) {
                // 詳細ダイアログをid指定で直接開く
                showBubbleMarkerAt(targetPoint, selectedMarker.name, showDetailInsteadOfRegister = true)
            }
        }

        // 6. 親レイアウトにListViewを追加
        containerLayout.addView(listView)

        // ダイアログが閉じられた時の処理を設定
        dialog.setOnDismissListener {
            // ハイライト情報をクリアして、グリッドを再描画する
            highlightedGridPolygon = null
            drawGridOverlay()
        }

        // 7. 親レイアウトをダイアログにセットして表示
        dialog.setContentView(containerLayout)


        dialog.show()
    }

    /**
     * 5mグリッドがタップされた時の処理（ハイライトとバブル表示）
     */
    private fun handle5mGridTap(point: Point) {
        if (isGridTapAnimationInProgress) return

        val gridBounds = calculateGridBounds(point, 5.0)
        val centerLat = (gridBounds.south + gridBounds.north) / 2.0
        val centerLng = (gridBounds.west + gridBounds.east) / 2.0
        val gridCenter = Point.fromLngLat(centerLng, centerLat)

        highlightedGridPolygon = com.mapbox.geojson.Polygon.fromLngLats(
            listOf(
                listOf(
                    Point.fromLngLat(gridBounds.west, gridBounds.south),
                    Point.fromLngLat(gridBounds.east, gridBounds.south),
                    Point.fromLngLat(gridBounds.east, gridBounds.north),
                    Point.fromLngLat(gridBounds.west, gridBounds.north),
                    Point.fromLngLat(gridBounds.west, gridBounds.south)
                )
            )
        )
        drawGridOverlay() // ハイライトを即時反映

        isGridTapAnimationInProgress = true
        animateCameraToPosition(gridCenter) {
            showBubbleMarkerAt(gridCenter, "このグリッド")
            isGridTapAnimationInProgress = false
        }
    }

    /**
     * 50m以上のグリッドがタップされた時の処理（範囲検索と一覧表示）
     */
    /**
     * 50m以上のグリッドがタップされた時の処理（範囲検索と一覧表示）
     */
    private fun handleLargeGridTap(point: Point) {
        val zoom = mapView.getMapboxMap().cameraState.zoom

        // ↓↓↓↓ このwhenブロックを修正します ↓↓↓↓
        val gridSizeMeters = when {
            zoom >= 16.0 -> 50.0
            zoom >= 13.0 -> 500.0
            else -> 5000.0 // ★ 5000mのケースを追加
        }

        val gridBounds = calculateGridBounds(point, gridSizeMeters)

        // ハイライト用のポリゴンを作成
        highlightedGridPolygon = com.mapbox.geojson.Polygon.fromLngLats(
            listOf(
                listOf(
                    Point.fromLngLat(gridBounds.west, gridBounds.south),
                    Point.fromLngLat(gridBounds.east, gridBounds.south),
                    Point.fromLngLat(gridBounds.east, gridBounds.north),
                    Point.fromLngLat(gridBounds.west, gridBounds.north),
                    Point.fromLngLat(gridBounds.west, gridBounds.south)
                )
            )
        )
        // グリッドを再描画して、ハイライトを即時反映させる
        drawGridOverlay()

        lifecycleScope.launch(Dispatchers.IO) {
            val markersInGrid = database.markerDao().getMarkersInBounds(
                north = gridBounds.north,
                south = gridBounds.south,
                east = gridBounds.east,
                west = gridBounds.west
            )
            withContext(Dispatchers.Main) {
                if (markersInGrid.isNotEmpty()) {
                    showMarkersInGridDialog(markersInGrid)
                } else {
                    // マーカーがなければ、ハイライトも消す
                    highlightedGridPolygon = null
                    drawGridOverlay()
                    Toast.makeText(requireContext(), "このグリッドに登録マーカーはありません", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 詳細ダイアログ表示（id指定で取得）
    private fun showMarkerDetailDialog(markerId: Int) {
        lifecycleScope.launch {
            val marker = withContext(Dispatchers.IO) {
                database.markerDao().getMarkerById(markerId)
            }
            if (marker == null) {
                Toast.makeText(requireContext(), "このIDの登録マーカーが見つかりません", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_marker_info, null)
            val nameText = dialogView.findViewById<TextView>(R.id.info_marker_name)
            val memoText = dialogView.findViewById<TextView>(R.id.info_marker_memo)
            val dateText = dialogView.findViewById<TextView>(R.id.info_marker_date)
            val imageView = dialogView.findViewById<ImageView>(R.id.info_marker_image)
            val editButton = dialogView.findViewById<Button>(R.id.button_edit_marker)

            nameText.text = marker.name
            memoText.text = marker.memo
            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            dateText.text = "登録日: ${sdf.format(java.util.Date(marker.registrationDate))}"

            val imageName = marker.filePath.substringBeforeLast('.', marker.filePath)
            val resId = requireContext().resources.getIdentifier(imageName, "drawable", requireContext().packageName)
            if (resId != 0) {
                imageView.setImageResource(resId)
            } else {
                imageView.setImageResource(R.drawable.baseline_map_24)
            }

            val dialog = BottomSheetDialog(requireContext()).apply {
                setContentView(dialogView)
            }
            // 編集ボタンのクリックリスナー
            editButton?.setOnClickListener {
                dialog.dismiss()
                showEditMarkerDialog(marker)
            }
            dialog.show()
        }
    }

    // 編集ダイアログの実装
    private fun showEditMarkerDialog(markerData: MarkerData) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_regist_marker, null)
        val nameEdit = dialogView.findViewById<EditText>(R.id.edit_marker_name)
        val memoEdit = dialogView.findViewById<EditText>(R.id.edit_marker_memo)
        val imageView = dialogView.findViewById<ImageView>(R.id.edit_marker_image)
        val saveButton = dialogView.findViewById<Button>(R.id.button_save)
        val deleteButton = dialogView.findViewById<Button>(R.id.button_delete)
        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)

        nameEdit.setText(markerData.name)
        memoEdit.setText(markerData.memo)
        // 画像表示
        val imageName = markerData.filePath.substringBeforeLast('.', markerData.filePath)
        val resId = requireContext().resources.getIdentifier(imageName, "drawable", requireContext().packageName)
        if (resId != 0) {
            imageView.setImageResource(resId)
        } else {
            imageView.setImageResource(R.drawable.baseline_map_24)
        }

        val dialog = BottomSheetDialog(requireContext()).apply {
            setContentView(dialogView)
        }

        saveButton.setOnClickListener {
            val updatedMarker = markerData.copy(
                name = nameEdit.text.toString(),
                memo = memoEdit.text.toString()
            )
            lifecycleScope.launch(Dispatchers.IO) {
                database.markerDao().update(updatedMarker)
                withContext(Dispatchers.Main) {
                    loadAndDisplayMarkers()
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "マーカー情報を更新しました", Toast.LENGTH_SHORT).show()
                }
            }
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("本当に削除しますか？")
                .setPositiveButton("削除") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        database.markerDao().delete(markerData)
                        withContext(Dispatchers.Main) {
                            loadAndDisplayMarkers()
                            dialog.dismiss()
                            removeBubbleViews()
                            Toast.makeText(requireContext(), "マーカーを削除しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * 新しく保存された1件のマーカーを、既存のSymbolLayerに動的に追加する
     */
    // ↓↓↓↓ このメソッド全体を、正しいコードに置き換えます ↓↓↓↓
    /**
     * 新しく保存された1件のマーカーを、既存のSymbolLayerに動的に追加する
     */
    private fun addSingleMarkerToMap(markerData: MarkerData) {
        val style = mapView.getMapboxMap().getStyle() ?: return
        val source = style.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID) ?: return

        // 新しいマーカーのGeoJSON Featureを作成
        val point = Point.fromLngLat(markerData.longitude, markerData.latitude)
        val properties = JsonObject().apply {
            addProperty(MARKER_ICON_PROPERTY, markerData.filePath.substringBeforeLast('.'))
            addProperty(MARKER_NAME_PROPERTY, markerData.name)
        }
        val newFeature = Feature.fromGeometry(point, properties)

        // 現在のリストに、新しいFeatureを追加
        currentMapFeatures.add(newFeature)

        // 更新されたリストで、地図のデータソースを上書き
        source.featureCollection(FeatureCollection.fromFeatures(currentMapFeatures))
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
// ↓ MapFragmentクラス内へ移動
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
/**
 * タップされた座標とグリッドサイズから、そのグリッドの中心座標を計算する
 */
private fun calculateGridCenter(point: Point, gridSizeMeters: Double): Point {
    val lat0 = 20.0
    val lng0 = 122.0
    val metersPerDegreeLat = 111132.0
    val metersPerDegreeLng = 111320.0 * Math.cos(Math.toRadians(lat0))
    val dLat = gridSizeMeters / metersPerDegreeLat
    val dLng = gridSizeMeters / metersPerDegreeLng
    val gridY = Math.floor((point.latitude() - lat0) / dLat)
    val gridX = Math.floor((point.longitude() - lng0) / dLng)
    val gridLat0 = lat0 + gridY * dLat
    val gridLng0 = lng0 + gridX * dLng
    val gridLat1 = gridLat0 + dLat
    val gridLng1 = gridLng0 + dLng
    val centerLat = (gridLat0 + gridLat1) / 2.0
    val centerLng = (gridLng0 + gridLng1) / 2.0
    return Point.fromLngLat(centerLng, centerLat)
}

/**
 * グリッドの中心座標とサイズから、ハイライト表示用の四角形ポリゴンを作成する
 */
private fun createGridPolygon(gridCenter: Point, gridSizeMeters: Double): com.mapbox.geojson.Polygon {
    val lat0 = 20.0
    val lng0 = 122.0
    val metersPerDegreeLat = 111132.0
    val metersPerDegreeLng = 111320.0 * Math.cos(Math.toRadians(lat0))
    val dLat = gridSizeMeters / metersPerDegreeLat
    val dLng = gridSizeMeters / metersPerDegreeLng

    // 中心座標からグリッドのインデックスを逆算
    val gridY = Math.floor((gridCenter.latitude() - lat0) / dLat)
    val gridX = Math.floor((gridCenter.longitude() - lng0) / dLng)

    val gridLat0 = lat0 + gridY * dLat
    val gridLng0 = lng0 + gridX * dLng
    val gridLat1 = gridLat0 + dLat
    val gridLng1 = gridLng0 + dLng

    return com.mapbox.geojson.Polygon.fromLngLats(
        listOf(
            listOf(
                Point.fromLngLat(gridLng0, gridLat0),
                Point.fromLngLat(gridLng1, gridLat0),
                Point.fromLngLat(gridLng1, gridLat1),
                Point.fromLngLat(gridLng0, gridLat1),
                Point.fromLngLat(gridLng0, gridLat0)
            )
        )
    )
}

