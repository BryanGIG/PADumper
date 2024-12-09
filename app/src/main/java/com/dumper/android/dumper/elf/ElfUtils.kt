package com.dumper.android.dumper.elf

import com.dumper.android.dumper.maps.MapLineParser
import com.dumper.android.dumper.sofixer.Arch
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

// ELF header magic number
private val hElf =
    byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())

/**
 * Checks if the memory at the given start address of a process is an ELF file.
 *
 * @param pid The process ID.
 * @param startAddress The start address in the process memory.
 * @return True if the memory at the start address is an ELF file, false otherwise.
 */
fun isELF(pid: Int, startAddress: Long): Boolean {
    val mFile = RandomAccessFile("/proc/$pid/mem", "r")
    val channel = mFile.channel
    val byteHeader = ByteBuffer.allocate(4)
    channel.read(byteHeader, startAddress)
    channel.close()
    mFile.close()
    return byteHeader.array().contentEquals(hElf)
}

/**
 * Determines the architecture of the ELF file in the given memory.
 *
 * @param mFile The file channel of the memory.
 * @param memory The memory map line parser.
 * @return The architecture of the ELF file.
 */
fun getArchELF(mFile: FileChannel, memory: MapLineParser): Arch {
    val byteHeader = ByteBuffer.allocate(5)

    mFile.read(byteHeader, memory.getStartAddress())

    for (i in 0 until 4) {
        if (byteHeader[i] != hElf[i]) {
            return Arch.UNKNOWN
        }
    }
    if (byteHeader[0] != hElf[0] || byteHeader[1] != hElf[1] ||
        byteHeader[2] != hElf[2] || byteHeader[3] != hElf[3]
    ) {
        return Arch.UNKNOWN
    }

    mFile.position(0) //reset pos

    return when (byteHeader[4].toInt()) {
        1 -> Arch.ARCH_32BIT
        2 -> Arch.ARCH_64BIT
        else -> Arch.UNKNOWN
    }
}