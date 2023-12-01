package com.dumper.android.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

const val TAG = "PADumper"
const val DEFAULT_DIR = "PADumper"

fun PackageManager.getApplicationInfoCompact(processName: String, flags: Int): ApplicationInfo {
    val packageName = processName.substringBefore(":")
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        getApplicationInfo(packageName, flags)
    }
}

fun ApplicationInfo.isInvalid() =
    (flags and ApplicationInfo.FLAG_STOPPED != 0) || (flags and ApplicationInfo.FLAG_SYSTEM != 0)
