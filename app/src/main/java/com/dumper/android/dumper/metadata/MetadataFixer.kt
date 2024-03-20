package com.dumper.android.dumper.metadata

import java.io.File

class MetadataFixer {
    fun isNeedFixer(file: File): Boolean {
        if (!file.exists())
            return false

        if (!file.canRead())
            return false

        val result = ByteArray(5)
        file.inputStream().let {
            it.read(result)
            it.close()
        }

        return !result.contentEquals(MetadataFinder.GLOBAL_METADATA_BYTES)
    }

    fun fixHeader(file: File, checkFile: Boolean = true): Boolean {
        if (checkFile && !isNeedFixer(file))
            return false

        if (!file.canWrite())
            return false

        file.outputStream().let {
            it.write(MetadataFinder.GLOBAL_METADATA_BYTES)
            it.close()
        }
        return true
    }
}