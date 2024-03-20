package com.dumper.android.dumper.maps

data class MemoryRegion(val line: String) {
    var startAddress: Long = 0L
    var endAddress: Long = 0L
    var perms: String = ""
    var offset: Long = 0L
    var dev: String = ""
    var inode: Long = 0L
    var path: String = ""
}