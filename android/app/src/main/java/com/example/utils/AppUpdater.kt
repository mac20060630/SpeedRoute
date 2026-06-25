package com.example.utils

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

object AppUpdater {
    fun downloadAndInstallApk(context: Context, downloadUrl: String = "") {
        val activity = context as? Activity ?: return
        val appUpdateManager = AppUpdateManagerFactory.create(context)
        
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    1001
                )
            }
        }
    }
}
