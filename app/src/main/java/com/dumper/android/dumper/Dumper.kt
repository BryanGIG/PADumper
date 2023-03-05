package com.dumper.android.dumper

import android.content.Context
import android.os.Environment
import com.anggrayudi.storage.extension.closeStreamQuietly
import com.anggrayudi.storage.file.createNewFileIfPossible
import com.dumper.android.dumper.process.Process
import com.dumper.android.utils.DEFAULT_DIR
import com.dumper.android.utils.removeNullChar
import com.dumper.android.utils.toHex
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class Dumper(private val pkg: String) {
    private val mem = Memory(pkg)
    var file: String = ""

    private fun dumpFileRoot(autoFix: Boolean, fixerPath: String) : StringBuilder {
        if (Shell.isAppGrantedRoot() == false)
            throw IllegalAccessException("The method need to be executed from root services")

        val log = StringBuilder()

        val outputDir = File("/sdcard/$DEFAULT_DIR/$pkg")
        if (!outputDir.exists())
            outputDir.mkdirs()

        val outputFile = File("${outputDir.absolutePath}/${mem.sAddress.toHex()}-${mem.eAddress.toHex()}-$file")
        if (!outputDir.exists())
            outputFile.createNewFileIfPossible()

        val inputChannel = RandomAccessFile("/proc/${mem.pid}/mem", "r").channel

        writeChannelIntoFile(inputChannel, outputFile)

        if (!file.contains(".dat") && autoFix) {
            log.appendLine("Fixing...")
            val fixer = Fixer.fixDump(fixerPath, outputFile, mem.sAddress.toHex())
            // Check output fixer and error fixer
            if (fixer.first.isNotEmpty()) {
                log.appendLine("Fixer output : \n${fixer.first.joinToString("\n")}")
            }
            if (fixer.second.isNotEmpty()) {
                log.appendLine("Fixer error : \n${fixer.second.joinToString("\n")}")
            }
        }
        log.appendLine("Output: ${outputFile.parent}")
        return log
    }

    private fun dumpFileNonRoot(ctx: Context, autoFix: Boolean, fixerPath: String) : StringBuilder {
        val log = StringBuilder()

        val outputDir = File(ctx.filesDir, "temp")
        if (!outputDir.exists())
            outputDir.mkdirs()

        val outputFile = File(outputDir, "${mem.sAddress.toHex()}-${mem.eAddress.toHex()}-$file")
        if (!outputDir.exists())
            outputFile.createNewFile()

        val inputChannel = RandomAccessFile("/proc/${mem.pid}/mem", "r").channel

        writeChannelIntoFile(inputChannel, outputFile)

        if (!file.contains(".dat") && autoFix) {
            log.appendLine("Fixing...")
            val fixer = Fixer.fixDump(fixerPath, outputFile, mem.sAddress.toHex())
            // Check output fixer and error fixer
            if (fixer.first.isNotEmpty()) {
                log.appendLine("Fixer output : \n${fixer.first.joinToString("\n")}")
            }
            if (fixer.second.isNotEmpty()) {
                log.appendLine("Fixer error : \n${fixer.second.joinToString("\n")}")
            }
        }

        val fileOutPath = listOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DEFAULT_DIR, pkg.removeNullChar())
        val fileOutputDir = File(fileOutPath.joinToString(File.separator))
        if (!fileOutputDir.exists())
            fileOutputDir.mkdirs()

        outputDir.copyRecursively(fileOutputDir, true, onError = { file, exc ->
            log.appendLine("[ERROR] Failed to copy: ${file.name}\n${exc.stackTraceToString()}")
            OnErrorAction.TERMINATE
        })

        log.appendLine("Output: $fileOutputDir")
        return log
    }

    /**
     * Dump the memory to a file
     *
     * @param ctx pass null if using root, vice versa
     * @param autoFix if `true` the dumped file will be fixed after dumping
     * @param flagCheck if `true` the dumped file will be checked for flags/
     * @return log of the dump
     */
    fun dumpFile(ctx: Context?, autoFix: Boolean, fixerPath: String, flagCheck: Boolean): String {
        val log = StringBuilder()
        try {
            mem.pid = Process.getProcessID(pkg) ?: throw Exception("Process not found!")

            log.appendLine("PID : ${mem.pid}")
            log.appendLine("FILE : $file")

            val map = parseMap(flagCheck)
            mem.sAddress = map.first
            mem.eAddress = map.second
            mem.size = mem.eAddress - mem.sAddress

            log.appendLine("Start Address : ${mem.sAddress.toHex()}")
            if (mem.sAddress < 1L) {
                log.appendLine("[ERROR] invalid Start Address!")
                return log.toString()
            }

            log.appendLine("End Address : ${mem.eAddress.toHex()}")
            if (mem.eAddress < 1L) {
                log.appendLine("[ERROR] invalid End Address!")
                return log.toString()
            }

            log.appendLine("Size Memory : ${mem.size}")
            if (mem.size < 1L) {
                log.appendLine("[ERROR] invalid memory size!")
                return log.toString()
            }

            if (ctx == null)
                log.appendLine(dumpFileRoot(autoFix, fixerPath))
             else
                log.appendLine(dumpFileNonRoot(ctx, autoFix, fixerPath))

            log.appendLine("Dump Success")
        } catch (e: Exception) {
            log.appendLine("[ERROR] ${e.stackTraceToString()}")
            e.printStackTrace()
        }
        return log.toString()
    }

    private fun writeChannelIntoFile(inputChannel: FileChannel, file: File) {

        val outputStream = file.outputStream()

        var bytesWritten = 0
        val buffer = ByteBuffer.allocate(1024)
        while (bytesWritten < mem.size) {
            val bytesReaded = inputChannel.read(buffer, mem.sAddress + bytesWritten)
            outputStream.write(buffer.array(), 0, bytesReaded)
            buffer.clear()
            bytesWritten += bytesReaded
        }

        outputStream.flush()
        outputStream.closeStreamQuietly()
        inputChannel.close()
    }

    /**
     * Parsing the memory map
     *
     * @throws FileNotFoundException if required file is not found in memory map
     */
    private fun parseMap(checkFlag: Boolean): Pair<Long, Long> {
        val files = File("/proc/${mem.pid}/maps")
        if (!files.exists()) {
            throw Exception("Failed To Open : ${files.path}")
        }
        val lines = files.readLines()

        val lineStart = lines.find {
            val map = MapLinux(it)
            if (file.contains(".so")) {
                if (checkFlag)
                    map.getPerms().contains("r-xp") && map.getPath().contains(file)
                else {
                    map.getPath().contains(file)
                }
            } else {
                map.getPath().contains(file)
            }
        } ?: throw Exception("Unable find baseAddress of $file")

        val mapStart = MapLinux(lineStart)

        val lineEnd = lines.findLast {
            val map = MapLinux(it)
            mapStart.getInode() == map.getInode() && mapStart.getDev() == map.getDev()
        } ?: throw Exception("Unable find endAddress of $file")

        val mapEnd = MapLinux(lineEnd)
        return Pair(mapStart.getStartAddress(), mapEnd.getEndAddress())
    }
}