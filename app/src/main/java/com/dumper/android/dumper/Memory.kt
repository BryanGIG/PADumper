package com.dumper.android.dumper

class MapLinux(str: String) {
    
    private var address = ""
    private var perms = ""
    private var offset = ""
    private var dev = ""
    private var inode = ""
    private var path = ""

    init {
        val strp = str.replace("\\s+".toRegex(), " ").split(" ")
        strp.forEachIndexed { index, s ->
            when (index) {
                0 -> address = s
                1 -> perms = s
                2 -> offset = s
                3 -> dev = s
                4 -> inode = s
                5 -> path = s
            }
        }
    }

    fun getStartAddress(): Long {
        if (address.isEmpty()) return 0L
        return address.split("-")[0].toLong(16)
    }

    fun getEndAddress(): Long {
        if (address.isEmpty()) return 0L
        return address.split("-")[1].toLong(16)
    }

    fun getPerms(): String {
        return perms
    }

    fun getOffset(): Long {
        return offset.toLong(16)
    }

    fun getDev(): String {
        return dev
    }

    fun getInode(): Int {
        return inode.toInt()
    }

    fun getPath(): String {
        return path
    }

    fun getSize(): Long {
        return getEndAddress() - getStartAddress()
    }

    fun isValid(): Boolean {
        return getStartAddress() != 0L && getEndAddress() != 0L
    }

    override fun toString(): String {
        return "MapLinux(address='$address', perms='$perms', offset='$offset', dev='$dev', inode='$inode', path='$path')"
    }
}

data class Memory(val pkg: String) {
    var pid: Int = 0
    var sAddress: Long = 0L
    var eAddress: Long = 0L
    var size: Long = 0L
    var perms: String = ""
    var offset: Long = 0L
    var device: String = ""
    var inode: Long = 0L
    var path: String = ""
}