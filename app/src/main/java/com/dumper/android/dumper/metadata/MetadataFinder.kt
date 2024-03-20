package com.dumper.android.dumper.metadata

import android.util.Log
import com.dumper.android.dumper.OutputHandler
import com.dumper.android.dumper.maps.MapLineParser
import com.dumper.android.utils.TAG
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class MetadataFinder(private val pid: Int) {

    fun findUnityMetadata(outLog: OutputHandler): MapLineParser? {
        return findByFile(outLog) ?: findByHeader(outLog) ?: findByFunction(outLog)
    }

    private fun findByFile(outLog: OutputHandler): MapLineParser? {
        outLog.appendInfo("Scanning /proc/$pid/maps...")

        val mapsFile = File("/proc/${pid}/maps")
        if (!mapsFile.exists()) {
            throw FileNotFoundException("Failed To Open : ${mapsFile.path}")
        }

        return mapsFile.readLines().find { it.contains("global-metadata.dat") }
            ?.let { MapLineParser(it) }
    }

    private fun findByHeader(outLog: OutputHandler, customHeader: String = GLOBAL_METADATA_PATTERN): MapLineParser? {
        outLog.appendInfo("Find by using header: $customHeader")

        val customHeaderSize = customHeader.split(" ").size

        val byteHeader = ByteBuffer.allocate(customHeaderSize)

        val mapsFile = File("/proc/${pid}/maps")
        if (!mapsFile.exists()) {
            throw FileNotFoundException("Failed To Open : ${mapsFile.path}")
        }

        val memFile = RandomAccessFile("/proc/${pid}/mem", "r").channel

        mapsFile.readLines()
            .filter {  it.contains(".dat") && !filteredPath.any { ext -> it.contains(ext) } }
            .map { MapLineParser(it) }
            .forEach {
                Log.i(TAG, it.toString())

                // Clear buffer and new position
                byteHeader.clear()
                byteHeader.position(0)

                val bytesRead = memFile.read(byteHeader, it.getStartAddress())
                if (bytesRead == customHeaderSize && containsPatternWithWildcard(byteHeader.array(), customHeader)) {
                    return it
                }
            }

        return null
    }

    private val commonFunctionMetadata = "zeroVector"

    private fun findByFunction(outLog: OutputHandler, funcName: String = commonFunctionMetadata): MapLineParser? {
        outLog.appendInfo("Find by using function: $funcName")

        val funcNameBytes = funcName.toByteArray(Charsets.UTF_8)
        val byteAllocate = ByteBuffer.allocate(2000) // 2 MB

        val mapsFile = File("/proc/${pid}/maps")
        if (!mapsFile.exists()) {
            throw FileNotFoundException("Failed To Open : ${mapsFile.path}")
        }

        val memFile = RandomAccessFile("/proc/${pid}/mem", "r").channel

        mapsFile.readLines()
            .filter {
                // mostly on path that has been deleted
                it.contains("deleted")
            }
            .map { MapLineParser(it) }
            .forEach {
                var pos = it.getStartAddress()

                while (pos <= it.getEndAddress()) {

                    // Clear buffer and new position
                    byteAllocate.clear()
                    byteAllocate.position(0)

                    if (memFile.read(byteAllocate, pos) <= 0)
                        return null

                    if (containsAnyByteArray(byteAllocate, funcNameBytes)) {
                        memFile.close()
                        return it
                    }
                    pos += 1024
                }
            }

        memFile.close()
        return null
    }

    companion object {
        val filteredPath = listOf("app_process", ".so", ".apk", ".odex", ".ttf", ".oat", ".jar", ".art", ".vdex")

        val GLOBAL_METADATA_BYTES = byteArrayOf(0xAF.toByte(),  0x1B.toByte(), 0xB1.toByte(), 0xFA.toByte())
        const val GLOBAL_METADATA_PATTERN = "AF 1B B1 FA ?? 00"
    }
}