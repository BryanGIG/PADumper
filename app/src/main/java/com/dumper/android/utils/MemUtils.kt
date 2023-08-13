package com.dumper.android.utils

import com.dumper.android.dumper.Arch
import com.dumper.android.dumper.Memory
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

private val hElf =
    byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())

// Generate documentation

fun isELF(pid: Int, startAddress: Long): Boolean {
    val file = File("/proc/$pid/mem")
    val mFile = RandomAccessFile(file, "r")
    val channel = mFile.channel
    val byteHeader = ByteBuffer.allocate(4)
    channel.read(byteHeader, startAddress)
    channel.close()
    mFile.close()
    return byteHeader[0] == hElf[0] && byteHeader[1] == hElf[1] &&
            byteHeader[2] == hElf[2] && byteHeader[3] == hElf[3]
}

fun getArchELF(mFile: FileChannel, memory: Memory): Arch {
    val byteHeader = ByteBuffer.allocate(5)

    mFile.read(byteHeader, memory.sAddress)

    if (byteHeader[0] != hElf[0] || byteHeader[1] != hElf[1] ||
        byteHeader[2] != hElf[2] || byteHeader[3] != hElf[3]
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