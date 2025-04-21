package com.dumper.android.dumper

import android.os.Parcelable
import com.dumper.android.utils.MB_to_Bytes
import kotlinx.parcelize.Parcelize

@Parcelize
data class DumperConfig(
    val processName: String,
    var file: String,
    var autoFix: Boolean,
    val isDumpMetadata: Boolean,
    val limitMaxDumpSize: Long = 1000.MB_to_Bytes
): Parcelable
