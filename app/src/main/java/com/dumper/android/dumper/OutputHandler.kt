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
    private lateinit var console: ConsoleViewModel

    private constructor()

    /**
     * This method is used to send message to client
     * Use this method if you're on root services
     * @param from: Message from client
     * @param reply: Message to client
    */
    constructor(from: Message, reply: Message) : this() {
        isRoot = true
        this.from = from
        this.reply = reply
    }

    /**
     * This method is used to append message to console
     * Use this method if you're on non-root
     * @param console: ConsoleViewModel to append
    */
    constructor(console: ConsoleViewModel) : this() {
        isRoot = false
        this.console = console
    }

    private fun processInput(str: String) {
        if (isRoot) {
            val data = Bundle()
            data.putString(RootServices.DUMP_LOG, str)
            reply.what = RootServices.MSG_DUMP_PROCESS
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

    fun finish(code: Int) {
        if (isRoot) {
            val data = Bundle()
            data.putInt(RootServices.DUMP_CODE, code)
            reply.what = RootServices.MSG_DUMP_FINISH
            reply.data = data
            try {
                from.replyTo.send(reply)
            } catch (e: RemoteException) {
                Log.e(TAG, "Remote error", e)
            }
        } else {
            console.finish(code)
        }
    }

    fun append(text: String) {
        processInput(text)
    }

    fun appendLine(text: String) {
        processInput(text + "\n")
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