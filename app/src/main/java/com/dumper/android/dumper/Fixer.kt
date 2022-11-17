package com.dumper.android.dumper

import android.content.Context
import com.dumper.android.BuildConfig
import com.topjohnwu.superuser.Shell
import java.io.File

object Fixer {

    /**
     * Extract SoFixer into filesDir and
     * set permissions to 777 so the file can be executed
     */
    fun extractLibs(ctx: Context) {
        val libs = ctx.assets.list("SoFixer")
        libs?.forEach { lib ->
            ctx.assets.open("SoFixer/$lib").use { input ->
                File(ctx.filesDir, lib).outputStream().use { output ->
                    input.copyTo(output)
                    Shell.cmd("chmod 777 ${ctx.filesDir.absolutePath}/$lib").exec()
                }
            }
        }
    }

    /**
     * Run SoFixer
     * @param dumpFile the file to dump
     * @param startAddress the start address of the dump
     * @param is32 if the dump is 32 bit or 64 bit
     * @return List of strings containing the results of the SoFixer
     */
    fun fixDump(
        dumpFile: File,
        startAddress: String, is32: Boolean
    ): Array<List<String>> {
        val outList = mutableListOf<String>()
        val errList = mutableListOf<String>()
        val fixerPath = File("/data/data/${BuildConfig.APPLICATION_ID}/files", if (is32) "SoFixer32" else "SoFixer64").absolutePath
        ProcessBuilder(
            listOf(
                fixerPath,
                "-s",
                dumpFile.path,
                "-o",
                "${dumpFile.parent}/${dumpFile.nameWithoutExtension}_fix.${dumpFile.extension}",
                "-m",
                "0x$startAddress"
            )
        )
            .redirectErrorStream(true)
            .start().let { proc ->
                proc.waitFor()
                proc.inputStream.bufferedReader().use { buff ->
                    buff.forEachLine {
                        outList.add(it)
                    }
                }
                proc.errorStream.bufferedReader().use { buff ->
                    buff.forEachLine {
                        errList.add(it)
                    }
                }
            }
        return arrayOf(outList, errList)
    }
}