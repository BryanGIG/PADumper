package com.dumper.android.dumper.sofixer

import android.content.Context
import java.io.File

object FixerUtils {

    /**
     * Extract folder assets into filesDir and
     * set permissions to 777 so the file can be executed
     */
    fun extractLibs(ctx: Context) {
        val filesDir = ctx.filesDir
        val list = listOf("armeabi-v7a", "arm64-v8a")
        list.forEach { arch ->

            val archDir = File(filesDir, arch)
            if (!archDir.exists())
                archDir.mkdirs()

            ctx.assets.list(arch)
                ?.forEach { v ->
                    val file = File(archDir, v)
                    if (!file.exists()) {
                        ctx.assets.open("$arch/$v").copyTo(file.outputStream())
                        file.setExecutable(true, false)
                    }
                }
        }
    }
}