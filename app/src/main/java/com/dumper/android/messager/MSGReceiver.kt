package com.dumper.android.messager

import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.dumper.android.core.MainActivity
import com.dumper.android.core.RootServices
import com.dumper.android.dumper.process.ProcessData
import com.dumper.android.ui.memory.MemoryFragment
import com.dumper.android.utils.getParcelableArrayListCompact

class MSGReceiver(private val activity: MainActivity) : Handler.Callback {

    override fun handleMessage(message: Message): Boolean {
        message.data.classLoader = activity.classLoader

        when (message.what) {
            RootServices.MSG_GET_PROCESS_LIST -> {
                message.data.getParcelableArrayListCompact<ProcessData>(RootServices.LIST_ALL_PROCESS)
                    ?.let {
                        val navHost = activity.binding.navHostFragmentActivityMain
                        val navController = navHost.getFragment<NavHostFragment>()
                        val fragments = navController.childFragmentManager.fragments

                        fragments.find { it is MemoryFragment }
                            ?.let { fragment ->
                                (fragment as MemoryFragment).showProcess(it)
                            }
                    }
            }
            RootServices.MSG_DUMP_PROCESS -> {
                message.data.getString(RootServices.DUMP_LOG)?.let {
                    activity.console.append(it)
                    activity.console.appendLine("==========================")
                    Toast.makeText(activity, "Dump Complete!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return false
    }
}