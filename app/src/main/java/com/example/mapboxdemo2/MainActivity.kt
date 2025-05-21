package com.example.mapboxdemo2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
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
import android.content.res.Resources
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
import com.mapbox.maps.extension.style.sources.addSource

import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.android.gestures.MoveGestureDetector

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

import android.text.Editable
import android.text.TextWatcher

class MainActivity : AppCompatActivity() , PermissionsListener {
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
    private var currentTitleView: TextView? = null
    private var currentMenuLayout: LinearLayout? = null
    private var currentOverlayView: View? = null
    private var cancelNaviButton: ImageView? = null

    private var arOverlayView: View? = null

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

    /**
     * Activity 起動時の初期化処理を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchEditText = findViewById(R.id.searchEditText)
        clearButton = findViewById(R.id.clearButton)

        // テキスト変化でバツボタン表示制御（set up）
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                clearButton.visibility = if (!isNavigating && !s.isNullOrEmpty()) View.VISIBLE else View.GONE
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // バツボタンを押したときの処理
        clearButton.setOnClickListener {
            searchEditText.text.clear()
        }

        mapView = findViewById(R.id.mapView)
        searchButton = findViewById(R.id.searchButton)
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

        searchButton.setImageResource(R.drawable.ic_history) // ← 履歴アイコンに変更
        searchButton.setOnClickListener {
            showSearchHistoryDialog()
        }



        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val query = searchEditText.text.toString()
                performSearch(query)
                saveSearchHistory(query)
                true
            } else {
                false
            }
        }



    }



    /**
     * Mapbox 地図のスタイル設定とジェスチャー設定を行います。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun initializeMap() {
        mapView.mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            // 縮尺バーを非表示にする
            mapView.getPlugin<ScaleBarPlugin>(Plugin.MAPBOX_SCALEBAR_PLUGIN_ID)?.updateSettings {
                enabled = false
            }

            mapView.gestures.updateSettings {
                rotateEnabled = false
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
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun checkLocationPermission(): Boolean {
        return PermissionsManager.areLocationPermissionsGranted(this)
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
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(currentZoom)
                            .build()
                    )

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
        if (query.isBlank()) return
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
                    val keyword = searchEditText.text.toString().trim()

                    // 🔎 SearchResult.name にマッチする候補だけを抽出
                    val filtered = results.filter {
                        it.name.contains(keyword, ignoreCase = true)
                    }

                    // 💡 一致候補が1件でもモーダルで表示
                    if (filtered.isNotEmpty()) {
                        val sorted = sortResultsByDistance(filtered)
                        showSearchResultsModal(sorted)
                    } else {
                        // もとの results をそのままモーダル表示（fallback）
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
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(DEFAULT_ZOOM)
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
                    hideSearchUI()       // ← UIを確実に非表示

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

                    hideSearchUI()

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

        // タイトル表示用 TextView
        val hanazome = ResourcesCompat.getFont(this, R.font.hanazome)

        val titleView = android.widget.TextView(this).apply {
            text = title
            typeface = hanazome  // ← フォントを適用！
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(20, 10, 20, 10)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                bubbleWidth,  // ← バブル画像と同じ幅
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                this.topMargin = topMargin + 20

            }
        }

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
                    drawRouteLine(origin, point) // ← この関数をあとで定義
                } ?: run {
                    Toast.makeText(context, "現在地が取得できません", Toast.LENGTH_SHORT).show()
                }
            })

            addView(createMenuButton(R.drawable.location_menu_list) {
                // バブルを閉じて検索結果一覧を再表示
                removeBubbleViews()
                if (lastSearchResults.isNotEmpty()) {
                    showSearchResultsModal(lastSearchResults)
                } else {
                    Toast.makeText(context, "検索結果がありません", Toast.LENGTH_SHORT).show()
                }
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
        rootView.addView(titleView)
        rootView.addView(menuLayout)
        rootView.addView(overlayView)

        overlayView.setOnTouchListener { _, event ->
            val bubbleRect = Rect()
            val titleRect = Rect()
            val menuRect = Rect()

            imageView.getGlobalVisibleRect(bubbleRect)
            titleView.getGlobalVisibleRect(titleRect)
            menuLayout.getGlobalVisibleRect(menuRect)

            // バブル領域の結合
            bubbleRect.union(titleRect)
            bubbleRect.union(menuRect)

            // タッチが外だった場合にすべて削除
            if (!bubbleRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                (imageView.parent as? ViewGroup)?.removeView(imageView)
                (titleView.parent as? ViewGroup)?.removeView(titleView)
                (menuLayout.parent as? ViewGroup)?.removeView(menuLayout)
                (overlayView.parent as? ViewGroup)?.removeView(overlayView)
                currentBubbleView = null
                return@setOnTouchListener true
            }
            false
        }


        currentBubbleView = imageView

        currentTitleView = titleView
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
                bottomMargin = 32.dpToPx()
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
    }

    /**
     * AR オーバーレイ画像を表示します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun showArDirectionOverlay() {
        if (arOverlayView != null) return

        arOverlayView = ImageView(this).apply {
            setImageResource(R.drawable.ar_navi)
            layoutParams = FrameLayout.LayoutParams(
                160.dpToPx(), 90.dpToPx()
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 32.dpToPx()
            }
        }

        val rootView = findViewById<FrameLayout>(android.R.id.content)
        rootView.addView(arOverlayView)
    }

    /**
     * 検索 UI を非表示にします。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun hideSearchUI() {
        searchEditText.visibility = View.GONE
        searchButton.visibility = View.GONE
        clearButton.visibility = View.GONE
    }

    /**
     * 検索 UI を再表示します。
     *
     * TODO: Add more details or parameters description if needed.
     */
    private fun showSearchUI() {
        if (!isNavigating) {
            searchEditText.visibility = View.VISIBLE
            searchButton.visibility = View.VISIBLE
            clearButton.visibility = if (!isNavigating && !searchEditText.text.isNullOrEmpty()) View.VISIBLE else View.GONE
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

        showSearchUI()

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


    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

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
        // ナビ中なら検索UIを非表示にする
        if (isNavigating) {
            hideSearchUI()
        }
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

    private fun showSearchHistoryDialog() {
        val history = getSearchHistory()
        if (history.isEmpty()) {
            Toast.makeText(this, "検索履歴はありません", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("検索履歴")
            .setItems(history.toTypedArray()) { _, which ->
                val selectedQuery = history[which]
                searchEditText.setText(selectedQuery)
                performSearch(selectedQuery)
            }
            .setNegativeButton("閉じる", null)
            .show()
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
    }

}
