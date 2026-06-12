package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.utils.ImageCompressionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            val base64 = ImageCompressionHelper.compressAndEncodeImage(context, uri)
            if (base64 != null) {
                viewModel.saveProfilePicture(base64,
                    onSuccess = {
                        isUploading = false
                        Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        isUploading = false
                        Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                isUploading = false
                Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = MaterialTheme.colorScheme.onBackground) },
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
        LaunchedEffect(Unit) {
            viewModel.fetchUserProfile()
        }
        
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile Picture with overlay
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(Color(0xFF2C2C2E)).clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val decodedBitmap = viewModel.profilePicBase64?.let { base64 ->
                    try {
                        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) { null }
                }
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Upload Picture", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                
                if (isUploading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap to change picture", color = Color.Gray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            var editingField by remember { mutableStateOf<String?>(null) }
            var editValue by remember { mutableStateOf("") }
            
            if (editingField != null) {
                AlertDialog(
                    onDismissRequest = { editingField = null },
                    title = { Text("Set $editingField") },
                    text = {
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = { editValue = it },
                            label = { Text("Enter your $editingField") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null && editValue.isNotBlank()) {
                                val fieldKey = when (editingField) {
                                    "Username" -> "u"
                                    "Date of Birth" -> "d"
                                    "Email" -> "e"
                                    else -> ""
                                }
                                if (fieldKey.isNotEmpty()) {
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("users").document(uid)
                                        .update(fieldKey, editValue)
                                    // Update locally
                                    when (editingField) {
                                        "Username" -> viewModel.username = editValue
                                        "Date of Birth" -> viewModel.dob = editValue
                                        "Email" -> viewModel.email = editValue
                                    }
                                }
                            }
                            editingField = null
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingField = null }) { Text("Cancel") }
                    }
                )
            }
            
            // Read-only Details (unless missing)
            ProfileField("Username", if (viewModel.username.isBlank()) "Not Set" else viewModel.username) {
                if (viewModel.username.isBlank() || viewModel.username == "Not Set") {
                    editingField = "Username"
                    editValue = ""
                }
            }
            ProfileField("Email", if (viewModel.email.isBlank()) "Not Set" else viewModel.email) {
                if (viewModel.email.isBlank() || viewModel.email == "Not Set") {
                    editingField = "Email"
                    editValue = ""
                }
            }
            ProfileField("Date of Birth", if (viewModel.dob.isBlank()) "Not Set" else viewModel.dob) {
                if (viewModel.dob.isBlank() || viewModel.dob == "Not Set") {
                    editingField = "Date of Birth"
                    editValue = ""
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            // Actions
            ProfileActionButton(
                icon = Icons.Default.DirectionsCar,
                text = "Add More Vehicles",
                onClick = { navController.navigate("vehicle_brand") }
            )
            
            ProfileActionButton(
                icon = Icons.Default.LockReset,
                text = "Change Password",
                onClick = {
                    viewModel.resetPassword(
                        onSuccess = { Toast.makeText(context, "Password reset email sent", Toast.LENGTH_SHORT).show() },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    )
                }
            )
            
            ProfileActionButton(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                text = "Logout",
                isDestructive = true,
                onClick = {
                    viewModel.logoutUser {
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, onClick: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(label, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Text(value.ifEmpty { "Not provided" }, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun ProfileActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = text, tint = if (isDestructive) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, color = if (isDestructive) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
