package com.dumper.android.ui.console

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dumper.android.ui.Event

class ConsoleViewModel : ViewModel() {

    val console = MutableLiveData("")
    val finishCode = MutableLiveData<Event<Int>>()
    fun copyConsole(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("PADumper-Log", console.value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Log Copied!", Toast.LENGTH_SHORT).show()
    }

    fun append(text: String) {
        console.value += text
    }

    fun clear() {
        console.value = ""
    }

    fun appendLine(text: String) {
        append(text + "\n")
    }

    fun appendLine() {
        append("\n")
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

    fun finish(ret: Int) {
        finishCode.value = Event(ret)
    }
}