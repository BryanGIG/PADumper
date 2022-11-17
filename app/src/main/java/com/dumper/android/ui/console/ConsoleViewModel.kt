package com.dumper.android.ui.console

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConsoleViewModel : ViewModel() {

    val console = MutableLiveData("")

    fun append(text: String) {
        console.value = console.value + text
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

}