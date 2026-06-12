package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
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
    
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    androidx.activity.compose.BackHandler(enabled = currentRoute != "track") {
        bottomNavController.navigate("track") {
            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    
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
                    onStopTracking = onStopTracking,
                    viewModel = viewModel,
                    onProfileClick = { mainNavController.navigate("profile") },
                    onViewLeaderboard = {
                        bottomNavController.navigate("leaderboard") {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("stats") {
                com.example.DetailedStatsScreen(
                    viewModel = viewModel,
                    onTotalTripsClick = { mainNavController.navigate("trip_history") }
                )
            }
            composable("leaderboard") {
                LeaderboardScreen(navController = mainNavController, showBackButton = false)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeedometerScreen(
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    viewModel: OnboardingViewModel,
    onProfileClick: () -> Unit = {},
    onViewLeaderboard: () -> Unit = {}
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
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
        viewModel.checkNotifications()
    }

    val stats by TripManager.stats.collectAsState()
    
    // Convert speed based on user's unit preference
    val displaySpeed = if (viewModel.speedUnit == "mph") stats.currentSpeedKmH * 0.621371f else stats.currentSpeedKmH
    val displayTopSpeed = if (viewModel.speedUnit == "mph") stats.topSpeedKmH * 0.621371f else stats.topSpeedKmH
    val displayDistance = if (viewModel.speedUnit == "mph") stats.totalDistanceKm * 0.621371f else stats.totalDistanceKm
    val distUnit = if (viewModel.speedUnit == "mph") "mi" else "km"
    val speedUnit = viewModel.speedUnit
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(32.dp))
            }
            Text(
                text = "Track your\ntrips",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp).align(Alignment.Center),
                lineHeight = 36.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            // Notification Bell
            var showNotificationsDialog by remember { mutableStateOf(false) }
            val hasNotifications = viewModel.isNewVersionAvailable || viewModel.isHighScoreBeaten
            
            IconButton(
                onClick = { showNotificationsDialog = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box {
                    Icon(
                        imageVector = if (hasNotifications) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = if (hasNotifications) Color(0xFF5FC9C9) else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                    if (hasNotifications) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Red, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
            
            if (showNotificationsDialog) {
                NotificationsDialog(
                    viewModel = viewModel,
                    onDismiss = { showNotificationsDialog = false },
                    onViewLeaderboard = onViewLeaderboard
                )
            }
        }
        
        // Circular Gauge
        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val speed = displaySpeed
            val maxSpeed = if (viewModel.speedUnit == "mph") 150f else 240f
            val sweepAngle = 240f
            val startAngle = 150f
            val progressAngle = (speed / maxSpeed).coerceIn(0f, 1f) * sweepAngle
            
            val onBackgroundColor = MaterialTheme.colorScheme.onBackground
            val backgroundColor = MaterialTheme.colorScheme.background
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth / 2
                
                // Draw background arc with dashes
                drawArc(
                    color = onBackgroundColor.copy(alpha = 0.15f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                
                // Draw progress arc
                drawArc(
                    color = Color(0xFF4DD0E1),
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
                        color = backgroundColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${speed.toInt()}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = speedUnit,
                    color = Color(0xFF4DD0E1),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Stats Grid
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            val avgSpeed = if (stats.durationSeconds > 0) {
                val rawAvg = stats.totalDistanceKm / (stats.durationSeconds / 3600f)
                if (viewModel.speedUnit == "mph") rawAvg * 0.621371f else rawAvg
            } else 0f
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Avg Speed", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(String.format("%.0f", avgSpeed), color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(" $speedUnit", color = Color(0xFF4DD0E1), fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Top Speed", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(String.format("%.0f", displayTopSpeed), color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(" $speedUnit", color = Color(0xFF4DD0E1), fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
            }
            
            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
            
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Distance", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(String.format("%.2f", displayDistance), color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(" $distUnit", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Altitude", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(String.format("%.0f", stats.currentAltitude), color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                    Text("${mins}m ${secs}s", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stopped", color = Color.Gray, fontSize = 12.sp)
                    val sMins = stats.stoppedTimeSeconds / 60
                    val sSecs = stats.stoppedTimeSeconds % 60
                    Text("${sMins}m ${sSecs}s", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Permission status message
        if (!permissionsState.allPermissionsGranted) {
            Text(
                text = "⚠️ Location permission required for tracking",
                color = Color(0xFFFFB74D),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
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

@Composable
fun NotificationsDialog(
    viewModel: OnboardingViewModel,
    onDismiss: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notifications", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val notificationsList = mutableListOf<@Composable () -> Unit>()

                if (viewModel.isNewVersionAvailable) {
                    notificationsList.add {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("New Version Released", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Update to Speed Route V1.1 is now available with new features and stability fixes.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        com.example.utils.AppUpdater.downloadAndInstallApk(context, viewModel.updateApkUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End).height(36.dp)
                                ) {
                                    Text("Download", fontSize = 12.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                if (viewModel.isHighScoreBeaten) {
                    notificationsList.add {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("High Score Beaten!", fontWeight = FontWeight.Bold, color = Color(0xFFEF5350), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Someone has beaten your top speed of ${String.format("%.0f", viewModel.userTopSpeed)} km/h with ${String.format("%.0f", viewModel.globalMaxSpeed)} km/h! Reclaim your rank now.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onViewLeaderboard,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End).height(36.dp)
                                ) {
                                    Text("View Rankings", fontSize = 12.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                if (notificationsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No new notifications.", color = Color.Gray)
                    }
                } else {
                    notificationsList.forEach { notification ->
                        notification()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
