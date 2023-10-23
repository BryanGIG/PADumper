package com.dumper.android.dumper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File

enum class Arch(val value: String) {
    UNKNOWN("Unknown"),
    ARCH_32BIT("armeabi-v7a"),
    ARCH_64BIT("arm64-v8a")
}

object Fixer {


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

    /**
     * Run SoFixer
     * @param fixerPath soFixer path
     * @param dumpFile file to dump
     * @param startAddress the start address of the dump
     * @return pair of results inputStream, errorStream
     */
    fun fixDump(
        filesDir: String,
        arch: Arch,
        dumpFile: File,
        startAddress: String,
        onSuccess: (input: String) -> Unit,
        onError: (err: String) -> Unit
    ) {
        val proc = ProcessBuilder(
            listOf(
                "$filesDir/${arch.value}/fixer",
                dumpFile.path,
                "${dumpFile.parent}/${dumpFile.nameWithoutExtension}_fix.${dumpFile.extension}",
                "0x$startAddress"
            )
        )
            .redirectErrorStream(true)
            .start()

        runBlocking {
            listOf(
                async(Dispatchers.IO) {
                    val input = proc.inputStream.reader()
                    var char: Int
                    while (input.read().also { char = it } >= 0) {
                        onSuccess(char.toChar().toString())
                    }
                },
                async(Dispatchers.IO) {
                    val input = proc.errorStream.reader()
                    var char: Int;
                    while (input.read().also { char = it } >= 0) {
                        onError(char.toChar().toString())
                    }
                }
            ).awaitAll()
        }
    }
}