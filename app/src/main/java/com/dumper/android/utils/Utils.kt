package com.dumper.android.utils

const val TAG = "PADumper"
const val DEFAULT_DIR = "/sdcard/PADumper"

fun Long.toHex(): String {
    return this.toString(16)
}

fun Long.toMB(): Long {
    return this * 1024 * 1024
}