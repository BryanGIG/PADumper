package com.dumper.android.ui.memory

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dumper.android.core.MainActivity
import com.dumper.android.dumper.process.ProcessData
import com.dumper.android.utils.asMainActivity
import kotlinx.coroutines.flow.MutableStateFlow

class MemoryViewModel : ViewModel() {
    val packageName = MutableStateFlow("")
    val libName = MutableStateFlow("libil2cpp.so")
    val isFixELF = MutableStateFlow(false)
    val isDumpMetadata = MutableStateFlow(false)
    val isDialogProcessList = MutableStateFlow(false)
    val processList = MutableLiveData<Array<ProcessData>>()

    fun showProcess(ctx: MainActivity, list: List<ProcessData>) {
        if (list.isEmpty()) {
            Toast.makeText(ctx, "No process found", Toast.LENGTH_SHORT).show()
            return
        }

        processList.value = list.sortedBy { it.getDisplayName() }.toTypedArray()
        isDialogProcessList.value = true
    }

    fun beginDump(context: Context) {
        context.asMainActivity()?.sendRequestDump(
            process = packageName.value,
            dumpFile = libName.value,
            isDumpGlobalMetadata = isDumpMetadata.value,
            autoFix = isFixELF.value
        )
    }

    fun closeProcessListDialog() {
        isDialogProcessList.value = false
    }

    fun getProcessList(context: Context) {
        context.asMainActivity()?.sendRequestAllProcess()
    }

    fun changePackageName(pkg: String) {
        packageName.value = pkg
    }

    fun changeLibName(lib: String) {
        libName.value = lib
    }

    fun changeFixELF(isFix: Boolean) {
        isFixELF.value = isFix
    }

    fun changeDumpMetadata(isDumpMetadata: Boolean) {
        this.isDumpMetadata.value = isDumpMetadata
    }
}