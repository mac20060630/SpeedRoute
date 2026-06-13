package com.example.utils

import android.content.Context
import com.example.models.Trip
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

object LocalTripStorage {
    private const val TRIPS_DIR_NAME = "local_trips"
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val adapter = moshi.adapter(Trip::class.java)

    fun saveTrip(context: Context, trip: Trip) {
        val dir = File(context.filesDir, TRIPS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "${trip.id}.json")
        try {
            val json = adapter.toJson(trip)
            file.writeText(json)
            
            // Backup to persistent external storage (survives app uninstall)
            val backupDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "SpeedRouteTrips")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val backupFile = File(backupDir, "${trip.id}.json")
            backupFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAllTrips(context: Context): List<Trip> {
        val dir = File(context.filesDir, TRIPS_DIR_NAME)
        if (!dir.exists()) return emptyList()

        val trips = mutableListOf<Trip>()
        val files = dir.listFiles() ?: return emptyList()
        
        for (file in files) {
            if (file.isFile && file.name.endsWith(".json")) {
                try {
                    val json = file.readText()
                    adapter.fromJson(json)?.let { trips.add(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return trips.sortedByDescending { it.timestamp }
    }

    fun getTripById(context: Context, tripId: String): Trip? {
        val file = File(File(context.filesDir, TRIPS_DIR_NAME), "$tripId.json")
        if (!file.exists()) return null
        return try {
            val json = file.readText()
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
