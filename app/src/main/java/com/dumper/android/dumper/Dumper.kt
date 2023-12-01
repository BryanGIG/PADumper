package com.dumper.android.dumper

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import com.dumper.android.dumper.process.Process
import com.dumper.android.utils.*
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile

class Dumper(private val pkg: String) {
    private val mem = Memory(pkg)
    var file: String = ""

    private fun dump(autoFix: Boolean, fixerPath: String, outputFile: File, outLog: OutputHandler) {
        RandomAccessFile("/proc/${mem.pid}/mem", "r")
            .channel
            .use {
                it.copyToFile(mem.sAddress, mem.size, outputFile)

                if (autoFix) {
                    val archELF = getArchELF(it, mem)
                    fixDumpFile(fixerPath, archELF, outputFile, outLog)
                }
                it.close()
            }
    }

    private fun fixDumpFile(
        fixerPath: String,
        archELF: Arch,
        outputFile: File,
        outLog: OutputHandler
    ) {
        if (archELF == Arch.UNKNOWN)
            return

        outLog.appendLine("Fixing...")
        outLog.appendLine("Fixer output :")
        Fixer.fixDump(
            fixerPath,
            archELF,
            outputFile,
            mem.sAddress.toHex(),
            onSuccess = { outLog.append(it) },
            onError = { outLog.append(it) }
        )
    }

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
        mem.pid = Process.getProcessID(pkg) ?: throw Exception("Process not found!")

        outLog.appendLine("PID : ${mem.pid}")
        outLog.appendLine("FILE : $file")

        val map = parseMap()
        mem.sAddress = map.first
        mem.eAddress = map.second
        mem.size = mem.eAddress - mem.sAddress

        outLog.appendLine("Start Address : ${mem.sAddress.toHex()}")
        if (mem.sAddress < 1L) {
            throw Exception("Invalid Start Address!")
        }

        outLog.appendLine("End Address : ${mem.eAddress.toHex()}")
        if (mem.eAddress < 1L) {
            throw Exception("Invalid End Address!")
        }

        outLog.appendLine("Size Memory : ${mem.size}")
        if (mem.size < 1L) {
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

        val outputFile = File(
            outputDir,
            "${mem.sAddress.toHex()}-${mem.eAddress.toHex()}-${mem.path.FileName}"
        )
        if (!outputDir.exists())
            outputFile.createNewFile()

        dump(autoFix, ctx.filesDir.absolutePath, outputFile, outLog)

        outLog.appendLine("Output: $outputDir")
    }.onSuccess {
        outLog.appendLine("Dump Success")
        outLog.appendLine("==========================")
        outLog.finish(0)
    }.onFailure {
        outLog.appendError(it.message ?: "Unknown Error")
        outLog.appendLine("==========================")
        outLog.finish(-1)
    }


    /**
     * Parsing the memory map
     *
     * @throws FileNotFoundException failed to open /proc/{pid}/maps
     * @throws RuntimeException start or end address is not found
     */
    private fun parseMap(): Pair<Long, Long> {
        val files = File("/proc/${mem.pid}/maps")
        if (!files.exists()) {
            throw FileNotFoundException("Failed To Open : ${files.path}")
        }

        var mapStart: MapParser? = null
        var mapEnd: MapParser? = null

        files.readLines()
            .map { MapParser(it) }
            .forEach { map ->
                if (mapStart == null) {
                    if (map.getPath().contains(file)) {
                        if (file.contains(".so")) {
                            if (isELF(mem.pid, map.getStartAddress())) {
                                mapStart = map
                                mem.path = map.getPath()
                            }
                        } else {
                            mapStart = map
                            mem.path = map.getPath()
                        }
                    }
                } else {
                    if (mapStart!!.getInode() == map.getInode()) {
                        mapEnd = map
                    }
                }
            }


        if (mapStart == null || mapEnd == null)
            throw Exception("Start or End Address not found!")

        return mapStart!!.getStartAddress() to mapEnd!!.getEndAddress()
    }
}