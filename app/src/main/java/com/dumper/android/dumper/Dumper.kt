package com.dumper.android.dumper

import android.content.Context
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.extension.closeStreamQuietly
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.MimeType
import com.anggrayudi.storage.file.createNewFileIfPossible
import com.anggrayudi.storage.media.FileDescription
import com.anggrayudi.storage.media.MediaStoreCompat
import com.dumper.android.dumper.process.Process
import com.dumper.android.utils.DEFAULT_DIR
import com.dumper.android.utils.toHex
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class Dumper(private val pkg: String) {
    private val mem = Memory(pkg)
    var file: String = ""

    /**
     * Dump the memory to a file
     *
     * @param ctx pass null if using root, vice versa
     * @param autoFix if `true` the dumped file will be fixed after dumping
     * @param flagCheck if `true` the dumped file will be checked for flags/
     * @return log of the dump
     */
    fun dumpFile(ctx: Context?, autoFix: Boolean, fixerPath: String, flagCheck: Boolean, output: String = ""): String {
        val log = StringBuilder()
        try {
            mem.pid = Process.getProcessID(pkg) ?: throw Exception("Process not found!")

            log.appendLine("PID : ${mem.pid}")
            log.appendLine("FILE : $file")

            val map = parseMap(flagCheck)
            mem.sAddress = map.first()
            mem.eAddress = map.last()
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

            val outputDir = File("$output/$DEFAULT_DIR/$pkg")
            if (!outputDir.exists())
                outputDir.mkdirs()

            val outputFile = File("${outputDir.path}/${mem.sAddress.toHex()}-${mem.eAddress.toHex()}-$file")
            if (!outputDir.exists())
                outputFile.createNewFileIfPossible()

            val inputChannel = RandomAccessFile("/proc/${mem.pid}/mem", "r").channel

            val outputPath = writeChannelIntoFile(ctx, inputChannel, outputFile)

            if (!file.contains(".dat") && autoFix) {
                log.appendLine("Fixing...")
                val fixer = Fixer.fixDump(fixerPath, File(outputPath), mem.sAddress.toHex())
                // Check output fixer and error fixer
                if (fixer[0].isNotEmpty()) {
                    log.appendLine("Fixer output : \n${fixer[0].joinToString("\n")}")
                }
                if (fixer[1].isNotEmpty()) {
                    log.appendLine("Fixer error : \n${fixer[1].joinToString("\n")}")
                }
            }

            log.appendLine("Dump Success")
            log.appendLine("Output: ${File(outputPath).parent}")
        } catch (e: Exception) {
            log.appendLine("[ERROR] ${e.stackTraceToString()}")
            e.printStackTrace()
        }
        return log.toString()
    }

    private fun writeChannelIntoFile(ctx: Context?, inputChannel: FileChannel, file: File) : String {

        val pairOutputStream: Pair<String, OutputStream> = if (ctx == null) {
            Pair(file.absolutePath,file.outputStream())
        } else {
            val mediaStore = MediaStoreCompat.createDownload(ctx, FileDescription(file.nameWithoutExtension, "$DEFAULT_DIR/${file.parentFile?.name}", MimeType.BINARY_FILE), CreateMode.REPLACE)
            requireNotNull(mediaStore)
            Pair(SimpleStorage.externalStoragePath + File.separator + mediaStore.basePath, requireNotNull(mediaStore.openOutputStream(false)))
        }

        var bytesWritten = 0
        val buffer = ByteBuffer.allocate(1024)
        while (bytesWritten < mem.size) {
            val bytesReaded = inputChannel.read(buffer, mem.sAddress + bytesWritten)
            pairOutputStream.second.write(buffer.array(), 0, bytesReaded)
            buffer.clear()
            bytesWritten += bytesReaded
        }

        pairOutputStream.second.closeStreamQuietly()
        return pairOutputStream.first
    }

    /**
     * Parsing the memory map
     *
     * @throws FileNotFoundException if required file is not found in memory map
     */
    private fun parseMap(checkFlag: Boolean): LongArray {
        val files = File("/proc/${mem.pid}/maps")
        if (files.exists()) {
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
            return longArrayOf(mapStart.getStartAddress(), mapEnd.getEndAddress())
        } else {
            throw Exception("Failed To Open : ${files.path}")
        }
    }
}