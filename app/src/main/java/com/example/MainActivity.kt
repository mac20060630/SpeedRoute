package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.LocationTrackerService
import com.example.location.TripManager
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Black) { innerPadding ->
                    val navController = rememberNavController()
                    val onboardingViewModel: OnboardingViewModel = viewModel()
                    NavHost(
                        navController = navController,
                        startDestination = "welcome",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("unit_select") { UnitSelectScreen(navController, onboardingViewModel) }
                        composable("country_select") { CountrySelectScreen(navController, onboardingViewModel) }
                        composable("location_permission") { LocationPermissionScreen(navController, onboardingViewModel) }
                        composable("vehicle_type") { VehicleTypeScreen(navController, onboardingViewModel) }
                        composable("vehicle_brand") { VehicleBrandScreen(navController, onboardingViewModel) }
                        composable("add_vehicles") { AddVehiclesScreen(navController, onboardingViewModel) }
                        composable("speed_camera") { SpeedCameraScreen(navController) }
                        composable("username") { UsernameScreen(navController, onboardingViewModel) }
                        composable("dob") { DOBScreen(navController, onboardingViewModel) }
                        composable("trust") { TrustScreen(navController) }
                        composable("dashboard") {
                            DashboardScreen(
                                onStartTracking = { startTracker() },
                                onStopTracking = { stopTracker() },
                                viewModel = onboardingViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startTracker() {
        val intent = Intent(this, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopTracker() {
        val intent = Intent(this, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.ACTION_STOP
        }
        startService(intent)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    viewModel: OnboardingViewModel
) {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)
    
    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }
    
    val stats by TripManager.stats.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Your profile is ready!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(viewModel.username.ifEmpty { "Guest" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("${viewModel.country.name} - ${viewModel.vehicleBrand} ${viewModel.vehicleModel}", color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                val speedVal = if (viewModel.speedUnit == "mph") stats.currentSpeedKmH * 0.621371 else stats.currentSpeedKmH
                Text(String.format("%.0f", speedVal), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(viewModel.speedUnit, color = Color.Gray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Map View Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(false)
                            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(15.0)
                        }
                    },
                    update = { mapView ->
                        if (stats.currentLat != null && stats.currentLng != null) {
                            val geoPoint = GeoPoint(stats.currentLat!!, stats.currentLng!!)
                            if (mapView.overlays.isEmpty() || stats.totalDistanceKm == 0f) {
                                mapView.overlays.clear()
                                val marker = Marker(mapView)
                                marker.position = geoPoint
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mapView.overlays.add(marker)
                                
                                val polyline = Polyline()
                                polyline.outlinePaint.color = android.graphics.Color.parseColor("#5FC9C9")
                                polyline.outlinePaint.strokeWidth = 10f
                                polyline.addPoint(geoPoint)
                                mapView.overlays.add(polyline)
                                
                                mapView.controller.setCenter(geoPoint)
                            } else {
                                val marker = mapView.overlays.find { it is Marker } as? Marker
                                val polyline = mapView.overlays.find { it is Polyline } as? Polyline
                                
                                marker?.position = geoPoint
                                polyline?.addPoint(geoPoint)
                                mapView.controller.animateTo(geoPoint)
                            }
                            mapView.invalidate()
                        }
                        if (!stats.isTracking && stats.totalDistanceKm == 0f) {
                             mapView.overlays.clear()
                             mapView.invalidate()
                        }
                    }
                )
                // This transparent spacer captures all touches, ensuring the parent column can scroll securely
                Spacer(modifier = Modifier.fillMaxSize().background(Color.Transparent))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                val distVal = if (viewModel.speedUnit == "mph") stats.totalDistanceKm * 0.621371 else stats.totalDistanceKm
                val distUnit = if (viewModel.speedUnit == "mph") "mi" else "km"
                StatCard(
                    icon = Icons.Default.Navigation,
                    iconColor = Color(0xFF4FC3F7),
                    title = "Total Distance",
                    value = String.format("%.1f", distVal) + " " + distUnit
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                val mins = stats.durationSeconds / 60
                StatCard(
                    icon = Icons.Default.Timer,
                    iconColor = Color(0xFFFFB74D),
                    title = "Total Duration",
                    value = "${mins}m"
                )
            }
        }
        
        val buttonText = if (stats.isTracking) "Stop Tracking" else "Start Journey"
        val buttonColor = if (stats.isTracking) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
        
        Button(
            onClick = {
                if (permissionsState.allPermissionsGranted) {
                    if (stats.isTracking) onStopTracking() else onStartTracking()
                } else {
                    permissionsState.launchMultiplePermissionRequest()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(buttonText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

