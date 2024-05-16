package com.dumper.android.dumper

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import com.dumper.android.dumper.elf.getArchELF
import com.dumper.android.dumper.elf.isELF
import com.dumper.android.dumper.maps.MapLineParser
import com.dumper.android.dumper.metadata.MetadataFinder
import com.dumper.android.dumper.process.Process
import com.dumper.android.dumper.sofixer.Fixer
import com.dumper.android.utils.DEFAULT_DIR
import com.dumper.android.utils.FileName
import com.dumper.android.utils.copyToFile
import com.dumper.android.utils.toHex
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile

class Dumper(
    private val pkg: String,
    private var file: String,
    private val isDumpMetadata: Boolean
) {
    private var pid: Int? = null

    /**
     * Dump the memory to a file
     *
     * @param ctx pass null if using root, vice versa
     * @param autoFix if `true` the dumped file will be fixed after dumping
     * @param outLog output handler
     * @return 0 if success, -1 if failed
     */
    @SuppressLint("SdCardPath")
    fun dumpFile(ctx: Context, autoFix: Boolean, outLog: OutputHandler) = runCatching {
        pid = Process.getProcessID(pkg) ?: throw Exception("Process not found!")

        outLog.appendLine("PID: $pid")
        outLog.appendLine("FILE: $file")

        val mem = parseMap() ?: throw Exception("Unable to parse map!")

        outLog.appendLine("Start Address: ${mem.getStartAddress().toHex()}")
        if (mem.getStartAddress() < 1L) {
            throw Exception("Invalid Start Address!")
        }

        outLog.appendLine("End Address: ${mem.getEndAddress().toHex()}")
        if (mem.getEndAddress() < 1L) {
            throw Exception("Invalid End Address!")
        }

        outLog.appendLine("Size Memory: ${mem.getSizeInMB()}MB (${mem.getSize()})")
        if (mem.getSize() < 1L) {
            throw Exception("Invalid memory size!")
        }

        val fileOutPath =
            if (Shell.isAppGrantedRoot() == false) {
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$DEFAULT_DIR/$pkg"
            } else {
                "/sdcard/$DEFAULT_DIR/$pkg"
            }

        val outputDir = File(fileOutPath)
        if (!outputDir.exists())
            outputDir.mkdirs()

        val outputFile = File(outputDir, "${mem.getStartAddress().toHex()}-${mem.getEndAddress().toHex()}-${mem.getPath().FileName}")
        if (!outputDir.exists())
            outputFile.createNewFile()

        dump(mem, autoFix, ctx.filesDir.absolutePath, outputFile, outLog)

        if (isDumpMetadata) {
            val metadataFinder = MetadataFinder(pid!!)
            metadataFinder.findUnityMetadata(outLog).let {
                if (it == null)
                    outLog.appendError("Unable to find global-metadata.dat")
                else {
                    outLog.appendInfo("Dumping global-metadata.dat...")
                    dump(it, outputFile = File(outputDir, "global-metadata.dat"), outLog = outLog)
                }
            }
        }

        outLog.appendLine("Output: $outputDir")
    }.onSuccess {
        outLog.appendInfo("Dump Success")
        outLog.appendLine("==========================")
        outLog.finish(0)
    }.onFailure {
        outLog.appendError(it.stackTraceToString())
        outLog.appendLine("==========================")
        outLog.finish(-1)
    }


    private fun dump(
        mem: MapLineParser,
        autoFix: Boolean = false,
        fixerPath: String? = null,
        outputFile: File,
        outLog: OutputHandler
    ) {
        RandomAccessFile("/proc/$pid/mem", "r")
            .channel
            .use {
                it.copyToFile(mem.getStartAddress(), mem.getSize(), outputFile)

                if (autoFix && fixerPath != null) {
                    val archELF = getArchELF(it, mem)
                    Fixer(fixerPath).fixELFFile(mem.getStartAddress(), archELF, outputFile, outLog)
                }
                it.close()
            }
    }

    /**
     * Parsing the memory map
     *
     * @throws FileNotFoundException failed to open /proc/{pid}/maps
     * @throws RuntimeException start or end address is not found
     */
    private fun parseMap(): MapLineParser? {
        if (pid == null) {
            throw ExceptionInInitializerError("Init Pid?")
        }

        val files = File("/proc/$pid/maps")
        if (!files.exists()) {
            throw FileNotFoundException("Failed To Open : ${files.path}")
        }

        var map: MapLineParser? = null

        files.readLines()
            .filter { it.contains(file) }
            .map { MapLineParser(it) }
            .forEach {
                if (map == null) {
                    val path = it.getPath()
                    if (!path.contains(".so")) {
                        map = it // For all other files
                    } else if (isELF(pid!!, it.getStartAddress())) {
                        map = it // Must be valid .so ELF files
                    }
                } else {
                    if (map!!.getInode() == it.getInode()) {
                        map!!.setEndAddress(it.getEndAddress())
                    }
                }
            }

        if (map == null || map?.isValid() == false)
            throw Exception("Start or End Address not found!")

        return map
    }
}