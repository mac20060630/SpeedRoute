package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.utils.ReleaseLinks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class OnboardingViewModel : ViewModel() {
    companion object {
        private const val TAG = "OnboardingViewModel"
    }
    var isDarkTheme by mutableStateOf(true)

    var speedUnit by mutableStateOf("km/h")
    var enableSpeedCameras by mutableStateOf<Boolean?>(null)
    var country by mutableStateOf(Countries.list.first { it.name == "India" })
    var vehicleType by mutableStateOf("Car")
    
    // For "Other" vehicle brand
    var showOtherBrand by mutableStateOf(false)
    var vehicleBrand by mutableStateOf("Maruti Suzuki")
    
    // For "Other" vehicle model
    var showOtherModel by mutableStateOf(false)
    var vehicleModel by mutableStateOf("Swift")
    
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var authError by mutableStateOf("")
    var authMessage by mutableStateOf("")
    var isAuthenticating by mutableStateOf(false)
    
    var dob by mutableStateOf("")
    var profilePicBase64 by mutableStateOf<String?>(null)

    var isNewVersionAvailable by mutableStateOf(false)
    var isUpdateMandatory by mutableStateOf(false)
    var isHighScoreBeaten by mutableStateOf(false)
    var userTopSpeed by mutableStateOf(0f)
    var globalMaxSpeed by mutableStateOf(0f)
    var updateApkUrl by mutableStateOf(ReleaseLinks.LATEST_RELEASE_URL)
    var latestVersion by mutableStateOf("")

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
                // Also check auth
                // if we were to enforce globally unique usernames across auth
                
                // Assuming "leaderboard" collection exists and we check for a document with the username
                // Or maybe check another collection if we want unique usernames.
                // Let's assume we allow duplicate usernames or handle it later.
                // For now, if we want unique usernames, we check leaderboard:
                val query = db.collection("leaderboard").whereEqualTo("u", username).get().await()
                if (!query.isEmpty) {
                    isUsernameAvailable = false
                    usernameCheckMessage = "Username is already taken"
                } else {
                    isUsernameAvailable = true
                    usernameCheckMessage = "Username is available"
                }
            } catch (e: Exception) {
                isUsernameAvailable = null
                usernameCheckMessage = "Error checking username"
            } finally {
                isCheckingUsername = false
            }
        }
    }

    fun registerUser(onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            authError = "All fields are required"
            return
        }
        if (!email.lowercase().endsWith("@gmail.com")) {
            authError = "Please use an official Google email (@gmail.com)"
            return
        }
        if (password.length < 6) {
            authError = "Password must be at least 6 characters"
            return
        }
        
        isAuthenticating = true
        authError = ""
        authMessage = ""
        
        viewModelScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    val verificationSent = try {
                        user.sendEmailVerification().await()
                        true
                    } catch (e: Exception) {
                        Log.w(TAG, "Email verification send failed for uid=${user.uid}", e)
                        false
                    }
                    val uid = user.uid
                    val db = FirebaseFirestore.getInstance()
                    val userDoc = hashMapOf(
                        "u" to username,
                        "d" to dob,
                        "e" to email
                    )
                    db.collection("users").document(uid).set(userDoc).await()
                    
                    auth.signOut()
                    authMessage = if (verificationSent) {
                        "Account created. Verification email sent. Check inbox/spam, then log in to continue."
                    } else {
                        "Account created. Verification email could not be sent right now. Please log in to continue."
                    }
                }
            } catch (e: Exception) {
                authError = e.message ?: "Registration failed"
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun loginUser(onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            authError = "Email and password are required"
            return
        }
        
        isAuthenticating = true
        authError = ""
        authMessage = ""
        
        viewModelScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                
                if (user != null && !user.isEmailVerified) {
                    val verificationResent = try {
                        user.sendEmailVerification().await()
                        true
                    } catch (e: Exception) {
                        Log.w(TAG, "Email verification resend failed for uid=${user.uid}", e)
                        false
                    }
                    authMessage = if (verificationResent) {
                        "Email not verified yet. A new verification email was sent. Check inbox/spam."
                    } else {
                        "Email not verified yet. Verification email could not be sent right now."
                    }
                }
                
                val uid = user?.uid
                if (uid != null) {
                    val db = FirebaseFirestore.getInstance()
                    val doc = db.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        username = doc.getString("u") ?: ""
                        dob = doc.getString("d") ?: ""
                        email = doc.getString("e") ?: email
                        profilePicBase64 = doc.getString("p")
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                authError = e.message ?: "Login failed"
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun dismissHighScore(context: Context) {
        isHighScoreBeaten = false
        val prefs = context.getSharedPreferences("SpeedRoutePrefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("dismissed_highscore", globalMaxSpeed).apply()
    }

    private fun showUpdateNotification(context: Context, latestVersion: String, updateUrl: String) {
        val channelId = "update_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "App Updates", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Fallback icon
            .setContentTitle("New Update Available")
            .setContentText("Version $latestVersion is available. Tap to download.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(2, notification)
    }

    fun checkNotifications(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = context.getSharedPreferences("SpeedRoutePrefs", Context.MODE_PRIVATE)
        
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                
                // 1. Check Version (simulate/get from app_metadata/version)
                val versionDoc = db.collection("app_metadata").document("version").get().await()
                val currentVersion = com.example.BuildConfig.VERSION_NAME
                val currentVersionCode = com.example.BuildConfig.VERSION_CODE.toLong()
                if (versionDoc.exists()) {
                    val latest = versionDoc.getString("latest_version") ?: currentVersion
                    latestVersion = latest
                    
                    updateApkUrl = versionDoc.getString("apk_url")
                        ?: ReleaseLinks.LATEST_RELEASE_URL
                    
                    val minRequiredCode = versionDoc.getLong("minimum_version_code") ?: 0L
                    isUpdateMandatory = currentVersionCode < minRequiredCode
                    
                    if (latest != currentVersion && !isUpdateMandatory) {
                        val lastNotifiedVersion = prefs.getString("notified_version", "")
                        if (latest != lastNotifiedVersion) {
                            showUpdateNotification(context, latest, updateApkUrl)
                            prefs.edit().putString("notified_version", latest).apply()
                        }
                    }
                    
                    isNewVersionAvailable = false // Not used in-app anymore
                } else {
                    db.collection("app_metadata").document("version").set(hashMapOf(
                        "latest_version" to currentVersion,
                        "minimum_version_code" to currentVersionCode,
                        "apk_url" to ReleaseLinks.LATEST_RELEASE_URL
                    ))
                    latestVersion = currentVersion
                    isNewVersionAvailable = false
                    isUpdateMandatory = false
                    updateApkUrl = ReleaseLinks.LATEST_RELEASE_URL
                }

                val calendarYearWeek = java.util.Calendar.getInstance()
                val weekId = "${calendarYearWeek.get(java.util.Calendar.YEAR)}_${calendarYearWeek.get(java.util.Calendar.WEEK_OF_YEAR)}"

                // 2. Check Leaderboard
                val userLeadDoc = db.collection("leaderboard_$weekId").document(uid).get().await()
                val userSpeed = userLeadDoc.getDouble("ts")?.toFloat() ?: 0f
                userTopSpeed = userSpeed

                val maxLeadQuery = db.collection("leaderboard_$weekId")
                    .orderBy("ts", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                if (!maxLeadQuery.isEmpty) {
                    val maxSpeed = maxLeadQuery.documents.first().getDouble("ts")?.toFloat() ?: 0f
                    globalMaxSpeed = maxSpeed
                    val dismissedSpeed = prefs.getFloat("dismissed_highscore", 0f)
                    isHighScoreBeaten = userSpeed > 0f && maxSpeed > userSpeed && maxSpeed > dismissedSpeed
                } else {
                    isHighScoreBeaten = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchUserProfile() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    username = doc.getString("u") ?: username
                    dob = doc.getString("d") ?: dob
                    email = doc.getString("e") ?: auth.currentUser?.email ?: ""
                    profilePicBase64 = doc.getString("p")
                    vehicleType = doc.getString("vType") ?: vehicleType
                    vehicleBrand = doc.getString("vBrand") ?: vehicleBrand
                    vehicleModel = doc.getString("vModel") ?: vehicleModel
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveProfilePicture(base64: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(hashMapOf("p" to base64), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                profilePicBase64 = base64
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save profile picture")
            }
        }
    }

    fun logoutUser(onSuccess: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        username = ""
        email = ""
        password = ""
        dob = ""
        profilePicBase64 = null
        onSuccess()
    }

    fun resetPassword(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentEmail = FirebaseAuth.getInstance().currentUser?.email ?: email
        if (currentEmail.isBlank()) {
            onError("No email found to reset password")
            return
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(currentEmail)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to send reset email") }
    }

    fun updateVehicleDetails(type: String, brand: String, model: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val updates = hashMapOf<String, Any>(
                    "vType" to type,
                    "vBrand" to brand,
                    "vModel" to model
                )
                db.collection("users").document(uid).update(updates).await()
                vehicleType = type
                vehicleBrand = brand
                vehicleModel = model
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update vehicle details")
            }
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val updates = hashMapOf<String, Any>(
                        "d" to dob,
                        "vType" to vehicleType,
                        "vBrand" to vehicleBrand,
                        "vModel" to vehicleModel,
                        "country" to country.name,
                        "speedUnit" to speedUnit
                    )
                    db.collection("users").document(uid).update(updates).await()
                } catch (e: Exception) {
                    // Ignore errors if update fails, we can still proceed
                }
                onSuccess()
            }
        } else {
            onSuccess()
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
    val carBrands = listOf("Maruti Suzuki", "Hyundai", "Tata", "Mahindra", "Kia", "Toyota", "Honda", "Volkswagen", "Skoda", "Renault", "MG", "Nissan", "Ford", "Chevrolet", "Jeep", "BMW", "Mercedes-Benz", "Audi", "Volvo", "Other")
    val bikeBrands = listOf("Hero", "Honda", "TVS", "Bajaj", "Royal Enfield", "Yamaha", "Suzuki", "KTM", "Jawa", "Yezdi", "Ather", "Ola", "Triumph", "Harley-Davidson", "Ducati", "BMW Motorrad", "Kawasaki", "Aprilia", "Vespa", "Other")

    val carModels = mapOf(
        "Maruti Suzuki" to listOf("Swift", "Baleno", "WagonR", "Alto K10", "Dzire", "Ertiga", "Brezza", "Fronx", "Grand Vitara", "Jimny", "Celerio", "Ignis", "Ciaz", "XL6", "S-Presso", "Eeco", "Invicto", "Gypsy", "Esteem", "Zen", "Other"),
        "Hyundai" to listOf("Creta", "Venue", "i20", "Grand i10 Nios", "Verna", "Aura", "Tucson", "Alcazar", "Exter", "Santro", "Ioniq 5", "Kona Electric", "Elantra", "Xcent", "Eon", "Getz", "Accent", "Sonata", "Terracan", "Santa Fe", "Other"),
        "Tata" to listOf("Nexon", "Punch", "Tiago", "Altroz", "Harrier", "Safari", "Tigor", "Nexon EV", "Tiago EV", "Punch EV", "Hexa", "Aria", "Bolt", "Zest", "Indica", "Indigo", "Nano", "Sumo", "Sierra", "Estate", "Other"),
        "Mahindra" to listOf("Scorpio-N", "Scorpio Classic", "XUV700", "Thar", "Bolero", "Bolero Neo", "XUV300", "Marazzo", "XUV400 EV", "KUV100", "TUV300", "Quanto", "Verito", "Logan", "Xylo", "Armada", "Commander", "Jeep", "Marshal", "Alturas G4", "Other"),
        "Kia" to listOf("Seltos", "Sonet", "Carens", "Carnival", "EV6", "EV9", "Sportage", "Sorento", "Rio", "Forte", "Optima", "Stinger", "Telluride", "Soul", "Niro", "Picanto", "Ceed", "Stonic", "Xceed", "Pegas", "Other"),
        "Toyota" to listOf("Innova Crysta", "Innova Hycross", "Fortuner", "Glanza", "Urban Cruiser Hyryder", "Camry", "Vellfire", "Hilux", "Corolla Altis", "Yaris", "Etios", "Liva", "Qualis", "Land Cruiser", "Prado", "Prius", "RAV4", "Supra", "Celica", "MR2", "Other"),
        "Honda" to listOf("City", "Amaze", "Elevate", "Jazz", "WR-V", "BR-V", "CR-V", "Civic", "Accord", "Brio", "Mobilio", "City Hybrid", "HR-V", "Odyssey", "Pilot", "Passport", "Ridgeline", "Insight", "Fit", "Element", "Other"),
        "Volkswagen" to listOf("Virtus", "Taigun", "Polo", "Vento", "Tiguan", "Jetta", "Passat", "Ameo", "Touareg", "Beetle", "Golf", "Scirocco", "Arteon", "T-Roc", "ID.4", "ID.Buzz", "Atlas", "Up!", "Bora", "Phaeton", "Other"),
        "Skoda" to listOf("Slavia", "Kushaq", "Octavia", "Superb", "Kodiaq", "Rapid", "Fabia", "Yeti", "Laura", "Karoq", "Kamiq", "Scala", "Enyaq", "Roomster", "Citigo", "Felicia", "Favorit", "Superb Combi", "Octavia RS", "Other"),
        "Renault" to listOf("Kiger", "Triber", "Kwid", "Duster", "Captur", "Lodgy", "Fluence", "Koleos", "Pulse", "Scala", "Clio", "Megane", "Zoe", "Arkana", "Kadjar", "Twingo", "Scenic", "Espace", "Talisman", "Alaskan", "Other"),
        "MG" to listOf("Hector", "Hector Plus", "Astor", "Gloster", "ZSEV", "Comet EV", "MG4", "MG5", "HS", "RX5", "Extender", "V80", "G10", "Cyberster", "Marvel R", "Midget", "MGB", "MGA", "MGC", "TF", "Other"),
        "Nissan" to listOf("Magnite", "Kicks", "Micra", "Sunny", "Terrano", "X-Trail", "Leaf", "GT-R", "Teana", "Evalia", "Patrol", "Navara", "Juke", "Qashqai", "Ariya", "Armada", "Pathfinder", "Murano", "Rogue", "Sentra", "Other"),
        "Ford" to listOf("EcoSport", "Endeavour", "Figo", "Aspire", "Freestyle", "Mustang", "Fiesta", "Ikon", "Escort", "Mondeo", "Puma", "Focus", "Kuga", "Explorer", "Edge", "Bronco", "F-150", "Ranger", "Maverick", "Transit", "Other"),
        "Chevrolet" to listOf("Beat", "Cruze", "Spark", "Tavera", "Enjoy", "Sail", "Captiva", "Optra", "Aveo", "Trailblazer", "Camaro", "Corvette", "Silverado", "Equinox", "Tahoe", "Suburban", "Malibu", "Trax", "Blazer", "Colorado", "Other"),
        "Jeep" to listOf("Compass", "Meridian", "Wrangler", "Grand Cherokee", "Renegade", "Gladiator", "Avenger", "Patriot", "Liberty", "Commander", "Wagoneer", "Cherokee", "CJ", "Scrambler", "Comanche", "Honcho", "DJ", "FC", "VJ", "Other"),
        "BMW" to listOf("X1", "X3", "X5", "X7", "3 Series", "5 Series", "7 Series", "Z4", "iX", "i4", "M2", "M3", "M4", "M5", "M8", "X4", "X6", "i7", "iX3", "2 Series", "Other"),
        "Mercedes-Benz" to listOf("C-Class", "E-Class", "S-Class", "GLC", "GLE", "GLS", "A-Class", "G-Class", "EQE", "EQS", "AMG GT", "CLA", "GLA", "GLB", "SL", "SLC", "EQC", "EQB", "Maybach", "V-Class", "Other"),
        "Audi" to listOf("A4", "A6", "A8", "Q3", "Q5", "Q7", "Q8", "e-tron", "RS5", "R8", "A3", "A5", "A7", "Q2", "Q4 e-tron", "TT", "RS6", "RS7", "RS Q8", "e-tron GT", "Other"),
        "Volvo" to listOf("XC40", "XC60", "XC90", "S60", "S90", "C40 Recharge", "V60", "V90", "EX30", "EX90", "C30", "V40", "S80", "V70", "XC70", "850", "940", "240", "140", "Amazon", "Other"),
        "Other" to listOf("Other")
    )

    val bikeModels = mapOf(
        "Hero" to listOf("Splendor Plus", "HF Deluxe", "Passion Pro", "Glamour", "Super Splendor", "Xtreme 160R", "Xpulse 200", "Destini 125", "Maestro Edge", "Pleasure Plus", "Karizma XMR", "Xoom", "Vida V1", "CBZ", "Hunk", "Ignitor", "Achiever", "Dawn", "Joy", "Street", "Other"),
        "Honda" to listOf("Activa 6G", "Shine", "SP 125", "Unicorn", "Dio", "Hness CB350", "CB350RS", "Livo", "X-Blade", "Hornet 2.0", "CBR 150R", "CBR 250R", "CB Shine", "Dream Yuga", "Navi", "Aviator", "Grazia", "Gold Wing", "Africa Twin", "CBR1000RR", "Other"),
        "TVS" to listOf("Jupiter", "Apache RTR 160", "Apache RTR 200", "Raider", "Ntorq", "XL100", "Star City Plus", "Sport", "Radeon", "Ronin", "iQube", "Apache RR 310", "Scooty Pep+", "Zest", "Victor", "Fiero", "Centra", "Jive", "Max", "Samurai", "Other"),
        "Bajaj" to listOf("Pulsar 150", "Pulsar NS200", "Pulsar N160", "Platina", "CT 100", "Discover", "Dominar 400", "Dominar 250", "Avenger Cruise 220", "Chetak", "Pulsar 220F", "Pulsar RS200", "V15", "XCD", "Boxer", "Caliber", "Wind", "Byk", "Kristal", "Sunny", "Other"),
        "Royal Enfield" to listOf("Classic 350", "Bullet 350", "Hunter 350", "Meteor 350", "Himalayan", "Interceptor 650", "Continental GT 650", "Super Meteor 650", "Thunderbird", "Electra", "Machismo", "Taurus", "Lightning", "Fury", "Scram 411", "Shotgun 650", "Classic 500", "Bullet 500", "Rumbler", "Other"),
        "Yamaha" to listOf("MT-15", "R15 V4", "FZS-FI", "Fascino", "RayZR", "Aerox 155", "FZ-X", "FZ25", "R3", "MT-03", "RX100", "RX135", "RD350", "Crux", "Enticer", "Gladiator", "YBR", "Saluto", "FZ16", "R1", "Other"),
        "Suzuki" to listOf("Access 125", "Burgman Street", "Avenis", "Gixxer", "Gixxer SF", "V-Strom SX", "Hayabusa", "Intruder", "Katana", "V-Strom 650", "GSX-R1000", "Samurai", "Shogun", "Shaolin", "Fiero", "Zeus", "Slingshot", "Let's", "Swish", "Max100", "Other"),
        "KTM" to listOf("Duke 200", "Duke 250", "Duke 390", "RC 200", "RC 390", "Adventure 250", "Adventure 390", "Duke 125", "RC 125", "Super Duke R", "EXC", "SX", "Freeride", "SMC", "Enduro", "RC8", "Duke 790", "Duke 890", "Adventure 890", "Super Adventure", "Other"),
        "Jawa" to listOf("Jawa 350", "Jawa 42", "Jawa Perak", "Jawa 42 Bobber", "Jawa Standard", "Jawa Classic", "Type 353", "Type 354", "Type 634", "Type 638", "Californian", "Mustang", "Babetta", "Pionyr", "Robot", "Ogar", "Minor", "Supersport", "Roadking", "Other"),
        "Yezdi" to listOf("Adventure", "Scrambler", "Roadster", "Classic", "Monarch", "Deluxe", "Roadking", "Colt", "350 Twin", "175", "60", "Supersprint", "Jawa 250", "Model B", "Model C", "Model D", "Model E", "Model F", "Model G", "Other"),
        "Ather" to listOf("450X", "450S", "450 Apex", "Rizta", "450 Plus", "340", "450", "Other"),
        "Ola" to listOf("S1 Pro", "S1 Air", "S1 X", "S1", "Other"),
        "Triumph" to listOf("Speed 400", "Scrambler 400 X", "Street Triple", "Trident 660", "Tiger 900", "Tiger 1200", "Bonneville T100", "Bonneville T120", "Thruxton", "Bobber", "Speed Twin", "Scrambler 900", "Scrambler 1200", "Rocket 3", "Daytona", "Sprint", "Trophy", "Adventurer", "Legend", "Thunderbird", "Other"),
        "Harley-Davidson" to listOf("X440", "Iron 883", "Forty-Eight", "Street 750", "Street Rod", "Fat Boy", "Heritage Classic", "Road King", "Street Glide", "Road Glide", "Pan America", "Sportster S", "Nightster", "Softail Standard", "Low Rider", "Breakout", "Fat Bob", "Electra Glide", "V-Rod", "Other"),
        "Ducati" to listOf("Panigale V4", "Streetfighter V4", "Multistrada V4", "Monster", "Scrambler", "Diavel", "XDiavel", "Hypermotard", "SuperSport", "DesertX", "1199 Panigale", "1299 Panigale", "899 Panigale", "959 Panigale", "1098", "1198", "848", "916", "996", "998", "Other"),
        "BMW Motorrad" to listOf("G 310 R", "G 310 GS", "R 1250 GS", "S 1000 RR", "F 900 R", "F 900 XR", "R 18", "CE 04", "M 1000 RR", "S 1000 XR", "F 850 GS", "F 750 GS", "R 1250 R", "R 1250 RS", "R 1250 RT", "K 1600 GT", "K 1600 GTL", "K 1600 B", "C 400 X", "C 400 GT", "Other"),
        "Kawasaki" to listOf("Ninja 300", "Ninja 400", "Ninja 650", "Ninja 1000", "Z900", "Z650", "Versys 650", "Vulcan S", "ZX-10R", "Z H2", "W800", "KX250", "KLX 140", "Ninja H2", "Ninja ZX-6R", "Z400", "Versys 1000", "Concours 14", "KLR 650", "Eliminator", "Other"),
        "Aprilia" to listOf("SR 160", "SR 125", "SXR 160", "SXR 125", "RS 457", "RSV4", "Tuono V4", "Tuareg 660", "RS 660", "Tuono 660", "Dorsoduro", "Shiver", "Caponord", "Pegaso", "Mana", "Scarabeo", "SportCity", "Atlantic", "SR Max", "SRV 850", "Other"),
        "Vespa" to listOf("VXL 125", "VXL 150", "SXL 125", "SXL 150", "ZX 125", "LX 125", "Notte 125", "Elegante 150", "GTS 300", "Primavera", "Sprint", "Elettrica", "Sei Giorni", "946", "PX", "Cosa", "PK", "Rally", "Super", "Sprint Veloce", "Other"),
        "Other" to listOf("Other")
    )
}
