package com.dumper.android.dumper

import android.os.ParcelFileDescriptor
import android.util.Log
import com.dumper.android.ui.console.ConsoleViewModel
import com.dumper.android.utils.TAG
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Class responsible for handling output messages.
 */
class OutputHandler {
    private var isRoot = false
    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var outputStream: OutputStream
    private lateinit var console: ConsoleViewModel

    private constructor()

    /**
     * Constructor for root services.
     *
     * @param parcelFileDescriptor The ParcelFileDescriptor for root services.
     */
    constructor(parcelFileDescriptor: ParcelFileDescriptor) : this() {
        isRoot = true
        this.parcelFileDescriptor = parcelFileDescriptor
        outputStream = FileOutputStream(parcelFileDescriptor.fileDescriptor)
    }

    /**
     * Constructor for non-root services.
     *
     * @param console The ConsoleViewModel to append messages to.
     */
    constructor(console: ConsoleViewModel) : this() {
        isRoot = false
        this.console = console
    }

    /**
     * Process the input string and send it to the appropriate output.
     *
     * @param str The string to be processed.
     */
    private fun processInput(str: String) {
        if (isRoot) {
            try {
                outputStream.write(str.toByteArray())
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to output stream", e)
            }
        } else {
            console.append(str)
        }
    }

    /**
     * Finish the output handling and close resources.
     *
     * @param code The exit code.
     */
    fun finish(code: Int) {
        if (isRoot) {
            try {
                outputStream.close()
                parcelFileDescriptor.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing output stream", e)
            }
        } else {
            console.finish(code)
        }
    }

    /**
     * Append text to the output.
     *
     * @param text The text to append.
     */
    fun append(text: String) {
        processInput(text)
    }

    /**
     * Append a line of text to the output.
     *
     * @param text The text to append.
     */
    fun appendLine(text: String) {
        processInput(text + "\n")
    }

    /**
     * Append an error message to the output.
     *
     * @param text The error message to append.
     */
    fun appendError(text: String) {
        appendLine("[ERROR] $text")
    }

    /**
     * Append a warning message to the output.
     *
     * @param text The warning message to append.
     */
    fun appendWarning(text: String) {
        appendLine("[WARNING] $text")
    }

    /**
     * Append an info message to the output.
     *
     * @param text The info message to append.
     */
    fun appendInfo(text: String) {
        appendLine("[INFO] $text")
    }

    /**
     * Append a success message to the output.
     *
     * @param text The success message to append.
     */
    fun appendSuccess(text: String) {
        appendLine("[SUCCESS] $text")
    }
}