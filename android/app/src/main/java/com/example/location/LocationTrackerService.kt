package com.example.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class LocationTrackerService : Service(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var autoStartPending = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_AUTO = "ACTION_START_AUTO"
        const val ACTION_STOP_AUTO = "ACTION_STOP_AUTO"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "location_tracker_channel"
        private const val TAG = "LocationTrackerService"
    }

    override fun onCreate() {
        super.onCreate()
        
        TripManager.init(applicationContext)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        Log.d(TAG, "Service created. Accelerometer: ${accelerometer != null}, Gyroscope: ${gyroscope != null}")
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    val speedKmh = if (location.hasSpeed() && location.speed >= 0f) location.speed * 3.6f else 0f
                    Log.d(TAG, "Location update: lat=${location.latitude}, lng=${location.longitude}, speedKmh=$speedKmh")
                    
                    if (autoStartPending && speedKmh > 5.0f) {
                        Log.d(TAG, "Auto-start threshold reached (>5km/h). Starting tracking session.")
                        TripManager.startTracking()
                        autoStartPending = false
                        
                        // Update the foreground notification to reflect active tracking
                        val notification = NotificationCompat.Builder(this@LocationTrackerService, CHANNEL_ID)
                            .setContentTitle("Riding detected, tracking")
                            .setContentText("Your trip is currently being recorded...")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setOngoing(true)
                            .build()
                        val manager = getSystemService(NotificationManager::class.java)
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    
                    TripManager.processLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking(isAuto = false)
            ACTION_STOP -> stopTracking()
            ACTION_START_AUTO -> startTracking(isAuto = true)
            ACTION_STOP_AUTO -> stopTracking()
        }
        return START_STICKY
    }

    @Suppress("MissingPermission")
    private fun startTracking(isAuto: Boolean = false) {
        Log.d(TAG, "Starting tracking... (isAuto=$isAuto)")
        autoStartPending = isAuto
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isAuto) "Auto-Detecting Trip..." else "Tracking Trip")
            .setContentText(if (isAuto) "Waiting for movement > 5km/h..." else "Your trip is currently being recorded...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(1500)
            .build()

        try {
            // Get last known location first to immediately show on map
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Got last known location: lat=${location.latitude}, lng=${location.longitude}")
                    TripManager.processLocation(location)
                }
            }
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            // Register sensors with SENSOR_DELAY_GAME for better resolution
            // This is important for accurate driving data
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                Log.d(TAG, "Accelerometer registered with SENSOR_DELAY_GAME")
            } ?: Log.w(TAG, "No accelerometer sensor available!")
            
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                Log.d(TAG, "Gyroscope registered with SENSOR_DELAY_GAME")
            } ?: Log.w(TAG, "No gyroscope sensor available!")
            
            if (!isAuto) {
                TripManager.startTracking()
            }
            Log.d(TAG, "Tracking started successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing location permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting tracking", e)
        }
    }

    private fun stopTracking() {
        Log.d(TAG, "Stopping tracking...")
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            sensorManager.unregisterListener(this)
            TripManager.stopTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.d(TAG, "Tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tracking", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trip Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notification while tracking your trip"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { TripManager.processSensorEvent(it) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} -> $accuracy")
    }
}
