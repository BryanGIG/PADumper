package com.dumper.android.dumper.maps

import com.dumper.android.dumper.exception.DumperException
import com.dumper.android.utils.toHex

class MapLineParser(line: String) {
    private val memoryRegion = MemoryRegion(line)

    init {
        val strp = line.replace("\\s+".toRegex(), " ").split(" ")
        strp.forEachIndexed { index, s ->
            when (index) {
                0 -> {
                    val lineAddrs = s.split("-")
                    if (lineAddrs.size < 2)
                        throw DumperException("Invalid maps line parsing...")

                    memoryRegion.startAddress = lineAddrs[0].toLong(16)
                    memoryRegion.endAddress = lineAddrs[1].toLong(16)
                }
                1 -> memoryRegion.perms = s
                2 -> memoryRegion.offset = s.toLong(16)
                3 -> memoryRegion.dev = s
                4 -> memoryRegion.inode = s.toLong()
                5 -> memoryRegion.path = s
            }
        }
    }

    fun getStartAddress(): Long {
        return memoryRegion.startAddress
    }

    fun getEndAddress(): Long {
        return memoryRegion.endAddress
    }

    fun setEndAddress(endAddress: Long) {
        memoryRegion.endAddress = endAddress
    }

    fun getPerms(): String {
        return memoryRegion.perms
    }

    fun getOffset(): Long {
        return memoryRegion.offset
    }

    fun getDev(): String {
        return memoryRegion.dev
    }

    fun getInode(): Int {
        return memoryRegion.inode.toInt()
    }

    fun getPath(): String {
        return memoryRegion.path
    }

    fun getSize(): Long {
        return getEndAddress() - getStartAddress()
    }

    fun isValid(): Boolean {
        return getStartAddress() > 0L || getEndAddress() > 0L
    }

    override fun toString(): String {
        return "startAddress=${getStartAddress().toHex()} || endAddress=${getEndAddress().toHex()} || size=${getSize()}|| path=${getPath()}"
    }
}
