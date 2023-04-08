package com.dumper.android.utils

import com.anggrayudi.storage.extension.closeStreamQuietly
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

fun FileChannel.copyToFile(startPos: Long, byteSize: Long, outputFile: File) {
    val outputStream = outputFile.outputStream()

    var bytesWritten = 0
    val buffer = ByteBuffer.allocate(1024)
    while (bytesWritten < byteSize) {
        val bytesRead = read(buffer, startPos + bytesWritten)
        outputStream.write(buffer.array(), 0, bytesRead)
        buffer.clear()
        bytesWritten += bytesRead
    }

    outputStream.flush()
    outputStream.closeStreamQuietly()
    close()
}