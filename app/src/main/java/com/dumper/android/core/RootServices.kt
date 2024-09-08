package com.dumper.android.core

import android.content.Intent
import android.os.IBinder
import com.dumper.android.dumper.DumperMain
import com.topjohnwu.superuser.ipc.RootService


class RootServices : RootService() {
    override fun onBind(intent: Intent): IBinder {
        return DumperMain(this)
    }

    override fun onUnbind(intent: Intent): Boolean {
        return false
    }
}