package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavController, currentStep: Int, totalSteps: Int = 11) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF5FC9C9),
            trackColor = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5FC9C9), disabledContainerColor = Color.DarkGray),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WelcomeScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "How do your Trips Rank?",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Let's find out.",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            ActionButton(text = "Get Started", onClick = { navController.navigate("register") })
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { navController.navigate("login") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "By continuing you're accepting our Terms of Use and Privacy Notice",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun UnitSelectScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 1)
        Spacer(modifier = Modifier.weight(1f))
        
        Text("Choose your unit", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select your preferred speed unit for the speedometer and trip tracking.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Color.DarkGray)) {
                Row {
                    Box(modifier = Modifier.clip(RoundedCornerShape(24.dp))
                        .background(if (viewModel.speedUnit == "km/h") Color.Gray else Color.Transparent)
                        .clickable { viewModel.speedUnit = "km/h" }
                        .padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text("km/h", color = if (viewModel.speedUnit == "km/h") Color.White else Color.LightGray)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(24.dp))
                        .background(if (viewModel.speedUnit == "mph") Color.Gray else Color.Transparent)
                        .clickable { viewModel.speedUnit = "mph" }
                        .padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text("mph", color = if (viewModel.speedUnit == "mph") Color.White else Color.LightGray)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = { navController.navigate("country_select") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 2)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Select your country", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("This determines which leaderboards you appear on", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        var expanded by remember { mutableStateOf(false) }
        
        Spacer(modifier = Modifier.height(32.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = "${viewModel.country.flag} ${viewModel.country.name}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C1C1E),
                    unfocusedContainerColor = Color(0xFF1C1C1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Countries.list.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text("${selectionOption.flag} ${selectionOption.name}", color = Color.Black) },
                        onClick = {
                            viewModel.country = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = { navController.navigate("location_permission") })
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionScreen(navController: NavController, viewModel: OnboardingViewModel) {
    val permissionsToRequest = mutableListOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            add(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(permissionsToRequest)
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            com.example.location.AutoTrackingManager.enableAutoTracking(context)
            navController.navigate("vehicle_type")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 3)
        Spacer(modifier = Modifier.weight(1f))
        
        Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1B3B1B)).align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF66BB6A), modifier = Modifier.size(64.dp).align(Alignment.Center))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Enable Required Permissions", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Allow location and physical activity access to track your speed, record trips, and enable auto-tracking features in the background.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Grant Permissions", onClick = {
            if (!permissionsState.allPermissionsGranted) {
                permissionsState.launchMultiplePermissionRequest()
            } else {
                com.example.location.AutoTrackingManager.enableAutoTracking(context)
                navController.navigate("vehicle_type")
            }
        })
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.TextButton(
            onClick = { navController.navigate("vehicle_type") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Skip for now", color = Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
fun VehicleTypeScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        AppTopBar(navController, 4)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("What do you drive?", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select your primary vehicle type", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                modifier = Modifier.weight(1f).aspectRatio(1f).clickable { viewModel.vehicleType = "Car" },
                colors = CardDefaults.cardColors(containerColor = if (viewModel.vehicleType == "Car") Color(0xFF2C2C2E) else Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(16.dp),
                border = if (viewModel.vehicleType == "Car") androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF5FC9C9)) else null
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = if (viewModel.vehicleType == "Car") Color.White else Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Car", color = if (viewModel.vehicleType == "Car") Color.White else Color.LightGray)
                }
            }
            Card(
                modifier = Modifier.weight(1f).aspectRatio(1f).clickable { viewModel.vehicleType = "Motorbike" },
                colors = CardDefaults.cardColors(containerColor = if (viewModel.vehicleType == "Motorbike") Color(0xFF2C2C2E) else Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(16.dp),
                border = if (viewModel.vehicleType == "Motorbike") androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF5FC9C9)) else null
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = if (viewModel.vehicleType == "Motorbike") Color.White else Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Motorbike", color = if (viewModel.vehicleType == "Motorbike") Color.White else Color.LightGray)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = {
            viewModel.vehicleBrand = if (viewModel.vehicleType == "Car") VehicleBrands.carBrands.first() else VehicleBrands.bikeBrands.first()
            viewModel.showOtherBrand = false
            navController.navigate("vehicle_brand")
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleBrandScreen(navController: NavController, viewModel: OnboardingViewModel) {
    val brands = if (viewModel.vehicleType == "Car") VehicleBrands.carBrands else VehicleBrands.bikeBrands
    var expandedBrand by remember { mutableStateOf(false) }
    var expandedModel by remember { mutableStateOf(false) }

    val models = if (viewModel.vehicleType == "Car") {
        VehicleBrands.carModels[if (viewModel.showOtherBrand) "Other" else viewModel.vehicleBrand] ?: listOf("Other")
    } else {
        VehicleBrands.bikeModels[if (viewModel.showOtherBrand) "Other" else viewModel.vehicleBrand] ?: listOf("Other")
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        AppTopBar(navController, 5)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Choose your main ride", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select the ${viewModel.vehicleType.lowercase()} you ride the most", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Brand Dropdown
        ExposedDropdownMenuBox(
            expanded = expandedBrand,
            onExpandedChange = { expandedBrand = !expandedBrand }
        ) {
            OutlinedTextField(
                value = if (viewModel.showOtherBrand) "Other" else viewModel.vehicleBrand,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expandedBrand,
                onDismissRequest = { expandedBrand = false }
            ) {
                brands.forEach { brand ->
                    DropdownMenuItem(
                        text = { Text(brand, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            if (brand == "Other") {
                                viewModel.showOtherBrand = true
                                viewModel.vehicleBrand = ""
                            } else {
                                viewModel.showOtherBrand = false
                                viewModel.vehicleBrand = brand
                            }
                            
                            // Reset model when brand changes
                            viewModel.showOtherModel = false
                            viewModel.vehicleModel = ""
                            
                            expandedBrand = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Custom Brand Input
        if (viewModel.showOtherBrand) {
            OutlinedTextField(
                value = viewModel.vehicleBrand,
                onValueChange = { viewModel.vehicleBrand = it },
                placeholder = { Text("Enter brand") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Model Dropdown
        ExposedDropdownMenuBox(
            expanded = expandedModel,
            onExpandedChange = { expandedModel = !expandedModel }
        ) {
            OutlinedTextField(
                value = if (viewModel.showOtherModel) "Other" else viewModel.vehicleModel,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
                modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expandedModel,
                onDismissRequest = { expandedModel = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            if (model == "Other") {
                                viewModel.showOtherModel = true
                                viewModel.vehicleModel = ""
                            } else {
                                viewModel.showOtherModel = false
                                viewModel.vehicleModel = model
                            }
                            expandedModel = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Custom Model Input
        if (viewModel.showOtherModel || (viewModel.showOtherBrand && viewModel.vehicleModel.isEmpty())) {
            OutlinedTextField(
                value = viewModel.vehicleModel,
                onValueChange = { viewModel.vehicleModel = it },
                placeholder = { Text("Enter model") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = { navController.navigate("add_vehicles") })
    }
}

@Composable
fun AddVehiclesScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        AppTopBar(navController, 6)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Add more vehicles?", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("You can add other vehicles you drive", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        // Selected Vehicle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (viewModel.vehicleType == "Car") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("${viewModel.vehicleBrand} ${viewModel.vehicleModel}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("Primary vehicle", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add Button
        Card(
            modifier = Modifier.fillMaxWidth().clickable { },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add another vehicle", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = { navController.navigate("speed_camera") })
    }
}

@Composable
fun SpeedCameraScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        AppTopBar(navController, 7)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Speed Camera Alerts", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Get notified before approaching speed cameras while driving.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.weight(1f))
        Text("Enable speed camera detection?", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        
        val isEnabled = viewModel.enableSpeedCameras == true
        val isDisabled = viewModel.enableSpeedCameras == false
        
        Button(
            onClick = { viewModel.enableSpeedCameras = true },
            colors = ButtonDefaults.buttonColors(containerColor = if (isEnabled) Color(0xFF5FC9C9) else Color(0xFF2C2C2E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Yes, enable", color = if (isEnabled) Color.Black else Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { viewModel.enableSpeedCameras = false },
            colors = ButtonDefaults.buttonColors(containerColor = if (isDisabled) Color(0xFF5FC9C9) else Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("No thanks", color = if (isDisabled) Color.Black else Color.White, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        ActionButton(text = "Continue", onClick = { navController.navigate("dob") }, enabled = viewModel.enableSpeedCameras != null)
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        AppTopBar(navController, 1)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Welcome Back", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Log in to track your trips", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email", color = Color.Gray) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Password", color = Color.Gray) },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
        
        if (viewModel.authError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(viewModel.authError, color = Color.Red, fontSize = 14.sp)
        }
        if (viewModel.authMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(viewModel.authMessage, color = Color(0xFF4CAF50), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (viewModel.isAuthenticating) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.primary)
        } else {
            ActionButton(
                text = "Log In", 
                onClick = { 
                    viewModel.loginUser {
                        // Clear backstack and go to main
                        navController.navigate("main") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun RegistrationScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        AppTopBar(navController, 1)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Create an Account", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Join the leaderboards and save your trips", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email", color = Color.Gray) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Password", color = Color.Gray) },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it; viewModel.isUsernameAvailable = null; viewModel.usernameCheckMessage = "" },
            label = { Text("Username", color = Color.Gray) },
            leadingIcon = { Text("@", color = Color.Gray) },
            trailingIcon = { 
                if (viewModel.isCheckingUsername) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                } else if (viewModel.isUsernameAvailable == true) {
                    Icon(Icons.Default.Check, contentDescription = "Available", tint = Color.Green)
                } else if (viewModel.isUsernameAvailable == false) {
                    Icon(Icons.Default.Close, contentDescription = "Unavailable", tint = Color.Red)
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { viewModel.checkUsernameAvailability() }),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
        
        if (viewModel.usernameCheckMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(viewModel.usernameCheckMessage, color = if (viewModel.isUsernameAvailable == true) Color.Green else Color.Red, fontSize = 14.sp)
        }

        if (viewModel.authError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(viewModel.authError, color = Color.Red, fontSize = 14.sp)
        }
        if (viewModel.authMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(viewModel.authMessage, color = Color(0xFF4CAF50), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (viewModel.isAuthenticating) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.primary)
        } else {
            ActionButton(
                text = "Continue", 
                onClick = { 
                    viewModel.registerUser {
                        navController.navigate("unit_select")
                    }
                },
                enabled = viewModel.username.isNotBlank() && viewModel.email.isNotBlank() && viewModel.password.isNotBlank()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onItemSelected(listState.firstVisibleItemIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(200.dp)
    ) {
        items(items.size + 4) { index ->
            val actualIndex = index - 2
            val isSelected = index == listState.firstVisibleItemIndex + 2
            val item = if (actualIndex in items.indices) items[actualIndex] else ""
            
            Box(
                modifier = Modifier.height(40.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    color = if (isSelected) Color.White else Color.DarkGray,
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun WheelDatePicker(
    onDateSelected: (String) -> Unit
) {
    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val days = (1..31).map { it.toString() }
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (currentYear - 100..currentYear).map { it.toString() }.reversed()

    var selectedMonth by remember { mutableStateOf(months[5]) }
    var selectedDay by remember { mutableStateOf(days[11]) }
    // Default to year 2000 for DOB, clamped to valid range
    val defaultYearIndex = (currentYear - 2000).coerceIn(0, years.size - 1)
    var selectedYear by remember { mutableStateOf(years[defaultYearIndex]) }

    LaunchedEffect(selectedMonth, selectedDay, selectedYear) {
        onDateSelected("$selectedMonth $selectedDay, $selectedYear")
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(40.dp)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WheelPicker(
                items = months,
                initialIndex = 5,
                onItemSelected = { if (it in months.indices) selectedMonth = months[it] },
                modifier = Modifier.weight(1.5f)
            )
            WheelPicker(
                items = days,
                initialIndex = 11,
                onItemSelected = { if (it in days.indices) selectedDay = days[it] },
                modifier = Modifier.weight(1f)
            )
            WheelPicker(
                items = years,
                initialIndex = defaultYearIndex,
                onItemSelected = { if (it in years.indices) selectedYear = years[it] },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DOBScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        AppTopBar(navController, 9)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("When were you born?", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.weight(1f))
        
        WheelDatePicker(onDateSelected = { viewModel.dob = it })
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = { navController.navigate("trust") })
    }
}

@Composable
fun TrustScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        AppTopBar(navController, 10)
        Spacer(modifier = Modifier.weight(1f))
        
        Box(modifier = Modifier.size(200.dp).align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(80.dp).align(Alignment.Center), tint = Color.White)
            // Imagine some circles and hands icon here
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Thank you for trusting us", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF5FC9C9))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your privacy and security matter to us.", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("We promise to always keep your personal information private and secure. No location tracking history is stored.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                Text(
                    "Read our Privacy Policy",
                    color = Color(0xFF5FC9C9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://policies.google.com/privacy"))
                        context.startActivity(intent)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue to Dashboard", onClick = {
            viewModel.completeOnboarding {
                navController.navigate("main") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        })
    }
}
