package com.dumper.android.messager

import android.os.Handler
import android.os.Message
import com.dumper.android.core.MainActivity
import com.dumper.android.core.RootServices
import com.dumper.android.dumper.process.ProcessData
import com.dumper.android.utils.getParcelableArrayListCompact

class MSGReceiver(private val activity: MainActivity) : Handler.Callback {

    override fun handleMessage(message: Message): Boolean {
        message.data.classLoader = activity.classLoader

        when (message.what) {
            RootServices.MSG_GET_PROCESS_LIST -> {
                message.data.getParcelableArrayListCompact<ProcessData>(RootServices.LIST_ALL_PROCESS)
                    ?.let {
                        activity.memory.showProcess(activity, it)
                    }
            }


            RootServices.MSG_DUMP_PROCESS -> {
                message.data.getString(RootServices.DUMP_LOG)?.let {
                        activity.console.append(it)
                    }
            }

            RootServices.MSG_DUMP_FINISH -> {
                message.data.getInt(RootServices.DUMP_CODE, -1).let {
                    activity.console.finish(it)
                }
            }

        }
        return false
    }
}