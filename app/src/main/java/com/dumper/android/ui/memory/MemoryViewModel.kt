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
    val processList = MutableLiveData<Array<ProcessData>>()

    fun showProcess(ctx: MainActivity, list: List<ProcessData>) {
        if (list.isEmpty()) {
            Toast.makeText(ctx, "No process found", Toast.LENGTH_SHORT).show()
            return
        }

        processList.value = list.sortedBy { it.processName }.toTypedArray()
    }

    fun beginDump(context: Context) {
        val dumpFile = mutableListOf(libName.value)
        if (isDumpMetadata.value) {
            dumpFile.add("global-metadata.dat")
        }

        context.asMainActivity()?.sendRequestDump(
            process = packageName.value,
            dumpFile = dumpFile.toTypedArray(),
            autoFix = isFixELF.value
        )
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

    override fun onCleared() {
        super.onCleared()

    }
}