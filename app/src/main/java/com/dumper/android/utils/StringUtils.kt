package com.dumper.android.utils

fun String.removeNullChar(): String = replace("\u0000", "")

fun String.getFileName(): String = substringAfterLast("/")

fun String.getFileNameWithoutExtension(): String = substringBeforeLast(".")
