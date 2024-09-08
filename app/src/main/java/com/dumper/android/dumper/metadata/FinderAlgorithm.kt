package com.dumper.android.dumper.metadata

import java.nio.ByteBuffer

fun containsPatternWithWildcard(buffer: ByteArray, pattern: String): Boolean {
    val patternBytes = pattern.split(" ")
        .map {
            when (it) {
                "??" -> null  // Represent wildcard as null
                else -> it.toInt(16).toByte()
            }
        }

    for (i in buffer.indices) {
        if (patternBytes.size > (buffer.size - i))
            return false

        for (j in patternBytes.indices) {
            if (patternBytes[j] == null)
                continue  // Skip wildcard comparison

            if (buffer[i + j] != patternBytes[j])
                continue
        }

        return true
    }

    return false
}

// Boyer-Moore search algorithm
fun containsAnyByteArray(byteAllocate: ByteBuffer, byteArray: ByteArray): Boolean {
    val buffer = byteAllocate.array()
    val badCharSkip = IntArray(256) { byteArray.size }
    for (i in byteArray.indices) {
        badCharSkip[byteArray[i].toInt() and 0xFF] = byteArray.size - i - 1
    }
    var pos = 0
    while (buffer.size - pos >= byteArray.size) {
        var i = byteArray.size - 1
        while (i >= 0 && byteArray[i] == buffer[pos + i]) {
            i -= 1
        }
        if (i < 0) {
            return true  // Found
        } else {
            pos += maxOf(
                1,
                badCharSkip[buffer[pos + i].toInt() and 0xFF] - (byteArray.size - 1 - i)
            )
        }
    }
    return false
}

