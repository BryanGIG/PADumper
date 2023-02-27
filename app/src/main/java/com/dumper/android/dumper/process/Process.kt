package com.dumper.android.dumper.process

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.pm.ApplicationInfo
import androidx.core.text.isDigitsOnly
import com.dumper.android.BuildConfig
import com.dumper.android.utils.getApplicationInfoCompact
import java.io.File
import java.io.FileNotFoundException

object Process {

    fun getAllProcess(ctx: Context, isRoot: Boolean): ArrayList<ProcessData> {
        val finalAppsBundle = ArrayList<ProcessData>()
        if (isRoot) {
            val activityManager = ctx.getSystemService(ACTIVITY_SERVICE) as ActivityManager

            activityManager.runningAppProcesses.forEach {
                try {
                    val apps = ctx.packageManager.getApplicationInfoCompact(
                        it.processName.substringBefore(":"), 0
                    )
                    if (!apps.isInvalid() && apps.packageName != BuildConfig.APPLICATION_ID) {
                        val data = ProcessData(
                            it.processName,
                            ctx.packageManager.getApplicationLabel(apps).toString()
                        )
                        finalAppsBundle.add(data)
                    }
                } catch (_: Exception) { }
            }
        } else {
            val proc = File("/proc")
            if (proc.exists()) {
                val dPID = proc.listFiles()
                if (!dPID.isNullOrEmpty()) {
                    for (line in dPID) {
                        if (line.name.isDigitsOnly()) {
                            val comm = File("${line.path}/comm")
                            val cmdline = File("${line.path}/cmdline")
                            if (comm.exists() && cmdline.exists()) {

                                val processName = comm.readText(Charsets.UTF_8)
                                val processPkg = cmdline.readText(Charsets.UTF_8)

                                if (processPkg != "sh" && processPkg != BuildConfig.APPLICATION_ID) {
                                    val data = ProcessData(processPkg, processName)
                                    finalAppsBundle.add(data)
                                }
                            }
                        }
                    }
                }
            }
        }
        return finalAppsBundle
    }

    /**
     * Get the process ID
     *
     * @throws Exception if dir "/proc" is empty
     * @throws FileNotFoundException if "/proc" failed to open
     */
    fun getProcessID(pkg: String): Int? {
        val proc = File("/proc")
        if (proc.exists()) {
            val dPID = proc.listFiles()
            if (!dPID.isNullOrEmpty()) {
                dPID.firstOrNull {
                    if (it.name.isDigitsOnly()) {
                        val cmdline = File("${it.path}/cmdline")
                        if (cmdline.exists()) {
                            val textCmd = cmdline.readText(Charsets.UTF_8)
                            if (textCmd.contains(pkg)) {
                                return@firstOrNull true
                            }
                        }
                    }
                    return@firstOrNull false
                }?.let {
                    return it.name.toInt()
                }
            }
        }
        return null
    }

    private fun ApplicationInfo.isInvalid() =
        (flags and ApplicationInfo.FLAG_STOPPED != 0) || (flags and ApplicationInfo.FLAG_SYSTEM != 0)
}