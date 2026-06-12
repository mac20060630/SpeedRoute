package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.StatCard
import com.example.models.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.BoundingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(navController: NavController, tripId: String, viewModel: OnboardingViewModel) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(tripId) {
        val loadedTrip = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.utils.LocalTripStorage.getTripById(context, tripId)
        }
        trip = loadedTrip
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Details", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (trip == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Trip not found.", color = Color.Gray)
            }
        } else {
            val t = trip!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Map View
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(bottom = 24.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setMultiTouchControls(true)
                                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                                
                                // Disable touch interception so user can scroll down the screen without getting stuck on map
                                setOnTouchListener { v, event ->
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                    false
                                }

                                if (t.routePoints.isNotEmpty()) {
                                    val geoPoints = t.routePoints.map { GeoPoint(it.lat, it.lng) }
                                    
                                    val polyline = Polyline()
                                    polyline.outlinePaint.color = android.graphics.Color.parseColor("#5FC9C9")
                                    polyline.outlinePaint.strokeWidth = 10f
                                    polyline.setPoints(geoPoints)
                                    overlays.add(polyline)

                                    // Add Start Marker
                                    val startMarker = Marker(this)
                                    startMarker.position = geoPoints.first()
                                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    startMarker.title = "Start"
                                    overlays.add(startMarker)

                                    // Add End Marker
                                    val endMarker = Marker(this)
                                    endMarker.position = geoPoints.last()
                                    endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    endMarker.title = "End"
                                    overlays.add(endMarker)

                                    // Zoom to bounding box
                                    post {
                                        try {
                                            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                                            // zoomToBoundingBox takes (box, withAnimation, padding)
                                            zoomToBoundingBox(boundingBox, false, 100)
                                        } catch (e: Exception) {
                                            controller.setCenter(geoPoints.first())
                                            controller.setZoom(15.0)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                // Stats Grid
                val topSpeedDisplay = if (viewModel.speedUnit == "mph") t.topSpeedKmH * 0.621371 else t.topSpeedKmH
                val distanceDisplay = if (viewModel.speedUnit == "mph") t.totalDistanceKm * 0.621371 else t.totalDistanceKm
                val distUnit = if (viewModel.speedUnit == "mph") "mi" else "km"
                val accelLabel = if (viewModel.speedUnit == "mph") "0-60 mph" else "0-100 km/h"

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(icon = Icons.Default.FlashOn, iconColor = Color(0xFFE53935), title = "Top Speed", value = "${String.format("%.0f", topSpeedDisplay)} ${viewModel.speedUnit}")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(icon = Icons.Default.SwapHoriz, iconColor = Color(0xFF66BB6A), title = "Distance", value = "${String.format("%.1f", distanceDisplay)} $distUnit")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(icon = Icons.Default.Schedule, iconColor = Color(0xFFB388FF), title = "Duration", value = "${t.durationSeconds / 60}m")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(icon = Icons.Default.Timer, iconColor = Color(0xFFE53935), title = accelLabel, value = if (t.best0To100TimeSec != null) String.format("%.1f s", t.best0To100TimeSec) else "-")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(icon = Icons.Default.TurnLeft, iconColor = Color(0xFF5C6BC0), title = "Left Turns", value = "${t.leftTurns}")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(icon = Icons.Default.TurnRight, iconColor = Color(0xFFEF5350), title = "Right Turns", value = "${t.rightTurns}")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(icon = Icons.Default.PanTool, iconColor = Color(0xFFFF9800), title = "Hard Brakes", value = "${t.hardBrakes}")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(icon = Icons.Default.ArrowUpward, iconColor = Color(0xFF66BB6A), title = "Hard Accel", value = "${t.hardAccelerations}")
                    }
                }
            }
        }
    }
}
