package com.dumper.android.utils

fun String.removeNullChar(): String = replace("\u0000", "")

val String.FileName: String
    get() = substringAfterLast("/")

fun Long.toHex(): String {
    return this.toString(16)
}