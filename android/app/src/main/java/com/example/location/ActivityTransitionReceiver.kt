package com.example.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.transitionEvents?.forEach { event ->
                val activityType = event.activityType
                val transitionType = event.transitionType

                val isStart = transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
                val isStop = transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT

                when (activityType) {
                    DetectedActivity.IN_VEHICLE, DetectedActivity.ON_BICYCLE, DetectedActivity.RUNNING -> {
                        if (isStart) {
                            Log.d("ActivityTransition", "Started moving. Auto-starting tracker.")
                            startTrackerService(context)
                        } else if (isStop) {
                            Log.d("ActivityTransition", "Stopped moving. Will wait for 30-min stationary timeout.")
                            // Do not call stopTrackerService here; let TripManager handle it after 30 mins stationary.
                        }
                    }
                }
            }
        }
    }

    private fun startTrackerService(context: Context) {
        val serviceIntent = Intent(context, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.ACTION_START_AUTO
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun stopTrackerService(context: Context) {
        val serviceIntent = Intent(context, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.ACTION_STOP_AUTO
        }
        context.startService(serviceIntent)
    }
}
