package com.dumper.android.dumper

import android.content.Context
import com.topjohnwu.superuser.Shell
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


enum class Arch {
    UNKNOWN,
    ARCH_32BIT,
    ARCH_64BIT
}
object Fixer {

    private val ELFMAG =
        byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())

    /**
     * Extract SoFixer into filesDir and
     * set permissions to 777 so the file can be executed
     */
    fun extractLibs(ctx: Context) {
        val libs = ctx.assets.list("SoFixer")
        libs?.forEach { lib ->
            ctx.assets.open("SoFixer/$lib").use { input ->
                val out = File(ctx.filesDir, lib)
                if (!out.exists()) {
                    input.copyTo(out.outputStream())
                    Shell.cmd("chmod 777 ${out.absolutePath}").exec()
                }
            }
        }
    }

    fun getArchELF(mFile: FileChannel, memory: Memory) : Arch {
        val byteHeader = ByteBuffer.allocate(512)

        mFile.read(byteHeader, memory.sAddress)

        if (byteHeader[0] != ELFMAG[0] || byteHeader[1] != ELFMAG[1] ||
            byteHeader[2] != ELFMAG[2] || byteHeader[3] != ELFMAG[3]
        ) {
            return Arch.UNKNOWN
        }


        mFile.position(0) //reset pos
        return when (byteHeader[4].toInt()) {
            1 -> {
                Arch.ARCH_32BIT
            }
            2 -> {
                Arch.ARCH_64BIT
            }
            else -> {
                Arch.UNKNOWN
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
        fixerPath: String,
        dumpFile: File,
        startAddress: String
    ): Pair<List<String>, List<String>> {
        val outList = mutableListOf<String>()
        val errList = mutableListOf<String>()
        
        val proc = ProcessBuilder(
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
            .start()

        proc.waitFor()
        proc.inputStream.bufferedReader().useLines { buff ->
            outList.addAll(buff)
        }
        proc.errorStream.bufferedReader().useLines { buff ->
            errList.addAll(buff)
        }

        return Pair(outList, errList)
    }
}