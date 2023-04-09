package com.dumper.android.dumper

import android.content.Context
import android.os.Environment
import com.dumper.android.dumper.process.Process
import com.dumper.android.utils.DEFAULT_DIR
import com.dumper.android.utils.copyToFile
import com.dumper.android.utils.removeNullChar
import com.dumper.android.utils.toHex
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile

class Dumper(private val pkg: String) {
    private val mem = Memory(pkg)
    var file: String = ""

    private fun dumpFileRoot(autoFix: Boolean, fixerPath: String, outLog: OutputHandler) {
        if (Shell.isAppGrantedRoot() == false)
            throw IllegalAccessException("The method need to be executed from root services")

        val outputDir = File("/sdcard/$DEFAULT_DIR/$pkg")
        if (!outputDir.exists())
            outputDir.mkdirs()

        val outputFile =
            File("${outputDir.absolutePath}/${mem.sAddress.toHex()}-${mem.eAddress.toHex()}-$file")
        if (!outputDir.exists())
            outputFile.createNewFile()

        dump(autoFix, fixerPath, outputFile, outLog)

        outLog.appendLine("Output: ${outputFile.parent}")
    }

    private fun dumpFileNonRoot(ctx: Context, autoFix: Boolean, fixerPath: String, outLog: OutputHandler) {

        val outputDir = File(ctx.filesDir, "temp")
        if (!outputDir.exists())
            outputDir.mkdirs()

        val outputFile = File(outputDir, "${mem.sAddress.toHex()}-${mem.eAddress.toHex()}-$file")
        if (!outputDir.exists())
            outputFile.createNewFile()

        dump(autoFix, fixerPath, outputFile, outLog)

        val fileOutPath = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DEFAULT_DIR,
            pkg.removeNullChar()
        )
        val fileOutputDir = File(fileOutPath.joinToString(File.separator))
        if (!fileOutputDir.exists())
            fileOutputDir.mkdirs()

        outputDir.copyRecursively(fileOutputDir, true, onError = { file, exc ->
            outLog.appendLine("[ERROR] Failed to copy: ${file.name}\n${exc.stackTraceToString()}")
            OnErrorAction.TERMINATE
        })

        outLog.appendLine("Output: $fileOutputDir")
    }

    private fun dump(autoFix: Boolean,fixerPath: String, outputFile: File, outLog: OutputHandler) {

        val inputChannel = RandomAccessFile("/proc/${mem.pid}/mem", "r").channel

        inputChannel.copyToFile(mem.sAddress, mem.size, outputFile)

        if (autoFix) {
            val archELF = Fixer.getArchELF(inputChannel, mem)
            fixDumpFile(fixerPath, archELF, outputFile, outLog)
        }
        inputChannel.close()
    }

    private fun fixDumpFile(fixerPath: String, archELF: Arch, outputFile: File, outLog: OutputHandler) {
        if (archELF == Arch.UNKNOWN)
            return

        outLog.appendLine("Fixing...")

        val fixerELF = fixerPath + when (archELF) {
            Arch.ARCH_32BIT -> "32"
            Arch.ARCH_64BIT -> "64"
            else -> "" //just to disable the warning
        }

        val fixer = Fixer.fixDump(fixerELF, outputFile, mem.sAddress.toHex())
        // Check output fixer and error fixer
        if (fixer.first.isNotEmpty()) {
            outLog.appendLine("Fixer output : \n${fixer.first.joinToString("\n")}")
        }
        if (fixer.second.isNotEmpty()) {
            outLog.appendLine("Fixer error : \n${fixer.second.joinToString("\n")}")
        }
    }

    /**
     * Dump the memory to a file
     *
     * @param ctx pass null if using root, vice versa
     * @param autoFix if `true` the dumped file will be fixed after dumping
     * @param fixerPath ELFixer path
     * @param flagCheck check for flags r-xp in file
     * @return log of the dump
     */
    fun dumpFile(ctx: Context?, autoFix: Boolean, fixerPath: String, flagCheck: Boolean, outLog: OutputHandler) {
        try {
            mem.pid = Process.getProcessID(pkg) ?: throw Exception("Process not found!")

            outLog.appendLine("PID : ${mem.pid}")
            outLog.appendLine("FILE : $file")

            val map = parseMap(flagCheck)
            mem.sAddress = map.first
            mem.eAddress = map.second
            mem.size = mem.eAddress - mem.sAddress

            outLog.appendLine("Start Address : ${mem.sAddress.toHex()}")
            if (mem.sAddress < 1L) {
                outLog.appendError("[ERROR] invalid Start Address!")
                return 
            }

            outLog.appendLine("End Address : ${mem.eAddress.toHex()}")
            if (mem.eAddress < 1L) {
                outLog.appendError("[ERROR] invalid End Address!")
                return 
            }

            outLog.appendLine("Size Memory : ${mem.size}")
            if (mem.size < 1L) {
                outLog.appendError("[ERROR] invalid memory size!")
                return
            }

            if (ctx == null)
                dumpFileRoot(autoFix, fixerPath, outLog)
            else
                dumpFileNonRoot(ctx, autoFix, fixerPath, outLog)

            outLog.appendSuccess("Dump Success")
        } catch (e: Exception) {
            outLog.appendLine("[ERROR] ${e.stackTraceToString()}")
            e.printStackTrace()
        }
        outLog.appendLine("==========================")
    }


    /**
     * Parsing the memory map
     *
     * @throws FileNotFoundException failed to open /proc/{pid}/maps
     * @throws RuntimeException start or end address is not found
     */
    private fun parseMap(checkFlag: Boolean): Pair<Long, Long> {
        val files = File("/proc/${mem.pid}/maps")
        if (!files.exists()) {
            throw FileNotFoundException("Failed To Open : ${files.path}")
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
        } ?: throw RuntimeException("Unable find baseAddress of $file")

        val mapStart = MapLinux(lineStart)

        val lineEnd = lines.findLast {
            val map = MapLinux(it)
            mapStart.getInode() == map.getInode()
        } ?: throw RuntimeException("Unable find endAddress of $file")

        val mapEnd = MapLinux(lineEnd)
        return Pair(mapStart.getStartAddress(), mapEnd.getEndAddress())
    }
}