package com.dumper.android.ui.memory

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dumper.android.dumper.process.ProcessData
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MemoryViewModel : ViewModel() {

    val selectedApps = MutableLiveData<String>()
    val packageName = MutableLiveData<String>()
    val libName = MutableLiveData<String>()
    val isFixELF = MutableLiveData<Boolean>()
    val isDumpMetadata = MutableLiveData<Boolean>()

    fun showProcess(ctx: Context, list: List<ProcessData>) {
        if (list.isEmpty()) {
            Toast.makeText(ctx, "No process found", Toast.LENGTH_SHORT).show()
            return
        }

        val appNames = list.sortedBy { it.processName }
        val appsTitle = appNames
            .map { processData ->
                val processName = processData.processName
                if (processName.contains(":"))
                    "${processData.appName} (${processName.substringAfter(":")})"
                else
                    processData.appName
            }.toTypedArray()

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Select process")
            .setSingleChoiceItems(appsTitle, -1
            ) { dialog, idx ->
                selectedApps.value = appNames[idx].processName
                dialog.dismiss()
            }
            .show()
    }
}