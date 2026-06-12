package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
            ActionButton(text = "Get Started", onClick = { navController.navigate("unit_select") })
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
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
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
        Text("Enable GPS Location Access", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Allow location access to track your speed, record trips, and provide accurate speedometer readings.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Enable Location", onClick = {
            if (!locationPermissionState.status.isGranted) {
                locationPermissionState.launchPermissionRequest()
            } else {
                navController.navigate("vehicle_type")
            }
        })
    }
}

@Composable
fun VehicleTypeScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 4)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("What do you drive?", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 5)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Choose your main ride", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select the ${viewModel.vehicleType.lowercase()} you ride the most", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = if (viewModel.showOtherBrand) "Other" else viewModel.vehicleBrand,
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
                brands.forEach { brand ->
                    DropdownMenuItem(
                        text = { Text(brand, color = Color.Black) },
                        onClick = {
                            if (brand == "Other") {
                                viewModel.showOtherBrand = true
                                viewModel.vehicleBrand = ""
                            } else {
                                viewModel.showOtherBrand = false
                                viewModel.vehicleBrand = brand
                            }
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        if (viewModel.showOtherBrand) {
            OutlinedTextField(
                value = viewModel.vehicleBrand,
                onValueChange = { viewModel.vehicleBrand = it },
                placeholder = { Text("Enter brand") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C1C1E),
                    unfocusedContainerColor = Color(0xFF1C1C1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = viewModel.vehicleModel,
            onValueChange = { viewModel.vehicleModel = it },
            placeholder = { Text("Model (e.g. Discover 135)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1C1C1E),
                unfocusedContainerColor = Color(0xFF1C1C1E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = { navController.navigate("add_vehicles") })
    }
}

@Composable
fun AddVehiclesScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 6)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Add more vehicles?", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("You can add other vehicles you drive", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        // Selected Vehicle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5FC9C9))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (viewModel.vehicleType == "Car") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("${viewModel.vehicleBrand} ${viewModel.vehicleModel}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Primary vehicle", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add Button
        Card(
            modifier = Modifier.fillMaxWidth().clickable { },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF5FC9C9))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add another vehicle", color = Color(0xFF5FC9C9), fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = { navController.navigate("speed_camera") })
    }
}

@Composable
fun SpeedCameraScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 7)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Speed Camera Alerts", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("Get notified before approaching speed cameras while driving.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.weight(1f))
        Text("Enable speed camera detection?", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { /* enable */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Yes, enable", color = Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { /* disable */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("No thanks", color = Color.White, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        ActionButton(text = "Continue", onClick = { navController.navigate("username") })
    }
}

@Composable
fun UsernameScreen(navController: NavController, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 8)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Choose your username", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text("This is how you'll appear on the leaderboards", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(32.dp))
        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Color.White).align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(64.dp).align(Alignment.Center), tint = Color.Black)
        }
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it; viewModel.isUsernameAvailable = null; viewModel.usernameCheckMessage = "" },
            leadingIcon = { Text("@", color = Color.Gray) },
            trailingIcon = { 
                if (viewModel.isCheckingUsername) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF5FC9C9))
                } else if (viewModel.isUsernameAvailable == true) {
                    Icon(Icons.Default.Check, contentDescription = "Available", tint = Color.Green)
                } else if (viewModel.isUsernameAvailable == false) {
                    Icon(Icons.Default.Close, contentDescription = "Unavailable", tint = Color.Red)
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { viewModel.checkUsernameAvailability() }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1C1C1E),
                unfocusedContainerColor = Color(0xFF1C1C1E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
        
        if (viewModel.usernameCheckMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(viewModel.usernameCheckMessage, color = if (viewModel.isUsernameAvailable == true) Color.Green else Color.Red, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(
            text = "Continue", 
            onClick = { navController.navigate("dob") },
            enabled = viewModel.isUsernameAvailable == true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DOBScreen(navController: NavController, viewModel: OnboardingViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = Calendar.getInstance().timeInMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val formatter = SimpleDateFormat("MMM dd yyyy", Locale.getDefault())
                        viewModel.dob = formatter.format(Date(it))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 9)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("When were you born?", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Transparent).clickable { showDatePicker = true }, contentAlignment = Alignment.Center) {
            Text(viewModel.dob.ifEmpty { "Select Date" }, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xFF2C2C2E)).padding(horizontal = 32.dp, vertical = 8.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue", onClick = { navController.navigate("trust") })
    }
}

@Composable
fun TrustScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        AppTopBar(navController, 10)
        Spacer(modifier = Modifier.weight(1f))
        
        Box(modifier = Modifier.size(200.dp).align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(80.dp).align(Alignment.Center), tint = Color.White)
            // Imagine some circles and hands icon here
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Thank you for trusting us", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
                Text("We promise to always keep your personal information private and secure.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        ActionButton(text = "Continue to Dashboard", onClick = { navController.navigate("dashboard") {
            popUpTo("welcome") { inclusive = true }
        } })
    }
}
