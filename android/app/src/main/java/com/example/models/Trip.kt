package com.example.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Trip(
    @get:Exclude var id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val totalDistanceKm: Double = 0.0,
    val topSpeedKmH: Double = 0.0,
    val best0To60TimeSec: Double? = null,
    val best0To100TimeSec: Double? = null,
    val leftTurns: Int = 0,
    val rightTurns: Int = 0,
    val hardBrakes: Int = 0,
    val hardAccelerations: Int = 0,
    val stoppedTimeSeconds: Long = 0,
    val maxAcceleration: Double = 0.0,
    val maxDeceleration: Double = 0.0,
    val peakGForce: Double = 0.0,
    val topCornerSpeedKmH: Double = 0.0,
    val totalStops: Int = 0,
    val laneChanges: Int = 0,
    val routePoints: List<TripPoint> = emptyList()
)

@IgnoreExtraProperties
data class TripPoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val speedKmh: Float = 0f
)
