package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.location.TripManager
import kotlin.math.cos
import kotlin.math.sin
import android.Manifest
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@Composable
fun MainScreen(
    mainNavController: NavController,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    viewModel: OnboardingViewModel
) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Speed, contentDescription = "Track") },
                    label = { Text("Track") },
                    selected = currentRoute == "track",
                    onClick = {
                        if (currentRoute != "track") {
                            bottomNavController.navigate("track") {
                                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map & Stats") },
                    label = { Text("Map & Stats") },
                    selected = currentRoute == "stats",
                    onClick = {
                        if (currentRoute != "stats") {
                            bottomNavController.navigate("stats") {
                                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard") },
                    label = { Text("Rankings") },
                    selected = currentRoute == "leaderboard",
                    onClick = {
                        if (currentRoute != "leaderboard") {
                            bottomNavController.navigate("leaderboard") {
                                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "track",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("track") {
                SpeedometerScreen(
                    onStartTracking = onStartTracking,
                    onStopTracking = onStopTracking
                )
            }
            composable("stats") {
                com.example.DetailedStatsScreen(
                    viewModel = viewModel
                )
            }
            composable("leaderboard") {
                LeaderboardScreen(navController = mainNavController)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeedometerScreen(
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Track your\ntrips",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 32.dp),
            lineHeight = 36.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        // Circular Gauge
        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val speed = stats.currentSpeedKmH
            val maxSpeed = 240f
            val sweepAngle = 240f
            val startAngle = 150f
            val progressAngle = (speed / maxSpeed).coerceIn(0f, 1f) * sweepAngle
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth / 2
                
                // Draw background arc with dashes
                drawArc(
                    color = Color.DarkGray.copy(alpha = 0.3f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                
                // Draw progress arc
                drawArc(
                    color = Color(0xFF4DD0E1), // Cyan color from screenshot
                    startAngle = startAngle,
                    sweepAngle = progressAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                
                // Draw dashes / tick marks
                val tickCount = 60
                val tickSweep = sweepAngle / tickCount
                for (i in 0..tickCount) {
                    val angle = startAngle + (i * tickSweep)
                    val angleRad = Math.toRadians(angle.toDouble())
                    val innerRadius = radius - strokeWidth / 2
                    val outerRadius = radius + strokeWidth / 2
                    
                    val startX = center.x + (innerRadius * cos(angleRad)).toFloat()
                    val startY = center.y + (innerRadius * sin(angleRad)).toFloat()
                    val endX = center.x + (outerRadius * cos(angleRad)).toFloat()
                    val endY = center.y + (outerRadius * sin(angleRad)).toFloat()
                    
                    drawLine(
                        color = Color(0xFF121212),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${speed.toInt()}",
                    color = Color.White,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "km/h",
                    color = Color(0xFF4DD0E1),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Stats Grid
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            val avgSpeed = if (stats.durationSeconds > 0) stats.totalDistanceKm / (stats.durationSeconds / 3600f) else 0f
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Avg Speed", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${String.format("%.0f", avgSpeed)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(" km/h", color = Color(0xFF4DD0E1), fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Top Speed", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${String.format("%.0f", stats.topSpeedKmH)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(" km/h", color = Color(0xFF4DD0E1), fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
            }
            
            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Distance", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${String.format("%.2f", stats.totalDistanceKm)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(" km", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Altitude", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${String.format("%.0f", stats.currentAltitude)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(" m", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
            }
            
            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Duration", color = Color.Gray, fontSize = 12.sp)
                    val mins = stats.durationSeconds / 60
                    val secs = stats.durationSeconds % 60
                    Text("${mins}m ${secs}s", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stopped", color = Color.Gray, fontSize = 12.sp)
                    val sMins = stats.stoppedTimeSeconds / 60
                    val sSecs = stats.stoppedTimeSeconds % 60
                    Text("${sMins}m ${sSecs}s", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (permissionsState.allPermissionsGranted) {
                    if (stats.isTracking) onStopTracking() else onStartTracking()
                } else {
                    permissionsState.launchMultiplePermissionRequest()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (stats.isTracking) Color(0xFFEF5350) else Color(0xFF66BB6A)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Icon(
                if (stats.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (stats.isTracking) "Stop Tracking" else "Start Tracking",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
