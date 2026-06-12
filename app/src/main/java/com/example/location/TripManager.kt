package com.example.location

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

data class TripStats(
    val currentSpeedKmH: Float = 0f,
    val topSpeedKmH: Float = 0f,
    val totalDistanceKm: Float = 0f,
    val durationSeconds: Long = 0,
    val maxAcceleration: Float = 0f,
    val maxDeceleration: Float = 0f,
    val isTracking: Boolean = false,
    val totalTrips: Int = 0, // Placeholder
    val best0To100TimeSec: Float? = null,
    val leftTurns: Int = 0, // Placeholder
    val rightTurns: Int = 0, // Placeholder
    val brakeEvents: Int = 0, // Placeholder
    val laneChanges: Int = 0, // Placeholder
    val currentLat: Double? = null,
    val currentLng: Double? = null
)

object TripManager {
    private val _stats = MutableStateFlow(TripStats())
    val stats: StateFlow<TripStats> = _stats.asStateFlow()

    private var lastLocation: Location? = null
    private var lastSpeed: Float = 0f
    private var lastTime: Long = 0
    private var startTime: Long = 0
    
    // For 0-100 logic
    private var speed0StartTime: Long? = null

    fun startTracking() {
        startTime = System.currentTimeMillis()
        lastLocation = null
        lastSpeed = 0f
        lastTime = 0
        speed0StartTime = null
        
        _stats.update {
            it.copy(
                isTracking = true,
                currentSpeedKmH = 0f,
                topSpeedKmH = 0f,
                totalDistanceKm = 0f,
                durationSeconds = 0,
                maxAcceleration = 0f,
                maxDeceleration = 0f,
                best0To100TimeSec = null,
                totalTrips = it.totalTrips + 1
            )
        }
    }

    fun stopTracking() {
        _stats.update { it.copy(isTracking = false, currentSpeedKmH = 0f) }
    }

    fun processLocation(location: Location) {
        if (!_stats.value.isTracking) return

        val speedMs = if (location.hasSpeed()) location.speed else 0f
        val speedKmh = speedMs * 3.6f
        val currentTime = System.currentTimeMillis()

        _stats.update { current ->
            var newTopSpeed = current.topSpeedKmH
            var newDistance = current.totalDistanceKm
            var newMaxAccel = current.maxAcceleration
            var newMaxDecel = current.maxDeceleration
            var newBest0100 = current.best0To100TimeSec

            if (speedKmh > newTopSpeed) {
                newTopSpeed = speedKmh
            }

            if (lastLocation != null) {
                val distanceMeters = location.distanceTo(lastLocation!!)
                newDistance += distanceMeters / 1000f

                val timeDiffSec = (currentTime - lastTime) / 1000f
                if (timeDiffSec > 0) {
                    val acceleration = (speedMs - lastSpeed) / timeDiffSec
                    if (acceleration > newMaxAccel) newMaxAccel = acceleration
                    if (acceleration < 0 && acceleration < newMaxDecel) newMaxDecel = acceleration // this will be negative
                }
            }
            
            // basic 0-100 logic
            if (speedKmh < 1f) {
                speed0StartTime = currentTime
            } else if (speedKmh >= 100f && speed0StartTime != null) {
                val timeTo100 = (currentTime - speed0StartTime!!) / 1000f
                if (newBest0100 == null || timeTo100 < newBest0100) {
                    newBest0100 = timeTo100
                }
                speed0StartTime = null
            }

            current.copy(
                currentSpeedKmH = speedKmh,
                topSpeedKmH = newTopSpeed,
                totalDistanceKm = newDistance,
                durationSeconds = (currentTime - startTime) / 1000,
                maxAcceleration = newMaxAccel,
                maxDeceleration = newMaxDecel,
                best0To100TimeSec = newBest0100,
                currentLat = location.latitude,
                currentLng = location.longitude
            )
        }

        lastLocation = location
        lastSpeed = speedMs
        lastTime = currentTime
    }
}
