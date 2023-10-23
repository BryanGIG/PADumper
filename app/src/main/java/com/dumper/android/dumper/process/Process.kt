package com.dumper.android.dumper.process

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import androidx.core.text.isDigitsOnly
import com.dumper.android.BuildConfig
import com.dumper.android.utils.getApplicationInfoCompact
import com.dumper.android.utils.isInvalid
import com.dumper.android.utils.removeNullChar
import java.io.File

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
                } catch (_: Exception) {
                }
            }
        } else {
            val proc = File("/proc")
            if (!proc.exists())
                return finalAppsBundle

            val dPID = proc.listFiles()
            if (dPID.isNullOrEmpty())
                return finalAppsBundle

            for (line in dPID) {
                if (!line.name.isDigitsOnly())
                    continue;

                val comm = File("${line.path}/comm")
                val cmdline = File("${line.path}/cmdline")
                if (!comm.exists() || !cmdline.exists())
                    continue;

                val processName = comm.readText(Charsets.UTF_8).removeNullChar()
                val processPkg = cmdline.readText(Charsets.UTF_8).removeNullChar()

                if (processPkg != "sh" && !processPkg.contains(BuildConfig.APPLICATION_ID)) {
                    val data = ProcessData(processPkg, processName)
                    finalAppsBundle.add(data)
                }
            }
        }
        return finalAppsBundle
    }

    /**
     * Get the PID
     * @return pid of process or null if process id is not found
     */
    fun getProcessID(pkg: String): Int? {
        val proc = File("/proc")
        if (!proc.exists())
            return null

        val dPID = proc.listFiles()
        if (dPID.isNullOrEmpty())
            return null

        dPID.find {
            if (it.name.isDigitsOnly()) {
                val cmdline = File("${it.path}/cmdline")
                if (cmdline.exists()) {
                    val textCmd = cmdline.readText(Charsets.UTF_8)
                    if (textCmd.contains(pkg)) {
                        return@find true
                    }
                }
            }
            return@find false
        }?.let {
            return it.name.toInt()
        }
        return null
    }
}