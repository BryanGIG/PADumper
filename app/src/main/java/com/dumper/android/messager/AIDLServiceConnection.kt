package com.dumper.android.messager

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.dumper.android.IDumperInterface
import com.dumper.android.core.MainActivity
import com.dumper.android.dumper.DumperConfig
import com.dumper.android.dumper.process.ProcessData

class AIDLServiceConnection(private val activity: MainActivity) : ServiceConnection {

    private var iDumperInterface: IDumperInterface? = null

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        activity.console.appendInfo("RootService: Service connected")
        iDumperInterface = IDumperInterface.Stub.asInterface(service)
        activity.dumperConnection = this
    }

    fun getListProcess(): List<ProcessData>? = iDumperInterface?.getListProcess()

    fun dump(config: DumperConfig) {
        Thread {
            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            iDumperInterface?.dump(config, writeSide)

            val inputStream = ParcelFileDescriptor.AutoCloseInputStream(readSide)
            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val data = buffer.copyOfRange(0, bytesRead)
                activity.runOnUiThread {
                    activity.console.append(String(data))
                }
            }
        }.start()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        activity.console.appendInfo("RootService: Service disconnected")
        activity.dumperConnection = null
    }
}