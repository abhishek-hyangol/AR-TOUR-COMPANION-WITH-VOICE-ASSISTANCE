package com.mapbox.vision.teaser.ar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.vision.VisionManager
import com.mapbox.vision.ar.VisionArManager
import com.mapbox.vision.ar.core.models.Route
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
//import com.mapbox.vision.teaser.R
//import com.mapbox.vision.teaser.databinding.ActivityArMapBinding
import com.mapbox.vision.teaser.databinding.ActivityArNavigationBinding
import com.mapbox.vision.teaser.models.ArFeature
import com.mapbox.vision.teaser.utils.buildNavigationOptions
import com.mapbox.vision.teaser.utils.getRoutePoints

class ArNavigationActivity : AppCompatActivity(), RoutesObserver {

    companion object {
        private val TAG = ArNavigationActivity::class.java.simpleName

        private const val ARG_INPUT_JSON_ROUTE = "ARG_INPUT_JSON_ROUTE"

        fun start(context: Activity, jsonRoute: String) {
            val intent = Intent(context, ArNavigationActivity::class.java).apply {
                putExtra(ARG_INPUT_JSON_ROUTE, jsonRoute)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var directionsRoute: DirectionsRoute
    private lateinit var mapboxNavigation: MapboxNavigation

    //using view binding
    private lateinit var binding: ActivityArNavigationBinding


    private var activeArFeature: ArFeature = ArFeature.LaneAndFence

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        binding = ActivityArNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jsonRoute = intent.getStringExtra(ARG_INPUT_JSON_ROUTE)
        if (jsonRoute.isNullOrEmpty()) {
            finish()
        }

        directionsRoute = DirectionsRoute.fromJson(jsonRoute)

        binding.back.setOnClickListener {
            onBackPressed()
        }

        applyArFeature()
        binding.arModeView.setOnClickListener {
            activeArFeature = activeArFeature.getNextFeature()
            applyArFeature()
        }

        mapboxNavigation = MapboxNavigation(buildNavigationOptions())
    }

    private fun applyArFeature() {
        binding.arModeView.setImageResource(activeArFeature.drawableId)
        binding.arView.setLaneVisible(activeArFeature.isLaneVisible)
        binding.arView.setFenceVisible(activeArFeature.isFenceVisible)
    }

    override fun onResume() {
        super.onResume()

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
        mapboxNavigation.setRoutes(listOf(directionsRoute))

        VisionManager.create()
        VisionManager.start()
        VisionManager.setModelPerformance(
            ModelPerformance.On(ModelPerformanceMode.FIXED, ModelPerformanceRate.LOW)
        )

        VisionArManager.create(VisionManager)
        binding.arView.setArManager(VisionArManager)
        binding.arView.onResume()

        VisionArManager.setRoute(
            Route(
                points = directionsRoute.getRoutePoints(),
                eta = directionsRoute.duration().toFloat()
            )
        )
    }

    override fun onPause() {
        super.onPause()
        binding.arView.onPause()
        VisionArManager.destroy()

        VisionManager.stop()
        VisionManager.destroy()

        mapboxNavigation.stopTripSession()
        mapboxNavigation.setRoutes(emptyList())
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        println("Routes changed ${routes.joinToString(", ")}")
        // TODO
    }
}
