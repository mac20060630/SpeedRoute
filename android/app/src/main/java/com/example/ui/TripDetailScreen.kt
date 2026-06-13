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
import androidx.compose.ui.unit.sp
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

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

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
            ) {
                // Map View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setMultiTouchControls(true)
                                setMaxZoomLevel(17.0) // Prevent extreme zoom for short trips
                                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                                
                                // Dark mode tiles approximation
                                overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f, // red
                                        0f, -1f, 0f, 0f, 255f, // green
                                        0f, 0f, -1f, 0f, 255f, // blue
                                        0f, 0f, 0f, 1f, 0f     // alpha
                                    )
                                ))

                                setOnTouchListener { v, event ->
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                    false
                                }

                                if (t.routePoints.isNotEmpty()) {
                                    val geoPoints = t.routePoints.map { GeoPoint(it.lat, it.lng) }
                                    
                                    for (i in 0 until t.routePoints.size - 1) {
                                        val p1 = t.routePoints[i]
                                        val p2 = t.routePoints[i+1]
                                        val segment = Polyline()
                                        val avgSpeed = (p1.speedKmh + p2.speedKmh) / 2f
                                        segment.outlinePaint.color = when {
                                            avgSpeed < 40 -> android.graphics.Color.parseColor("#5FC9C9")
                                            avgSpeed < 80 -> android.graphics.Color.parseColor("#4CAF50")
                                            avgSpeed < 150 -> android.graphics.Color.parseColor("#FFC107")
                                            else -> android.graphics.Color.parseColor("#F44336")
                                        }
                                        segment.outlinePaint.strokeWidth = 12f
                                        segment.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                        segment.setPoints(listOf(GeoPoint(p1.lat, p1.lng), GeoPoint(p2.lat, p2.lng)))
                                        overlays.add(segment)
                                    }

                                    // Add End Marker only
                                    val endMarker = Marker(this)
                                    endMarker.position = geoPoints.last()
                                    endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    endMarker.title = "End"
                                    overlays.add(endMarker)

                                    // Zoom to bounding box
                                    post {
                                        try {
                                            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                                            zoomToBoundingBox(boundingBox, false, 250)
                                            if (zoomLevelDouble > 17.0) {
                                                controller.setZoom(17.0)
                                            }
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121212), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(16.dp)
                ) {
                    SpeedDistributionCard(t)
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Grid mapped to Travel Time style card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("TRIP OVERVIEW", color = Color.Gray, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val topSpeedDisplay = if (viewModel.speedUnit == "mph") t.topSpeedKmH * 0.621371 else t.topSpeedKmH
                            val distanceDisplay = if (viewModel.speedUnit == "mph") t.totalDistanceKm * 0.621371 else t.totalDistanceKm
                            val distUnit = if (viewModel.speedUnit == "mph") "mi" else "km"
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(String.format("%.0f", topSpeedDisplay), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    Text("Top Speed (${viewModel.speedUnit})", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(String.format("%.1f", distanceDisplay), color = Color(0xFF4CAF50), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    Text("Distance ($distUnit)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${t.durationSeconds / 60}m", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    Text("Duration", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Secondary stats
                    val topCornerDisplay = if (viewModel.speedUnit == "mph") t.topCornerSpeedKmH * 0.621371 else t.topCornerSpeedKmH
                    val cornerSpeedLabel = "Top Corner (${viewModel.speedUnit})"

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.PanTool, iconColor = Color(0xFFFF9800), title = "Hard Brakes", value = "${t.hardBrakes}")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.ArrowUpward, iconColor = Color(0xFF66BB6A), title = "Hard Accel", value = "${t.hardAccelerations}")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.TurnLeft, iconColor = Color(0xFF29B6F6), title = "Left Turns", value = "${t.leftTurns}")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.TurnRight, iconColor = Color(0xFF29B6F6), title = "Right Turns", value = "${t.rightTurns}")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.Timer, iconColor = Color(0xFFEC407A), title = "Stopped Time", value = "${t.stoppedTimeSeconds}s")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.StopCircle, iconColor = Color(0xFFEC407A), title = "Total Stops", value = "${t.totalStops}")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.Speed, iconColor = Color(0xFFFFCA28), title = "0-100 Time", value = t.best0To100TimeSec?.let { String.format("%.1fs", it) } ?: "N/A")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.DirectionsCar, iconColor = Color(0xFFAB47BC), title = cornerSpeedLabel, value = String.format("%.1f", topCornerDisplay))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.AutoMirrored.Filled.TrendingUp, iconColor = Color(0xFF66BB6A), title = "Max Accel", value = String.format("%.1f m/s²", t.maxAcceleration))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.AutoMirrored.Filled.TrendingDown, iconColor = Color(0xFFFF7043), title = "Max Decel", value = String.format("%.1f m/s²", t.maxDeceleration))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.Bolt, iconColor = Color(0xFF26A69A), title = "Peak G-Force", value = String.format("%.2f G", t.peakGForce))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(icon = Icons.Default.SwapHoriz, iconColor = Color(0xFF8D6E63), title = "Lane Changes", value = "${t.laneChanges}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedDistributionCard(trip: Trip) {
    val totalPoints = trip.routePoints.size.coerceAtLeast(1)
    val cyanCount = trip.routePoints.count { it.speedKmh < 40f }
    val greenCount = trip.routePoints.count { it.speedKmh in 40f..80f }
    val yellowCount = trip.routePoints.count { it.speedKmh in 80f..150f }
    val redCount = trip.routePoints.count { it.speedKmh > 150f }

    val cyanPct = (cyanCount * 100f / totalPoints)
    val greenPct = (greenCount * 100f / totalPoints)
    val yellowPct = (yellowCount * 100f / totalPoints)
    val redPct = (redCount * 100f / totalPoints)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("SPEED DISTRIBUTION", color = Color.Gray, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Progress Bar
            Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                val totalWidth = size.width
                val cornerRadius = 6.dp.toPx()
                var currentX = 0f

                fun drawSegment(pct: Float, color: Color) {
                    if (pct <= 0f) return
                    val segmentWidth = (pct / 100f) * totalWidth
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(currentX, 0f),
                        size = androidx.compose.ui.geometry.Size(segmentWidth - 4.dp.toPx(), size.height), // -4dp for padding
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                    )
                    currentX += segmentWidth
                }

                drawSegment(cyanPct, Color(0xFF5FC9C9))
                drawSegment(greenPct, Color(0xFF4CAF50))
                drawSegment(yellowPct, Color(0xFFFFC107))
                drawSegment(redPct, Color(0xFFF44336))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legends
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendItem(Color(0xFF5FC9C9), "< 40 km/h", "${cyanPct.toInt()}%")
                LegendItem(Color(0xFF4CAF50), "40-80 km/h", "${greenPct.toInt()}%")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendItem(Color(0xFFFFC107), "80-150 km/h", "${yellowPct.toInt()}%")
                LegendItem(Color(0xFFF44336), "> 150 km/h", "${redPct.toInt()}%")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, percentage: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(140.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(percentage, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
    }
}
