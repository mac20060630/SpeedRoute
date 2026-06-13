package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

import androidx.compose.ui.viewinterop.AndroidView

import androidx.navigation.NavController
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

import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.location.AutoTrackingManager

class MainActivity : ComponentActivity() {
    private val onboardingViewModel: com.example.ui.OnboardingViewModel by viewModels()
    private val UPDATE_REQUEST_CODE = 9001
    private var allTimeTopSpeedCache = 0f

    private fun checkForPlayUpdate() {
        val appUpdateManager = com.google.android.play.core.appupdate.AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val appUpdateManager = com.google.android.play.core.appupdate.AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkForPlayUpdate()
        
        // Try to enable auto-tracking (it will safely return if permissions are missing)
        AutoTrackingManager.enableAutoTracking(this)
        
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val allTrips = com.example.utils.LocalTripStorage.loadAllTrips(this@MainActivity)
            val maxSpeed = allTrips.maxOfOrNull { it.topSpeedKmH }?.toFloat() ?: 0f
            allTimeTopSpeedCache = maxSpeed
            
            // Re-upload max speed to leaderboard immediately to restore any lost record
            var retries = 0
            while (onboardingViewModel.username.isBlank() && retries < 10) {
                kotlinx.coroutines.delay(500)
                retries++
            }
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null && onboardingViewModel.username.isNotBlank() && allTimeTopSpeedCache > 0f) {
                 val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                 val userRecord = hashMapOf(
                     "u" to onboardingViewModel.username,
                     "c" to onboardingViewModel.country.name,
                     "v" to onboardingViewModel.vehicleBrand,
                     "ts" to allTimeTopSpeedCache,
                     "t" to System.currentTimeMillis()
                 )
                 db.collection("leaderboard").document(user.uid)
                     .set(userRecord, com.google.firebase.firestore.SetOptions.merge())
            }
        }

        lifecycleScope.launch {
            TripManager.stats
                .map { it.topSpeedKmH }
                .distinctUntilChanged()
                .collect { topSpeed ->
                    if (TripManager.stats.value.isTracking && topSpeed > 0) {
                        if (topSpeed > allTimeTopSpeedCache) {
                            allTimeTopSpeedCache = topSpeed
                        }
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val user = auth.currentUser
                        if (user != null && onboardingViewModel.username.isNotBlank()) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val userRecord = hashMapOf(
                                "u" to onboardingViewModel.username,
                                "c" to onboardingViewModel.country.name,
                                "v" to onboardingViewModel.vehicleBrand,
                                "ts" to allTimeTopSpeedCache,
                                "t" to System.currentTimeMillis()
                            )
                            db.collection("leaderboard").document(user.uid)
                                .set(userRecord, com.google.firebase.firestore.SetOptions.merge())
                        }
                    }
                }
        }

        setContent {
            val onboardingViewModel: com.example.ui.OnboardingViewModel = this.onboardingViewModel
            MyApplicationTheme(darkTheme = onboardingViewModel.isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
                    val navController = rememberNavController()
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val startDest = if (auth.currentUser != null) "main" else "welcome"
                    
                    if (auth.currentUser != null) {
                        LaunchedEffect(Unit) { onboardingViewModel.fetchUserProfile() }
                    }
                    
                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("login") { com.example.ui.LoginScreen(navController, onboardingViewModel) }
                        composable("register") { com.example.ui.RegistrationScreen(navController, onboardingViewModel) }
                        composable("unit_select") { UnitSelectScreen(navController, onboardingViewModel) }
                        composable("country_select") { CountrySelectScreen(navController, onboardingViewModel) }
                        composable("location_permission") { LocationPermissionScreen(navController, onboardingViewModel) }
                        composable("vehicle_type") { VehicleTypeScreen(navController, onboardingViewModel) }
                        composable("vehicle_brand") { VehicleBrandScreen(navController, onboardingViewModel) }
                        composable("add_vehicles") { AddVehiclesScreen(navController, onboardingViewModel) }
                        composable("speed_camera") { SpeedCameraScreen(navController, onboardingViewModel) }
                        composable("dob") { DOBScreen(navController, onboardingViewModel) }
                        composable("trust") { TrustScreen(navController, onboardingViewModel) }
                        composable("main") {
                            com.example.ui.MainScreen(
                                mainNavController = navController,
                                onStartTracking = { startTracker() },
                                onStopTracking = { stopTracker(onboardingViewModel) },
                                viewModel = onboardingViewModel
                            )
                        }
                        composable("leaderboard") { LeaderboardScreen(navController) }
                        composable("profile") { com.example.ui.ProfileScreen(navController, onboardingViewModel) }
                        composable("trip_history") { com.example.ui.TripHistoryScreen(navController, onboardingViewModel) }
                        composable("trip_detail/{tripId}") { backStackEntry ->
                            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                            com.example.ui.TripDetailScreen(navController, tripId, onboardingViewModel)
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

    private fun stopTracker(viewModel: OnboardingViewModel) {
        val intent = Intent(this, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.ACTION_STOP
        }
        startService(intent)
        
        // Save to Firestore
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val stats = TripManager.stats.value
            
            // Save to Leaderboard
            if (viewModel.username.isNotBlank()) {
                val finalMaxSpeed = maxOf(stats.topSpeedKmH, allTimeTopSpeedCache)
                allTimeTopSpeedCache = finalMaxSpeed
                val userRecord = hashMapOf(
                    "u" to viewModel.username,
                    "c" to viewModel.country.name,
                    "v" to viewModel.vehicleBrand,
                    "ts" to finalMaxSpeed,
                    "t" to System.currentTimeMillis()
                )
                db.collection("leaderboard")
                    .document(user.uid)
                    .set(userRecord, com.google.firebase.firestore.SetOptions.merge())
            }

            // Save Trip History locally instead of Firestore
            val tripId = java.util.UUID.randomUUID().toString()
            val trip = com.example.models.Trip(
                id = tripId,
                timestamp = System.currentTimeMillis(),
                durationSeconds = stats.durationSeconds,
                totalDistanceKm = stats.totalDistanceKm.toDouble(),
                topSpeedKmH = stats.topSpeedKmH.toDouble(),
                best0To60TimeSec = stats.best0To60TimeSec?.toDouble(),
                best0To100TimeSec = stats.best0To100TimeSec?.toDouble(),
                leftTurns = stats.leftTurns,
                rightTurns = stats.rightTurns,
                hardBrakes = stats.brakeEvents,
                stoppedTimeSeconds = stats.stoppedTimeSeconds,
                maxAcceleration = stats.maxAcceleration.toDouble(),
                maxDeceleration = stats.maxDeceleration.toDouble(),
                peakGForce = stats.peakGForce.toDouble(),
                topCornerSpeedKmH = stats.topCornerSpeedKmH.toDouble(),
                totalStops = stats.totalStops,
                laneChanges = stats.laneChanges,
                routePoints = stats.routePoints
            )
            com.example.utils.LocalTripStorage.saveTrip(this, trip)
        }
        TripManager.stopTracking()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DetailedStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel,
    onTotalTripsClick: () -> Unit = {}
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
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your profile is ready!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { viewModel.isDarkTheme = !viewModel.isDarkTheme }) {
                Icon(
                    if (viewModel.isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(viewModel.username.ifEmpty { "Guest" }, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("${viewModel.country.name} - ${viewModel.vehicleBrand} ${viewModel.vehicleModel}", color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                val speedVal = if (viewModel.speedUnit == "mph") stats.currentSpeedKmH * 0.621371 else stats.currentSpeedKmH
                Text(String.format("%.0f", speedVal), color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(viewModel.speedUnit, color = Color.Gray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Map View Card
        var isAutoFollowing by remember { mutableStateOf(true) }
        var mapViewRef by remember { mutableStateOf<MapView?>(null) }

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
                    factory = { ctx ->
                        MapView(ctx).apply {
                            mapViewRef = this
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            maxZoomLevel = 22.0
                            minZoomLevel = 4.0
                            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(16.0)
                            // Request that the parent does not intercept touch events on the map
                            setOnTouchListener { v, event ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                // If the user touches the map, stop auto-following so they can pan freely
                                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                                    isAutoFollowing = false
                                }
                                false
                            }
                        }
                    },
                    update = { mapView ->
                        if (!stats.isTracking && stats.totalDistanceKm == 0f) {
                            val p = mapView.overlays.find { it is Polyline }
                            if (p != null) {
                                mapView.overlays.remove(p)
                            }
                        }

                        if (stats.currentLat != null && stats.currentLng != null) {
                            val geoPoint = GeoPoint(stats.currentLat!!, stats.currentLng!!)
                            
                            var marker = mapView.overlays.find { it is Marker } as? Marker
                            if (marker == null) {
                                marker = Marker(mapView)
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Current Location"
                                mapView.overlays.add(marker)
                            }
                            marker.position = geoPoint

                            if (stats.isTracking) {
                                var polyline = mapView.overlays.find { it is Polyline } as? Polyline
                                if (polyline == null) {
                                    polyline = Polyline()
                                    polyline.outlinePaint.color = android.graphics.Color.parseColor("#5FC9C9")
                                    polyline.outlinePaint.strokeWidth = 10f
                                    mapView.overlays.add(polyline)
                                }
                                polyline.addPoint(geoPoint)
                            }
                            
                            if (isAutoFollowing) {
                                mapView.controller.animateTo(geoPoint)
                            }
                            
                            mapView.invalidate()
                        }
                    }
                )

                // "My Location" Floating Action Button
                SmallFloatingActionButton(
                    onClick = { 
                        isAutoFollowing = true 
                        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                        try {
                            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                                        mapViewRef?.controller?.setZoom(16.0)
                                        mapViewRef?.controller?.animateTo(geoPoint)
                                        if (!stats.isTracking) {
                                            TripManager.processLocation(location)
                                        }
                                    } else if (stats.currentLat != null && stats.currentLng != null) {
                                        val geoPoint = GeoPoint(stats.currentLat!!, stats.currentLng!!)
                                        mapViewRef?.controller?.setZoom(16.0)
                                        mapViewRef?.controller?.animateTo(geoPoint)
                                    }
                                }
                        } catch (e: SecurityException) {
                            if (stats.currentLat != null && stats.currentLng != null) {
                                val geoPoint = GeoPoint(stats.currentLat!!, stats.currentLng!!)
                                mapViewRef?.controller?.setZoom(16.0)
                                mapViewRef?.controller?.animateTo(geoPoint)
                            }
                        }
                    },
                    containerColor = if (isAutoFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isAutoFollowing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center Map")
                }
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
        
        Button(
            onClick = {
                val speedToShare = if (viewModel.speedUnit == "mph") stats.topSpeedKmH * 0.621371 else stats.topSpeedKmH
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "I just hit a top speed of ${String.format("%.1f", speedToShare)} ${viewModel.speedUnit} on my ${viewModel.vehicleBrand} using Speed Route! 🚀")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Top Speed"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- NEW ADVANCED STATS SECTION ---
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.Pause, iconColor = Color(0xFFB388FF), title = "Stopped Time", value = "${stats.stoppedTimeSeconds / 60}m")
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.Layers, iconColor = Color(0xFF81C784), title = "Total Trips", value = "${stats.totalTrips}", onClick = onTotalTripsClick)
            }
        }
        
        val topSpeedDisplay = if (viewModel.speedUnit == "mph") stats.topSpeedKmH * 0.621371f else stats.topSpeedKmH
        WideStatCard(
            icon = Icons.Default.FlashOn, iconColor = Color(0xFFE53935),
            title = "Top Speed", value = String.format("%.0f", topSpeedDisplay), unit = viewModel.speedUnit
        )
        
        val accel100Label = if (viewModel.speedUnit == "mph") "Best 0-62 mph time" else "Best 0-100 km/h time"
        WideStatCard(
            icon = Icons.Default.Timer, iconColor = Color(0xFFE53935),
            title = accel100Label, value = if (stats.best0To100TimeSec != null) String.format("%.1f s", stats.best0To100TimeSec) else "-"
        )

        val accel60Label = if (viewModel.speedUnit == "mph") "Best 0-37 mph time" else "Best 0-60 km/h time"
        WideStatCard(
            icon = Icons.Default.Timer, iconColor = Color(0xFFE53935),
            title = accel60Label, value = if (stats.best0To60TimeSec != null) String.format("%.1f s", stats.best0To60TimeSec) else "-"
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.TurnLeft, iconColor = Color(0xFF5C6BC0), title = "Left Turns", value = "${stats.leftTurns}")
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.TurnRight, iconColor = Color(0xFFEF5350), title = "Right Turns", value = "${stats.rightTurns}")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.PanTool, iconColor = Color(0xFFFF9800), title = "Brake Events", value = "${stats.brakeEvents}")
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.SwapHoriz, iconColor = Color(0xFF66BB6A), title = "Lane Changes", value = "${stats.laneChanges}")
            }
        }
        
        TurnPreferenceBar(leftTurns = stats.leftTurns, rightTurns = stats.rightTurns)
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.ArrowDownward, iconColor = Color(0xFFEF5350), title = "Max Deceleration", value = "${String.format("%.1f", stats.maxDeceleration)} m/s²")
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.ArrowUpward, iconColor = Color(0xFF66BB6A), title = "Max Acceleration", value = "${String.format("%.1f", stats.maxAcceleration)} m/s²")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.Refresh, iconColor = Color(0xFFFF9800), title = "Peak G-Force", value = "${String.format("%.2f", stats.peakGForce)} G")
            }
            Box(modifier = Modifier.weight(1f)) {
                val cornerSpeedDisplay = if (viewModel.speedUnit == "mph") stats.topCornerSpeedKmH * 0.621371f else stats.topCornerSpeedKmH
                StatCard(icon = Icons.Default.Refresh, iconColor = Color(0xFF29B6F6), title = "Top Corner Speed", value = "${String.format("%.0f", cornerSpeedDisplay)} ${viewModel.speedUnit}")
            }
        }
        
        Text("More Stats", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.DirectionsCar, iconColor = Color.Gray, title = "Total Trips", value = "${stats.totalTrips}", onClick = onTotalTripsClick)
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(icon = Icons.Default.Stop, iconColor = Color.Gray, title = "Total Stops", value = "${stats.totalStops}")
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                val avgLengthRaw = if (stats.totalTrips > 0) stats.allTimeDistanceKm / stats.totalTrips else 0f
                val avgLengthDisplay = if (viewModel.speedUnit == "mph") avgLengthRaw * 0.621371f else avgLengthRaw
                val avgLengthUnit = if (viewModel.speedUnit == "mph") "mi" else "km"
                StatCard(icon = Icons.Default.SwapHoriz, iconColor = Color.Gray, title = "Avg Trip Length", value = "${String.format("%.1f", avgLengthDisplay)} $avgLengthUnit")
            }
            Box(modifier = Modifier.weight(1f)) {
                val allTimeMins = stats.totalAllTimeDurationSeconds / 60
                StatCard(icon = Icons.Default.Schedule, iconColor = Color.Gray, title = "Total Duration", value = "${allTimeMins}m")
            }
        }
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, title: String, value: String, onClick: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WideStatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, title: String, value: String, unit: String = "") {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.Gray, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(unit, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }
    }
}

@Composable
fun TurnPreferenceBar(leftTurns: Int, rightTurns: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Turn Preference", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            val total = leftTurns + rightTurns
            val leftPct = if (total > 0) (leftTurns.toFloat() / total) * 100f else 50f
            val rightPct = if (total > 0) (rightTurns.toFloat() / total) * 100f else 50f
            
            Row(modifier = Modifier.fillMaxWidth().height(32.dp).background(Color.Transparent)) {
                Box(modifier = Modifier.weight(leftPct).fillMaxHeight()
                    .background(Color(0xFF5C6BC0), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                    contentAlignment = Alignment.Center) {
                    Text("${String.format("%.1f", leftPct)}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(rightPct).fillMaxHeight()
                    .background(Color(0xFFEF5350), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)),
                    contentAlignment = Alignment.Center) {
                    Text("${String.format("%.1f", rightPct)}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Left", color = Color.Gray, fontSize = 12.sp)
                Text("Right", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

