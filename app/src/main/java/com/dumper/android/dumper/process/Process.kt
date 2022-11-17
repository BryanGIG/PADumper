package com.dumper.android.dumper.process

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.pm.ApplicationInfo
import com.dumper.android.BuildConfig

class Process(private val ctx: Context) {
    fun getAllProcess(): ArrayList<ProcessData> {
        val finalAppsBundle = ArrayList<ProcessData>()
        val activityManager = ctx.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val processInfo = activityManager.runningAppProcesses

        processInfo.forEach {
            try {
                val apps =
                    ctx.packageManager.getApplicationInfo(it.processName.substringBefore(":"), 0)
                if (!apps.isInvalid() && apps.packageName != BuildConfig.APPLICATION_ID) {
                    val data = ProcessData(
                        it.processName,
                        ctx.packageManager.getApplicationLabel(apps).toString()
                    )
                    finalAppsBundle.add(data)
                }
            } catch (_: Exception) { }
        }
        return finalAppsBundle
    }

    private fun ApplicationInfo.isInvalid() = (flags and ApplicationInfo.FLAG_STOPPED != 0) || (flags and ApplicationInfo.FLAG_SYSTEM != 0)
}