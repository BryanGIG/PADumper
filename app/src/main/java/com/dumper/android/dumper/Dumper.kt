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
import com.dumper.android.utils.Bytes_to_MB
import com.dumper.android.utils.DEFAULT_DIR
import com.dumper.android.utils.FileName
import com.dumper.android.utils.copyToFile
import com.dumper.android.utils.toHex
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

class Dumper(private val context: Context, private val config: DumperConfig, private val outputHandler: OutputHandler) {
    private var pid: Int? = null

    /**
     * Dump the memory to a file
     * @return 0 if success, -1 if failed
     */
    @SuppressLint("SdCardPath")
    fun dumpFile(): Int {
        try {
            pid = Process.getProcessID(config.processName) ?: throw Exception("Process not found!")
            outputHandler.appendLine("==========================")
            outputHandler.appendLine("PROCESS: ${config.processName}")
            outputHandler.appendLine("PID: $pid")
            outputHandler.appendLine("FILE: ${config.file}")

            val mem = parseMap() ?: throw Exception("Unable to parse map!")

            outputHandler.appendLine("Start Address: ${mem.getStartAddress().toHex()}")
            if (mem.getStartAddress() < 1L) {
                throw Exception("Invalid Start Address!")
            }

            outputHandler.appendLine("End Address: ${mem.getEndAddress().toHex()}")
            if (mem.getEndAddress() < 1L) {
                throw Exception("Invalid End Address!")
            }

            outputHandler.appendLine("Size Memory: ${mem.getSize().Bytes_to_MB}MB (${mem.getSize()})")
            if (mem.getSize() < 1L) {
                throw Exception("Invalid memory size!")
            }

            val fileOutPath =
                if (Shell.isAppGrantedRoot() == false) {
                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$DEFAULT_DIR/${config.processName}"
                } else {
                    "/sdcard/$DEFAULT_DIR/${config.processName}"
                }

            val outputDir = File(fileOutPath)
            if (!outputDir.exists())
                outputDir.mkdirs()

            val outputFile = File(outputDir, "${mem.getStartAddress().toHex()}-${mem.getEndAddress().toHex()}-${mem.getPath().FileName}")
            if (!outputDir.exists())
                outputFile.createNewFile()

            dump(mem, context.filesDir.absolutePath, outputFile, outputHandler)

            if (config.isDumpMetadata) {
                val metadataFinder = MetadataFinder(pid!!)
                metadataFinder.findUnityMetadata(outputHandler).let {
                    if (it == null)
                        outputHandler.appendError("Unable to find global-metadata.dat")
                    else {
                        outputHandler.appendInfo("Dumping global-metadata.dat...")
                        dump(it, outputFile = File(outputDir, "global-metadata.dat"), outLog = outputHandler)
                    }
                }
            }

            outputHandler.appendLine("Output: $outputDir")
            outputHandler.appendInfo("Dump Success")
            outputHandler.appendLine("==========================")
            outputHandler.finish(0)
            return 0
        } catch (e: Throwable) {
            outputHandler.appendError(e.stackTraceToString())
            outputHandler.appendLine("==========================")
            outputHandler.finish(-1)
            return -1
        }
    }

    private fun dump(
        mem: MapLineParser,
        fixerPath: String? = null,
        outputFile: File,
        outLog: OutputHandler
    ) {
        val channel = RandomAccessFile("/proc/$pid/mem", "r").channel

        if (mem.getSize() > config.limitMaxDumpSize) {
            outLog.appendWarning("Memory size is too large!, dump maybe incorrect...")
            File("/proc/$pid/map_files").listFiles()
                ?.find { it.name.contains("${mem.getStartAddress().toHex()}-") }
                ?.let {
                    it.copyTo(outputFile, true)
                    outLog.appendLine("Dumped size: ${outputFile.length().Bytes_to_MB}MB (${outputFile.length()})")
                    if (config.autoFix) {
                        fixDump(channel, mem, outputFile, fixerPath, outLog)
                        channel.close()
                    }
                    return
                }

            outLog.appendError("Unable to dump this file, skipping...")
            return
        }

        channel.use {
            it.copyToFile(mem.getStartAddress(), mem.getSize(), outputFile)
            if (config.autoFix) {
                fixDump(it, mem, outputFile, fixerPath, outLog)
            }
            it.close()
        }
    }

    private fun fixDump(channel: FileChannel, mem: MapLineParser, outputFile: File, fixerPath: String?, outLog: OutputHandler) {
        if (fixerPath == null) {
            outLog.appendError("Fixer Path is null, skipping...")
            return
        }

        val archELF = getArchELF(channel, mem)
        Fixer(fixerPath).fixELFFile(mem.getStartAddress(), archELF, outputFile, outLog)
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
            .filter { it.contains(config.file) }
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

