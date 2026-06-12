package com.example.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

object AutoTrackingManager {
    private const val TAG = "AutoTrackingManager"

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun enableAutoTracking(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "ACTIVITY_RECOGNITION permission missing.")
                return
            }
        }

        val transitions = mutableListOf<ActivityTransition>()
        val trackableActivities = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.RUNNING
        )

        for (activity in trackableActivities) {
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        val request = ActivityTransitionRequest(transitions)
        val client = ActivityRecognition.getClient(context)

        client.requestActivityTransitionUpdates(request, getPendingIntent(context))
            .addOnSuccessListener {
                Log.d(TAG, "Successfully registered for Activity Transitions.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to register for Activity Transitions.", e)
            }
    }

    fun disableAutoTracking(context: Context) {
        val client = ActivityRecognition.getClient(context)
        client.removeActivityTransitionUpdates(getPendingIntent(context))
            .addOnSuccessListener {
                Log.d(TAG, "Successfully deregistered from Activity Transitions.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to deregister Activity Transitions.", e)
            }
    }
}
