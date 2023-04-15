package com.dumper.android.ui.memory

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dumper.android.dumper.process.ProcessData
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MemoryViewModel : ViewModel() {

    val selectedApps = MutableLiveData<String>()
    val packageName = MutableLiveData<String>()
    val libName = MutableLiveData<String>()


    fun showProcess(ctx: Context, list: ArrayList<ProcessData>) {
        list.sortBy { lists -> lists.appName }

        val appNames = list.map { processData ->
            val processName = processData.processName
            if (processName.contains(":"))
                "${processData.appName} (${processName.substringAfter(":")})"
            else
                processData.appName
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Select process")
            .setSingleChoiceItems(appNames.toTypedArray(), -1) { dialog, which ->
                selectedApps.value = list[which].processName
                dialog.dismiss()
            }
            .show()
    }

}