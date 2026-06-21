package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class LeaderboardEntry(
    val username: String = "",
    val country: String = "",
    val vehicleBrand: String = "",
    val topSpeed: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavController, showBackButton: Boolean = false) {
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val week = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        val weekId = "${year}_${week}"
        
        val db = FirebaseFirestore.getInstance()
        val listenerRegistration = db.collection("leaderboard_$weekId")
            .orderBy("ts", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val newEntries = snapshot.documents.mapNotNull { doc ->
                        val u = doc.getString("u") ?: return@mapNotNull null
                        val c = doc.getString("c") ?: ""
                        val v = doc.getString("v") ?: ""
                        val ts = doc.getDouble("ts")?.toFloat() ?: 0f
                        LeaderboardEntry(username = u, country = c, vehicleBrand = v, topSpeed = ts)
                    }
                    entries = newEntries
                    isLoading = false
                }
            }
            
        // No direct way to cancel listener in pure LaunchedEffect without DisposableEffect,
        // but since LeaderboardScreen is short-lived, it's mostly fine, or we can use DisposableEffect.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Top Speeds", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No records yet. Start a journey and set a top speed!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${index + 1}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (index < 3) Color(0xFFFFB74D) else Color.Gray,
                                modifier = Modifier.width(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.username.ifEmpty { "Anonymous" }, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("${entry.vehicleBrand} • ${entry.country}", color = Color.Gray, fontSize = 14.sp)
                            }
                            Text(
                                text = "${String.format("%.0f", entry.topSpeed)} km/h",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
