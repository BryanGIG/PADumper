package com.dumper.android.dumper.exception

class DumperException(private val msg: String) : Exception(msg) {
    override fun toString(): String {
        return "DumperException: $msg"
    }
}