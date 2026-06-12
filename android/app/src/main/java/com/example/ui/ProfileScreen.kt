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
import androidx.compose.material.icons.filled.TwoWheeler
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
            var showVehicleEditDialog by remember { mutableStateOf(false) }
            
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
            ProfileField("Primary Vehicle", "${if (viewModel.vehicleType == "Car") "🚗 Car" else "🏍️ Motorbike"} - ${viewModel.vehicleBrand} ${viewModel.vehicleModel}") {
                showVehicleEditDialog = true
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            if (showVehicleEditDialog) {
                VehicleEditDialog(
                    viewModel = viewModel,
                    onDismiss = { showVehicleEditDialog = false },
                    onSave = { type, brand, model ->
                        viewModel.updateVehicleDetails(type, brand, model,
                            onSuccess = {
                                showVehicleEditDialog = false
                                Toast.makeText(context, "Vehicle details updated", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
            
            // Actions
            ProfileActionButton(
                icon = if (viewModel.vehicleType == "Car") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                text = "Change Vehicle Details",
                onClick = { showVehicleEditDialog = true }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditDialog(
    viewModel: OnboardingViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(viewModel.vehicleType) }
    var selectedBrand by remember { mutableStateOf(viewModel.vehicleBrand) }
    var selectedModel by remember { mutableStateOf(viewModel.vehicleModel) }
    var showOtherBrand by remember { mutableStateOf(viewModel.vehicleBrand !in (VehicleBrands.carBrands + VehicleBrands.bikeBrands) && viewModel.vehicleBrand.isNotEmpty()) }
    var showOtherModel by remember { mutableStateOf(false) }

    val brands = if (selectedType == "Car") VehicleBrands.carBrands else VehicleBrands.bikeBrands
    val models = if (selectedType == "Car") {
        VehicleBrands.carModels[if (showOtherBrand) "Other" else selectedBrand] ?: listOf("Other")
    } else {
        VehicleBrands.bikeModels[if (showOtherBrand) "Other" else selectedBrand] ?: listOf("Other")
    }

    var expandedBrand by remember { mutableStateOf(false) }
    var expandedModel by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Vehicle Details", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vehicle Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).height(64.dp).clickable {
                            selectedType = "Car"
                            selectedBrand = VehicleBrands.carBrands.first()
                            selectedModel = ""
                            showOtherBrand = false
                            showOtherModel = false
                        },
                        colors = CardDefaults.cardColors(containerColor = if (selectedType == "Car") Color(0xFF2C2C2E) else Color(0xFF1C1C1E)),
                        border = if (selectedType == "Car") androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF5FC9C9)) else null
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = if (selectedType == "Car") Color.White else Color.LightGray)
                            Text("Car", color = if (selectedType == "Car") Color.White else Color.LightGray, fontSize = 12.sp)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).height(64.dp).clickable {
                            selectedType = "Motorbike"
                            selectedBrand = VehicleBrands.bikeBrands.first()
                            selectedModel = ""
                            showOtherBrand = false
                            showOtherModel = false
                        },
                        colors = CardDefaults.cardColors(containerColor = if (selectedType == "Motorbike") Color(0xFF2C2C2E) else Color(0xFF1C1C1E)),
                        border = if (selectedType == "Motorbike") androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF5FC9C9)) else null
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = if (selectedType == "Motorbike") Color.White else Color.LightGray)
                            Text("Motorbike", color = if (selectedType == "Motorbike") Color.White else Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }

                // Brand Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedBrand,
                    onExpandedChange = { expandedBrand = !expandedBrand }
                ) {
                    OutlinedTextField(
                        value = if (showOtherBrand) "Other" else selectedBrand,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Brand") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBrand,
                        onDismissRequest = { expandedBrand = false }
                    ) {
                        brands.forEach { brand ->
                            DropdownMenuItem(
                                text = { Text(brand) },
                                onClick = {
                                    if (brand == "Other") {
                                        showOtherBrand = true
                                        selectedBrand = ""
                                    } else {
                                        showOtherBrand = false
                                        selectedBrand = brand
                                    }
                                    selectedModel = ""
                                    showOtherModel = false
                                    expandedBrand = false
                                }
                            )
                        }
                    }
                }

                if (showOtherBrand) {
                    OutlinedTextField(
                        value = selectedBrand,
                        onValueChange = { selectedBrand = it },
                        label = { Text("Enter brand name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                // Model Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedModel,
                    onExpandedChange = { expandedModel = !expandedModel }
                ) {
                    OutlinedTextField(
                        value = if (showOtherModel) "Other" else selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedModel,
                        onDismissRequest = { expandedModel = false }
                    ) {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    if (model == "Other") {
                                        showOtherModel = true
                                        selectedModel = ""
                                    } else {
                                        showOtherModel = false
                                        selectedModel = model
                                    }
                                    expandedModel = false
                                }
                            )
                        }
                    }
                }

                if (showOtherModel || (showOtherBrand && selectedModel.isEmpty())) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        label = { Text("Enter model name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedType, selectedBrand, selectedModel) },
                enabled = selectedBrand.isNotBlank() && selectedModel.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
