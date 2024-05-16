package com.dumper.android.dumper.sofixer

import com.dumper.android.dumper.OutputHandler
import com.dumper.android.utils.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File

class Fixer(private val fixerPath: String) {

    fun fixELFFile(
        startAddress: Long,
        archELF: Arch,
        outputFile: File,
        outLog: OutputHandler
    ) {
        if (archELF == Arch.UNKNOWN)
            return

        outLog.appendInfo("Fixing...")
        outLog.appendInfo("Fixer output :")

        fixDump(
            archELF,
            outputFile,
            startAddress.toHex(),
            onSuccess = { outLog.append(it) },
            onError = { outLog.append(it) }
        )
    }

    /**
     * Run SoFixer
     */
    private fun fixDump(
        arch: Arch,
        dumpFile: File,
        startAddress: String,
        onSuccess: (input: String) -> Unit,
        onError: (err: String) -> Unit
    ) {
        val proc = ProcessBuilder(
            listOf(
                "$fixerPath/${arch.value}/fixer",
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
                    var char: Int
                    while (input.read().also { char = it } >= 0) {
                        onError(char.toChar().toString())
                    }
                }
            ).awaitAll()
        }
    }
}