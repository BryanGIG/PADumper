package com.dumper.android.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable

const val TAG = "PADumper"
const val DEFAULT_DIR = "PADumper"

fun Long.toHex(): String {
    return this.toString(16)
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompact(key: String): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, T::class.java)
    } else {
        getParcelableArrayList(key)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.getApplicationInfoCompact(packageName: String, flags: Int): ApplicationInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        getApplicationInfo(packageName, flags)
    }
}

fun ApplicationInfo.isInvalid() =
    (flags and ApplicationInfo.FLAG_STOPPED != 0) || (flags and ApplicationInfo.FLAG_SYSTEM != 0)


fun String.removeNullChar(): String = replace("\u0000", "")