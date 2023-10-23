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

    fun getAllProcess(ctx: Context, isRoot: Boolean) =
        if (isRoot)
            getAllProcessRoot(ctx)
        else
            getAllProcessNoRoot()


    private fun getAllProcessRoot(ctx: Context): List<ProcessData> {
        val activityManager = ctx.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return activityManager.runningAppProcesses
            .mapNotNull {
                runCatching {
                    val pkgManager = ctx.packageManager
                    val app = pkgManager.getApplicationInfoCompact(it.processName, 0)
                    if (!app.isInvalid() && app.packageName != BuildConfig.APPLICATION_ID) {
                        val appLabel = pkgManager.getApplicationLabel(app).toString()
                        ProcessData(it.processName, appLabel)
                    } else {
                        null
                    }
                }.getOrNull()
            }
    }

    private fun getAllProcessNoRoot(): List<ProcessData> {
        return File("/proc")
            .listFiles().orEmpty()
            .filter { it.name.isDigitsOnly() }
            .mapNotNull {
                val comm = File("${it.path}/comm")
                val cmdline = File("${it.path}/cmdline")
                if (!comm.exists() || !cmdline.exists())
                    return@mapNotNull null

                val processName = comm.readText(Charsets.UTF_8).removeNullChar()
                val processPkg = cmdline.readText(Charsets.UTF_8).removeNullChar()

                if (processPkg != "sh" && !processPkg.contains(BuildConfig.APPLICATION_ID)) {
                    ProcessData(processPkg, processName)
                } else {
                    null
                }
            }
    }

    /**
     * Get the PID
     * @return pid of process or null if process id is not found
     */
    fun getProcessID(pkg: String): Int? {
        return File("/proc")
            .listFiles().orEmpty()
            .filter { it.name.isDigitsOnly() }
            .find {
                val cmdline = File("${it.path}/cmdline")
                if (!cmdline.exists())
                    return@find false

                val textCmd = cmdline.readText(Charsets.UTF_8)
                return@find textCmd.contains(pkg)
            }?.name?.toIntOrNull()
    }
}