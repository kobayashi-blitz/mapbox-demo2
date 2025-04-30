package com.example.mapboxdemo2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mapboxdemo2.BuildConfig
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchRequestTask
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.search.ui.adapter.engines.SearchEngineUiAdapter
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.SearchResultsView
import com.mapbox.search.ui.view.place.SearchPlaceBottomSheet
import com.mapbox.search.ui.view.place.SearchPlaceBottomSheetConfiguration

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var zoomInButton: FloatingActionButton
    private lateinit var zoomOutButton: FloatingActionButton
    private lateinit var myLocationButton: FloatingActionButton
    
    private lateinit var searchEngine: SearchEngine
    private var searchRequestTask: SearchRequestTask? = null
    
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val DEFAULT_ZOOM = 14.0
    private val ZOOM_INCREMENT = 1.0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        mapView = findViewById(R.id.mapView)
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        zoomInButton = findViewById(R.id.zoomInButton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        myLocationButton = findViewById(R.id.myLocationButton)
        
        // Initialize Mapbox Search with v2.9.0 API
        searchEngine = SearchEngine.createSearchEngine(
            SearchEngineSettings(BuildConfig.MAPBOX_ACCESS_TOKEN)
        )
        
        // Initialize map with Japanese language
        initializeMap()
        
        // Set up button click listeners
        setupButtonListeners()
        
        // Set up search functionality
        setupSearch()
    }
    
    private fun initializeMap() {
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS + "?language=ja",
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    // Map style has been loaded, now we can add location component
                    if (checkLocationPermission()) {
                        enableLocationComponent()
                    } else {
                        requestLocationPermission()
                    }
                }
            }
        )
    }
    
    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun setupButtonListeners() {
        // Zoom in button
        zoomInButton.setOnClickListener {
            val currentZoom = mapView.getMapboxMap().cameraState.zoom
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .zoom(currentZoom + ZOOM_INCREMENT)
                    .build()
            )
        }
        
        // Zoom out button
        zoomOutButton.setOnClickListener {
            val currentZoom = mapView.getMapboxMap().cameraState.zoom
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .zoom(currentZoom - ZOOM_INCREMENT)
                    .build()
            )
        }
        
        // My location button
        myLocationButton.setOnClickListener {
            if (checkLocationPermission()) {
                val locationComponentPlugin = mapView.location
                locationComponentPlugin.getLastKnownLocation()?.let { location ->
                    val point = Point.fromLngLat(location.longitude, location.latitude)
                    animateCameraToPosition(point)
                } ?: run {
                    Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestLocationPermission()
            }
        }
    }
    
    private fun setupSearch() {
        // Search button click listener
        searchButton.setOnClickListener {
            performSearch(searchEditText.text.toString())
        }
        
        // Handle search when user presses enter/search on keyboard
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                performSearch(searchEditText.text.toString())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }
    
    private fun performSearch(query: String) {
        if (query.isBlank()) return
        
        // Cancel any ongoing search
        searchRequestTask?.cancel()
        
        // Get current map center or user location for search context
        val centerPoint = if (checkLocationPermission()) {
            mapView.location.getLastKnownLocation()?.let {
                Point.fromLngLat(it.longitude, it.latitude)
            } ?: mapView.getMapboxMap().cameraState.center
        } else {
            mapView.getMapboxMap().cameraState.center
        }
        
        // Create search options with proximity to current location/map center
        val options = SearchOptions.Builder()
            .proximity(centerPoint)
            .limit(5)
            .build()
        
        // Perform the search
        searchRequestTask = searchEngine.search(
            query,
            options,
            object : SearchSelectionCallback {
                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: com.mapbox.search.ResponseInfo
                ) {
                    if (suggestions.isNotEmpty()) {
                        // Select the first suggestion automatically
                        searchEngine.select(suggestions[0], this)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No results found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
                override fun onResult(
                    suggestion: SearchSuggestion,
                    result: SearchResult,
                    responseInfo: com.mapbox.search.ResponseInfo
                ) {
                    // Add marker at the result location
                    val point = result.coordinate
                    animateCameraToPosition(point)
                    
                    // Display result information
                    Toast.makeText(
                        this@MainActivity,
                        result.name,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                override fun onCategoryResult(
                    suggestion: SearchSuggestion,
                    results: List<SearchResult>,
                    responseInfo: com.mapbox.search.ResponseInfo
                ) {
                    if (results.isNotEmpty()) {
                        // Show all category results on the map
                        for (result in results) {
                            // In a real app, you would add markers for each result
                            // For simplicity, we'll just animate to the first result
                            if (results.indexOf(result) == 0) {
                                animateCameraToPosition(result.coordinate)
                            }
                        }
                    }
                }
                
                override fun onError(e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Search error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
    
    private fun animateCameraToPosition(point: Point) {
        mapView.getMapboxMap().camera.flyTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(DEFAULT_ZOOM)
                .build(),
            MapAnimationOptions.Builder()
                .duration(1000)
                .build()
        )
    }
    
    private fun enableLocationComponent() {
        val locationComponentPlugin = mapView.location
        
        // Configure the location component
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MainActivity,
                    android.R.drawable.arrow_up_float
                ),
                shadowImage = ContextCompat.getDrawable(
                    this@MainActivity,
                    android.R.drawable.arrow_up_float
                ),
                scaleExpression = null,
                opacity = 0.75f
            )
        }
        
        // Enable location updates
        locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
            // Update camera position if needed
        }
        
        locationComponentPlugin.addOnIndicatorBearingChangedListener { bearing ->
            // Update bearing if needed
        }
    }
    
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                enableLocationComponent()
            } else {
                // Permission denied
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    // Show rationale
                    Toast.makeText(
                        this,
                        R.string.permission_rationale,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // User denied with "don't ask again", show settings dialog
                    val snackbar = Snackbar.make(
                        mapView,
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE
                    )
                    snackbar.setAction(R.string.settings) {
                        // Build intent that displays the App settings screen
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            packageName,
                            null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    snackbar.show()
                }
            }
        }
    }
    
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
        // Cancel any ongoing search
        searchRequestTask?.cancel()
    }
}
