package com.dumper.android.dumper

import android.content.Context
import android.os.ParcelFileDescriptor
import com.dumper.android.IDumperInterface
import com.dumper.android.dumper.process.Process
import com.dumper.android.dumper.process.ProcessData

class DumperMain(private val ctx: Context): IDumperInterface.Stub() {
    override fun dump(config: DumperConfig, fileDescriptor: ParcelFileDescriptor) {
        Dumper(ctx, config, OutputHandler(fileDescriptor)).dumpFile()
    }

    override fun getListProcess(): List<ProcessData> {
        return Process.getAllProcess(ctx, true)
    }
}