package com.example.location

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.location.Location
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.sqrt

data class TripStats(
    val currentSpeedKmH: Float = 0f,
    val topSpeedKmH: Float = 0f,
    val totalDistanceKm: Float = 0f,
    val durationSeconds: Long = 0,
    val stoppedTimeSeconds: Long = 0,
    
    val maxAcceleration: Float = 0f,
    val maxDeceleration: Float = 0f,
    val peakGForce: Float = 0f,
    val topCornerSpeedKmH: Float = 0f,
    
    val isTracking: Boolean = false,
    
    val totalTrips: Int = 0,
    val totalStops: Int = 0,
    val totalAllTimeDurationSeconds: Long = 0,
    val allTimeDistanceKm: Float = 0f,
    
    val best0To100TimeSec: Float? = null,
    val leftTurns: Int = 0,
    val rightTurns: Int = 0,
    val brakeEvents: Int = 0,
    val laneChanges: Int = 0,
    
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val currentAltitude: Double = 0.0,
    val routePoints: List<com.example.models.TripPoint> = emptyList()
)

object TripManager {
    private const val TAG = "TripManager"
    
    private val _stats = MutableStateFlow(TripStats())
    val stats: StateFlow<TripStats> = _stats.asStateFlow()

    private var prefs: SharedPreferences? = null

    private var lastLocation: Location? = null
    private var lastTime: Long = 0
    private var startTime: Long = 0
    private var stoppedTimeAccumulator: Long = 0
    private var isCurrentlyStopped = true
    
    private var speed0StartTime: Long? = null

    // Sensor State
    private var currentAccel: Float = 0f
    private var isTurning: Boolean = false
    private var turnAccumulatedZ: Float = 0f
    private var lastGyroTime: Long = 0
    private var lastTurnDirection: Int = 0 // 1 left, -1 right
    private var lastTurnTime: Long = 0
    
    // Low pass filter for accelerometer
    private var gravity = FloatArray(3)
    private var linearAcceleration = FloatArray(3)
    private var gravityInitialized = false
    
    private var sessionTotalStops = 0
    
