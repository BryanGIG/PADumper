package com.dumper.android.dumper

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object MemUtils {

    private val ELFMAG =
        byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())

    fun isELF(pid: Int, startAddress: Long): Boolean {
        val file = File("/proc/$pid/mem")
        val mFile = RandomAccessFile(file, "r")
        val channel = mFile.channel
        val byteHeader = ByteBuffer.allocate(4)
        channel.read(byteHeader, startAddress)
        channel.close()
        mFile.close()
        return byteHeader[0] == ELFMAG[0] && byteHeader[1] == ELFMAG[1] &&
                byteHeader[2] == ELFMAG[2] && byteHeader[3] == ELFMAG[3]
    }

    fun getArchELF(mFile: FileChannel, memory: Memory) : Arch {
        val byteHeader = ByteBuffer.allocate(5)

        mFile.read(byteHeader, memory.sAddress)

        if (byteHeader[0] != ELFMAG[0] || byteHeader[1] != ELFMAG[1] ||
            byteHeader[2] != ELFMAG[2] || byteHeader[3] != ELFMAG[3]
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


}