package com.dumper.android.core

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.dumper.android.dumper.Dumper
import com.dumper.android.dumper.OutputHandler
import com.dumper.android.dumper.process.Process
import com.dumper.android.utils.TAG
import com.topjohnwu.superuser.ipc.RootService


class RootServices : RootService(), Handler.Callback {
    override fun onBind(intent: Intent): IBinder {
        val h = Handler(Looper.getMainLooper(), this)
        val m = Messenger(h)
        return m.binder
    }

    override fun handleMessage(msg: Message): Boolean {
        val reply = Message.obtain()
        val data = Bundle()

        when (msg.what) {
            MSG_GET_PROCESS_LIST -> {
                runCatching {
                    Process.getAllProcess(this, true)
                }.onSuccess { process ->
                    reply.what = MSG_GET_PROCESS_LIST
                    data.putParcelableArray(LIST_ALL_PROCESS, process.toTypedArray())
                }

                reply.data = data
                try {
                    msg.replyTo.send(reply)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Remote error", e)
                }
            }

            MSG_DUMP_PROCESS -> {
                val outCode = OutputHandler(msg, reply, MSG_DUMP_FINISH)
                val outLog = OutputHandler(msg, reply, MSG_DUMP_PROCESS)
                val process = msg.data.getString(PROCESS_NAME)
                val listFile = msg.data.getStringArray(LIST_FILE)
                val fixerPath = msg.data.getString(LIBRARY_DIR_NAME, "")
                val isAutoFix = msg.data.getBoolean(IS_FIX_NAME, false)
                if (process != null && listFile != null) {
                    val dumper = Dumper(process)
                    for (file in listFile) {
                        dumper.file = file
                        outCode.finish(dumper.dumpFile(null, isAutoFix, fixerPath, outLog))
                    }
                } else {
                    outLog.appendError("Data Error!")
                    outCode.finish(-1)
                }
            }
            else -> {
                data.putString(DUMP_LOG, "[ERROR] Unknown command")
                reply.data = data
                try {
                    msg.replyTo.send(reply)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Remote error", e)
                }
            }
        }

        return false
    }

    override fun onUnbind(intent: Intent): Boolean {
        return false
    }

    companion object {
        const val MSG_DUMP_PROCESS = 1
        const val MSG_GET_PROCESS_LIST = 2
        const val MSG_DUMP_FINISH = 3
        const val DUMP_CODE = "DUMP_CODE"
        const val DUMP_LOG = "DUMP_LOG"
        const val LIBRARY_DIR_NAME = "NATIVE_DIR"
        const val LIST_ALL_PROCESS = "LIST_ALL_PROCESS"
        const val PROCESS_NAME = "PROCESS"
        const val LIST_FILE = "LIST_FILE"
        const val IS_FIX_NAME = "FIX_ELF"
    }
}