    // Track previous speed for better acceleration detection
    private var previousSpeedKmH: Float = 0f
    private var lastSpeedTime: Long = 0

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("TripStatsPrefs", Context.MODE_PRIVATE)
            loadPersistentStats()
            Log.d(TAG, "TripManager initialized")
        }
    }

    private fun loadPersistentStats() {
        val p = prefs ?: return
        _stats.update {
            it.copy(
                totalTrips = p.getInt("totalTrips", 0),
                totalAllTimeDurationSeconds = p.getLong("totalAllTimeDuration", 0L),
                allTimeDistanceKm = p.getFloat("allTimeDistance", 0f),
                best0To100TimeSec = if (p.contains("best0100")) p.getFloat("best0100", 0f) else null
            )
        }
        Log.d(TAG, "Loaded persistent stats: trips=${_stats.value.totalTrips}, allTimeDist=${_stats.value.allTimeDistanceKm}")
    }

    private fun savePersistentStats(current: TripStats) {
        val p = prefs ?: return
        p.edit().apply {
            putInt("totalTrips", current.totalTrips)
            putLong("totalAllTimeDuration", current.totalAllTimeDurationSeconds)
            putFloat("allTimeDistance", current.allTimeDistanceKm)
            if (current.best0To100TimeSec != null) {
                putFloat("best0100", current.best0To100TimeSec)
            }
            apply()
        }
    }

    fun startTracking() {
        Log.d(TAG, "Starting tracking session")
        startTime = System.currentTimeMillis()
        lastLocation = null
        lastTime = startTime
        speed0StartTime = null
        stoppedTimeAccumulator = 0
        isCurrentlyStopped = true
        sessionTotalStops = 0
        previousSpeedKmH = 0f
        lastSpeedTime = 0
        
        turnAccumulatedZ = 0f
        isTurning = false
        lastGyroTime = 0
        lastTurnTime = 0
        lastTurnDirection = 0
        gravity = FloatArray(3)
        linearAcceleration = FloatArray(3)
        gravityInitialized = false
        currentAccel = 0f
        
        _stats.update {
            it.copy(
                isTracking = true,
                currentSpeedKmH = 0f,
                topSpeedKmH = 0f,
                totalDistanceKm = 0f,
                durationSeconds = 0,
                stoppedTimeSeconds = 0,
                maxAcceleration = 0f,
                maxDeceleration = 0f,
                peakGForce = 0f,
                topCornerSpeedKmH = 0f,
                leftTurns = 0,
                rightTurns = 0,
                brakeEvents = 0,
                laneChanges = 0,
                totalStops = 0,
                totalTrips = it.totalTrips + 1
            )
        }
        savePersistentStats(_stats.value)
    }

    fun stopTracking() {
        Log.d(TAG, "Stopping tracking session")
        val current = _stats.value
        _stats.update { it.copy(isTracking = false, currentSpeedKmH = 0f) }
        savePersistentStats(current.copy(
            totalAllTimeDurationSeconds = current.totalAllTimeDurationSeconds,
            allTimeDistanceKm = current.allTimeDistanceKm
        ))
    }

    fun processLocation(location: Location) {
        if (!_stats.value.isTracking) {
            // Even when not tracking, update current location for map display
            _stats.update { it.copy(
                currentLat = location.latitude,
                currentLng = location.longitude,
                currentAltitude = location.altitude
            ) }
            return
        }

        val speedMs = if (location.hasSpeed() && location.speed >= 0f) location.speed else 0f
        val speedKmh = speedMs * 3.6f
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "Processing location: speed=${speedKmh}km/h, lat=${location.latitude}, lng=${location.longitude}, hasSpeed=${location.hasSpeed()}")

        _stats.update { current ->
            var newTopSpeed = current.topSpeedKmH
            var newTopCorner = current.topCornerSpeedKmH
            var newDistance = current.totalDistanceKm
            var newAllTimeDist = current.allTimeDistanceKm
            var newBest0100 = current.best0To100TimeSec
            var newStoppedTime = stoppedTimeAccumulator
            var newDuration = (currentTime - startTime) / 1000

            if (speedKmh > newTopSpeed) {
                newTopSpeed = speedKmh
                Log.d(TAG, "New top speed: ${newTopSpeed} km/h")
            }
            if (isTurning && speedKmh > newTopCorner) {
                newTopCorner = speedKmh
            }

            if (speedKmh < 1.0f) {
                if (!isCurrentlyStopped) {
                    isCurrentlyStopped = true
                    sessionTotalStops++
                    Log.d(TAG, "Vehicle stopped. Total stops: $sessionTotalStops")
                }
                newStoppedTime += (currentTime - lastTime)
            } else {
                isCurrentlyStopped = false
            }
            stoppedTimeAccumulator = newStoppedTime

            if (lastLocation != null) {
                val distanceMeters = location.distanceTo(lastLocation!!)
                // Filter out unrealistic distance jumps (GPS noise)
                if (distanceMeters < 500f) { // Max 500m between 1-second updates = 1800 km/h
                    newDistance += distanceMeters / 1000f
                    newAllTimeDist += distanceMeters / 1000f
                }
            }
            
            // 0-100 logic
            if (speedKmh < 1f) {
                speed0StartTime = currentTime
            } else if (speedKmh >= 100f && speed0StartTime != null) {
                val timeTo100 = (currentTime - speed0StartTime!!) / 1000f
                if (timeTo100 > 0.5f && (newBest0100 == null || timeTo100 < newBest0100)) {
                    newBest0100 = timeTo100
                    Log.d(TAG, "New best 0-100: ${newBest0100}s")
                }
                speed0StartTime = null
            }
            
            previousSpeedKmH = speedKmh
            lastSpeedTime = currentTime

            val newRoutePoints = current.routePoints.toMutableList()
            if (newRoutePoints.isEmpty() || lastLocation == null || location.distanceTo(lastLocation!!) > 50f) {
                newRoutePoints.add(com.example.models.TripPoint(location.latitude, location.longitude))
            }

            current.copy(
                currentSpeedKmH = speedKmh,
                topSpeedKmH = newTopSpeed,
                topCornerSpeedKmH = newTopCorner,
                totalDistanceKm = newDistance,
                allTimeDistanceKm = newAllTimeDist,
                durationSeconds = newDuration,
                totalAllTimeDurationSeconds = current.totalAllTimeDurationSeconds + (currentTime - lastTime) / 1000,
                stoppedTimeSeconds = stoppedTimeAccumulator / 1000,
                totalStops = sessionTotalStops,
                best0To100TimeSec = newBest0100,
                currentLat = location.latitude,
                currentLng = location.longitude,
                currentAltitude = location.altitude,
                routePoints = newRoutePoints
            )
        }

        lastLocation = location
        lastTime = currentTime
    }

    fun processSensorEvent(event: SensorEvent) {
        if (!_stats.value.isTracking) return
        
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Low-pass filter to isolate gravity
                val alpha = if (gravityInitialized) 0.8f else 0f
                if (!gravityInitialized) {
                    gravity[0] = event.values[0]
                    gravity[1] = event.values[1]
                    gravity[2] = event.values[2]
                    gravityInitialized = true
                } else {
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                }

                linearAcceleration[0] = event.values[0] - gravity[0]
                linearAcceleration[1] = event.values[1] - gravity[1]
                linearAcceleration[2] = event.values[2] - gravity[2]

                val accelMagnitude = sqrt(
                    linearAcceleration[0] * linearAcceleration[0] +
                    linearAcceleration[1] * linearAcceleration[1] +
                    linearAcceleration[2] * linearAcceleration[2]
                )
                val gForce = accelMagnitude / 9.81f
                
                // Longitudinal acceleration (Y axis when phone is mounted vertically)
                val longitudinalAccel = linearAcceleration[1]

                _stats.update { current ->
                    var nMaxAccel = current.maxAcceleration
                    var nMaxDecel = current.maxDeceleration
                    var nPeakG = current.peakGForce
                    var nBrakes = current.brakeEvents

                    if (gForce > nPeakG) nPeakG = gForce
                    
                    if (longitudinalAccel > nMaxAccel) nMaxAccel = longitudinalAccel
                    if (longitudinalAccel < nMaxDecel) nMaxDecel = longitudinalAccel
                    
                    // Brake event: sudden strong deceleration
                    if (longitudinalAccel < -3.0f && currentAccel >= -3.0f) {
                        nBrakes++
                        Log.d(TAG, "Brake event detected! Total: $nBrakes, accel: $longitudinalAccel")
                    }

                    currentAccel = longitudinalAccel

                    current.copy(
                        maxAcceleration = nMaxAccel,
                        maxDeceleration = nMaxDecel,
                        peakGForce = nPeakG,
                        brakeEvents = nBrakes
                    )
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val zRotation = event.values[2] // Z axis is yaw (steering turns)
                val currentTime = System.currentTimeMillis()
                
                if (lastGyroTime != 0L) {
                    val dt = (currentTime - lastGyroTime) / 1000f
                    
                    // Avoid processing stale data
                    if (dt < 1.0f) {
                        turnAccumulatedZ += zRotation * dt
                        
                        // Threshold: ~45 degrees accumulated rotation indicates a turn
                        if (abs(turnAccumulatedZ) > 0.8f) {
                            isTurning = true
                            val thisTurnDirection = if (turnAccumulatedZ > 0) 1 else -1 // 1=left, -1=right
                            
                            _stats.update { current ->
                                var nLeft = current.leftTurns
                                var nRight = current.rightTurns
                                var nLanes = current.laneChanges
                                
                                if (thisTurnDirection == 1) { 
                                    nLeft++
                                    Log.d(TAG, "Left turn detected! Total: $nLeft")
                                } else {
                                    nRight++
                                    Log.d(TAG, "Right turn detected! Total: $nRight")
                                }
                                
                                // Lane change heuristic: opposing turns within 3 seconds
                                if (lastTurnDirection != 0 && lastTurnDirection != thisTurnDirection) {
                                    if (currentTime - lastTurnTime < 3000) {
                                        nLanes++
                                        Log.d(TAG, "Lane change detected! Total: $nLanes")
                                    }
                                }
                                
                                turnAccumulatedZ = 0f
                                lastTurnDirection = thisTurnDirection
                                lastTurnTime = currentTime
                                
                                current.copy(leftTurns = nLeft, rightTurns = nRight, laneChanges = nLanes)
                            }
                        } else if (abs(zRotation) < 0.1f) {
                            isTurning = false
                        }
                    }
                }
                lastGyroTime = currentTime
            }
        }
    }
}
