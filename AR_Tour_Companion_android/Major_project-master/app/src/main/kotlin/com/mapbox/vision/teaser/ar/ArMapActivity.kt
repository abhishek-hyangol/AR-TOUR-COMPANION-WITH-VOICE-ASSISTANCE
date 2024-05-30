package com.mapbox.vision.teaser.ar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.route.NavigationMapRoute
import com.mapbox.vision.teaser.BottomSheet
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.databinding.ActivityArMapBinding
import com.mapbox.vision.teaser.utils.buildNavigationOptions


class ArMapActivity : AppCompatActivity(), MapboxMap.OnMapClickListener, OnMapReadyCallback {

    companion object {
        private val TAG = ArMapActivity::class.java.simpleName
        const val ARG_RESULT_JSON_ROUTE = "ARG_RESULT_JSON_ROUTE"
    }

    private var originPoint: Point? = null

    //using ViewBinding
    private lateinit var binding: ActivityArMapBinding
    private lateinit var mapboxMap: MapboxMap
    private var mapboxNavigation: MapboxNavigation? = null

    private var destinationMarker: Marker? = null

    private var currentRoute: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var locationComponent: LocationComponent? = null

    private val locationObserver = object : LocationObserver {
        override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
            originPoint = Point.fromLngLat(
                enhancedLocation.longitude,
                enhancedLocation.latitude
            )
        }

        override fun onRawLocationChanged(rawLocation: Location) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            onBackPressed()
        }
        // To show  BottomSheet Fragment
        binding.button.setOnClickListener {
            BottomSheet().show(supportFragmentManager, "bottomSheet tag")
        }
        binding.mapView.onCreate(savedInstanceState)

        binding.startAr.setOnClickListener {
            val route = currentRoute
            if (route != null) {
                val jsonRoute = route.toJson()
                val data = Intent().apply {
                    putExtra(ARG_RESULT_JSON_ROUTE, jsonRoute)
                }
                setResult(RESULT_OK, data)
                finish()
            } else {
                Toast.makeText(this, "Route is not ready yet!", Toast.LENGTH_LONG).show()
            }
        }

        mapboxNavigation = MapboxNavigation(buildNavigationOptions())
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        binding.mapView.getMapAsync(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mapboxNavigation?.startTripSession()
        mapboxNavigation?.registerLocationObserver(locationObserver)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.stopTripSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
    }

    override fun onMapClick(destination: LatLng): Boolean {
        destinationMarker?.let(mapboxMap::removeMarker)
        destinationMarker = mapboxMap.addMarker(MarkerOptions().position(destination))

        if (originPoint == null) {
            Toast.makeText(this, "Source location is not determined yet!", Toast.LENGTH_LONG).show()
            return false
        }

        getRoute(
            origin = originPoint!!,
            destination = Point.fromLngLat(destination.longitude, destination.latitude)
        )

        binding.startAr.visibility = View.VISIBLE

        return true
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri(Style.DARK)) {
            enableLocationComponent()
        }

        mapboxMap.addOnMapClickListener(this)
    }

    private fun getRoute(origin: Point, destination: Point) {
        mapboxNavigation?.requestRoutes(
            routeOptions = RouteOptions.builder()
                .applyDefaultParams()
                .accessToken(Mapbox.getAccessToken()!!)
                .coordinates(listOf(origin, destination))
//                .profile(DirectionsCriteria.PROFILE_WALKING)         // Specifying Walking routing profile here
                .build(),
            routesRequestCallback = object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    currentRoute = routes.first()

                    // Draw the route on the map
                    if (navigationMapRoute == null) {
                        navigationMapRoute = NavigationMapRoute.Builder(
                            binding.mapView,
                            mapboxMap,
                            this@ArMapActivity,
                        )
                            .withStyle(R.style.MapboxStyleNavigationMapRoute)
                            .build()
                    } else {
                        navigationMapRoute?.updateRouteVisibilityTo(false)
                    }
                    navigationMapRoute?.addRoute(currentRoute)
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    Toast.makeText(this@ArMapActivity, "Route request canceled!", Toast.LENGTH_LONG).show()
                }

                override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                    Toast.makeText(this@ArMapActivity, "Route request failure!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        val locationComponentOptions = LocationComponentOptions.builder(this)
            .build()
        locationComponent = mapboxMap.locationComponent

        val locationComponentActivationOptions = LocationComponentActivationOptions
            .builder(this, mapboxMap.style!!)
            .locationComponentOptions(locationComponentOptions)
            .build()

        locationComponent?.let {
            it.activateLocationComponent(locationComponentActivationOptions)
            it.isLocationComponentEnabled = true
            it.cameraMode = CameraMode.TRACKING
        }
    }

    // Function to get route using latitude and longitude
    private fun getRouteFromCoordinates(latitude: Double, longitude: Double) {
        val origin = originPoint ?: return  // Ensure originPoint is not null
        val destination = Point.fromLngLat(longitude, latitude)
        val bottomSheetFragment = supportFragmentManager.findFragmentByTag("bottomSheet tag") as? BottomSheet

        getRoute(origin, destination)
        binding.startAr.visibility = View.VISIBLE

        // close bottom sheet
        bottomSheetFragment?.dismiss()
    }

    // Simulate the behavior of onMapClick by calling getRouteFromCoordinates
     fun getRouteForLocation(latitude: Double, longitude: Double) {
        getRouteFromCoordinates(latitude, longitude)
    }

}

