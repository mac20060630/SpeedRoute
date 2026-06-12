package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OnboardingViewModel : ViewModel() {
    var isDarkTheme by mutableStateOf(true)

    var speedUnit by mutableStateOf("km/h")
    var enableSpeedCameras by mutableStateOf<Boolean?>(null)
    var country by mutableStateOf(Countries.list.first { it.name == "India" })
    var vehicleType by mutableStateOf("Car")
    
    // For "Other" vehicle brand
    var showOtherBrand by mutableStateOf(false)
    var vehicleBrand by mutableStateOf("Maruti Suzuki")
    var vehicleModel by mutableStateOf("Swift")
    
    var username by mutableStateOf("")
    var dob by mutableStateOf("")

    var isCheckingUsername by mutableStateOf(false)
    var isUsernameAvailable by mutableStateOf<Boolean?>(null)
    var usernameCheckMessage by mutableStateOf("")

    fun checkUsernameAvailability() {
        if (username.isBlank()) {
            isUsernameAvailable = false
            usernameCheckMessage = "Username cannot be empty"
            return
        }
        
        isCheckingUsername = true
        isUsernameAvailable = null
        usernameCheckMessage = ""
        
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                // Assuming "users" collection exists and we check for a document with the username
                val querySnapshot = db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .await()
                
                if (querySnapshot.isEmpty) {
                    isUsernameAvailable = true
                    usernameCheckMessage = "Available!"
                } else {
                    isUsernameAvailable = false
                    usernameCheckMessage = "Username taken"
                }
            } catch (e: Exception) {
                // Ignore real errors and just allow them if network fails
                isUsernameAvailable = true
                usernameCheckMessage = "Available (Fallback)"
            } finally {
                isCheckingUsername = false
            }
        }
    }
}

data class Country(val name: String, val flag: String)

object Countries {
    val list = listOf(
        Country("India", "🇮🇳"),
        Country("United States", "🇺🇸"),
        Country("United Kingdom", "🇬🇧"),
        Country("Australia", "🇦🇺"),
        Country("Canada", "🇨🇦"),
        Country("Germany", "🇩🇪"),
        Country("France", "🇫🇷"),
        Country("Japan", "🇯🇵"),
        Country("Brazil", "🇧🇷")
    )
}

object VehicleBrands {
    val carBrands = listOf("Maruti Suzuki", "Hyundai", "Tata", "Mahindra", "Kia", "Toyota", "Honda", "Other")
    val bikeBrands = listOf("Hero", "Honda", "TVS", "Bajaj", "Royal Enfield", "Yamaha", "Suzuki", "Other")
}
