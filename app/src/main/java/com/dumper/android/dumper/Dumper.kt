package com.dumper.android.dumper

import com.dumper.android.dumper.process.Process
import com.dumper.android.utils.DEFAULT_DIR
import com.dumper.android.utils.toHex
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class Dumper(private val pkg: String) {
    private val mem = Memory(pkg)
    var file: String = ""

    /**
     * Dump the memory to a file
     *
     * @param autoFix if `true` the dumped file will be fixed after dumping
     * @param flagCheck if `true` the dumped file will be checked for flags/
     * @return log of the dump
     */
    fun dumpFile(autoFix: Boolean, is32Bit: Boolean, flagCheck: Boolean, output: String): String {
        val log = StringBuilder()
        try {
            mem.pid = Process.getProcessID(pkg) ?: throw Exception("Process not found!")

            log.appendLine("PID : ${mem.pid}")
            log.appendLine("FILE : $file")

            val map = parseMap(flagCheck)
            map.forEach {
                if (it == 0L) {
                    log.append("[ERROR] Failed to get memory map of $pkg\n")
                    return@forEach
                }
            }

            mem.sAddress = map.first()
            mem.eAddress = map.last()
            mem.size = mem.eAddress - mem.sAddress

            log.appendLine("Start Address : ${mem.sAddress.toHex()}")
            log.appendLine("End Address : ${mem.eAddress.toHex()}")
            log.appendLine("Size Memory : ${mem.size}")

            if (mem.sAddress > 1L && mem.eAddress > 1L) {
                val path = File("$DEFAULT_DIR/$pkg")
                if (!path.exists()) path.mkdirs()

            val pathOut = File("${path.absolutePath}/${mem.sAddress.toHex()}-${mem.eAddress.toHex()}-$file")
            val outputStream = pathOut.outputStream()

            val inputAccess = RandomAccessFile("/proc/${mem.pid}/mem", "r")
            inputAccess.channel.let { fileChannel ->
                var bytesWritten = 0
                val buffer = ByteBuffer.allocate(1024)
                while (bytesWritten < mem.size) {
                    val bytesReaded = fileChannel.read(buffer, mem.sAddress + bytesWritten)
                    outputStream.write(buffer.array(), 0, bytesReaded)
                    buffer.clear()
                    bytesWritten += bytesReaded
                }
            }

            if (!file.contains(".dat") && autoFix) {
                log.appendLine("Fixing...")
                val fixer = Fixer.fixDump(pathOut, mem.sAddress.toHex(), is32Bit)
                // Check output fixer and error fixer
                if (fixer[0].isNotEmpty()) {
                    log.appendLine("Fixer output : \n${fixer[0].joinToString("\n")}")
                }
                if (fixer[1].isNotEmpty()) {
                    log.appendLine("Fixer error : \n${fixer[1].joinToString("\n")}")
                }
            }

            log.appendLine("Dump Success")
            log.appendLine("Output: ${pathOut.parent}")
        } catch (e: Exception) {
            log.appendLine("[ERROR] $e")
            e.printStackTrace()
        }
        return log.toString()
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