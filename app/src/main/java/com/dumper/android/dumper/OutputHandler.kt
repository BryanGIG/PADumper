package com.dumper.android.dumper

import android.os.Bundle
import android.os.Message
import android.os.RemoteException
import android.util.Log
import com.dumper.android.core.RootServices
import com.dumper.android.ui.console.ConsoleViewModel
import com.dumper.android.utils.TAG

class OutputHandler {
    private var isRoot = false
    private lateinit var from: Message
    private lateinit var reply: Message
    private var what = 0
    private lateinit var console: ConsoleViewModel

    private constructor()

    constructor(from: Message, reply: Message, what: Int) : this() {
        isRoot = true
        this.from = from
        this.reply = reply
        this.what = what
    }

    constructor(console: ConsoleViewModel) : this() {
        isRoot = false
        this.console = console
    }

    private fun processInput(str: String) {
        if (isRoot) {
            val data = Bundle()
            data.putString(RootServices.DUMP_LOG, str)
            reply.what = what
            reply.data = data
            try {
                from.replyTo.send(reply)
            } catch (e: RemoteException) {
                Log.e(TAG, "Remote error", e)
            }
        } else {
            console.append(str)
        }
    }


    fun appendLine(text: String) {
        processInput(text + "\n")
    }

    fun appendLine() {
        processInput("\n")
    }

    fun appendError(text: String) {
        appendLine("[ERROR] $text")
    }

    fun appendWarning(text: String) {
        appendLine("[WARNING] $text")
    }

    fun appendInfo(text: String) {
        appendLine("[INFO] $text")
    }

    fun appendSuccess(text: String) {
        appendLine("[SUCCESS] $text")
    }
}