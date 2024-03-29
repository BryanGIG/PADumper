package com.dumper.android.core

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.StorageId
import com.anggrayudi.storage.permission.ActivityPermissionRequest
import com.anggrayudi.storage.permission.PermissionCallback
import com.anggrayudi.storage.permission.PermissionReport
import com.anggrayudi.storage.permission.PermissionRequest
import com.anggrayudi.storage.permission.PermissionResult
import com.dumper.android.core.RootServices.Companion.DUMP_FILE
import com.dumper.android.core.RootServices.Companion.IS_DUMP_METADATA
import com.dumper.android.core.RootServices.Companion.IS_FIX_NAME
import com.dumper.android.core.RootServices.Companion.MSG_DUMP_PROCESS
import com.dumper.android.core.RootServices.Companion.MSG_GET_PROCESS_LIST
import com.dumper.android.core.RootServices.Companion.PROCESS_NAME
import com.dumper.android.dumper.Dumper
import com.dumper.android.dumper.OutputHandler
import com.dumper.android.dumper.process.Process
import com.dumper.android.dumper.sofixer.FixerUtils
import com.dumper.android.messager.MSGConnection
import com.dumper.android.messager.MSGReceiver
import com.dumper.android.ui.MainScreen
import com.dumper.android.ui.console.ConsoleViewModel
import com.dumper.android.ui.memory.MemoryViewModel
import com.dumper.android.ui.theme.PADumperTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    var remoteMessenger: Messenger? = null
    private val receiver = Messenger(Handler(Looper.getMainLooper(), MSGReceiver(this)))
    private lateinit var dumperConnection: MSGConnection

    val memory: MemoryViewModel by viewModels()
    val console: ConsoleViewModel by viewModels()

    // FIXME: Use proper way to check storage permission
    private var isStoragePermissionGranted = true

    private val permissionRequest = ActivityPermissionRequest.Builder(this)
        .withPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        .withCallback(object : PermissionCallback {
            override fun onPermissionsChecked(result: PermissionResult, fromSystemDialog: Boolean) {
                val grantStatus = if (result.areAllPermissionsGranted) "granted" else "denied"
                Toast.makeText(baseContext, "Storage permissions are $grantStatus", Toast.LENGTH_SHORT).show()
            }

            override fun onShouldRedirectToSystemSettings(blockedPermissions: List<PermissionReport>) {
                super.onShouldRedirectToSystemSettings(blockedPermissions)
                SimpleStorageHelper.redirectToSystemSettings(this@MainActivity)
                isStoragePermissionGranted = false
            }

            override fun onDisplayConsentDialog(request: PermissionRequest) {
                super.onDisplayConsentDialog(request)
                isStoragePermissionGranted = false
            }

            override fun onPermissionRequestInterrupted(permissions: Array<String>) {
                super.onPermissionRequestInterrupted(permissions)
                isStoragePermissionGranted = false
            }
        })
        .build()

    private val storageHelper = SimpleStorageHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkShell()

        setContent {
            PADumperTheme {
                MainScreen(memory, console)
            }
        }

        FixerUtils.extractLibs(this)

        if (Shell.getCachedShell()?.isRoot == true) {
            setupRootService()
        } else {
            setupNonRoot(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Shell.getCachedShell()?.isRoot == true && !isStoragePermissionGranted) {
            setupNonRoot(null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        storageHelper.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        storageHelper.onRestoreInstanceState(savedInstanceState)
    }

    private fun checkShell() {
        if (Shell.getCachedShell() == null) {
            Intent(this, SplashActivity::class.java).also {
                startActivity(it)
                finish()
            }
        }
    }

    fun sendRequestAllProcess() {
        if (remoteMessenger != null && Shell.getCachedShell()?.isRoot == true) {
            val message = Message.obtain(null, MSG_GET_PROCESS_LIST)
            message.replyTo = receiver
            remoteMessenger?.send(message)
        } else {
            val processList = Process.getAllProcess(this, false)
            memory.showProcess(this, processList)
        }
    }

    fun sendRequestDump(
        process: String,
        dumpFile: String,
        isDumpGlobalMetadata: Boolean,
        autoFix: Boolean
    ) {
        if (process.isBlank()) {
            Toast.makeText(this, "Process name is empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (dumpFile.isBlank()) {
            Toast.makeText(this, "Lib name is empty", Toast.LENGTH_SHORT).show()
            return
        }

        console.appendLine("==========================\nProcess : $process")

        if (remoteMessenger != null && Shell.getCachedShell()?.isRoot == true) {
            val message = Message.obtain(null, MSG_DUMP_PROCESS)

            message.data.apply {
                putString(PROCESS_NAME, process)
                putString(DUMP_FILE, dumpFile)
                if (isDumpGlobalMetadata) {
                    putBoolean(IS_DUMP_METADATA, true)
                }
                if (autoFix) {
                    putBoolean(IS_FIX_NAME, true)
                }
            }

            message.replyTo = receiver
            remoteMessenger?.send(message)
        } else {
            val outHandler = OutputHandler(console)

            Dumper(process, dumpFile, isDumpGlobalMetadata)
                .dumpFile(this, autoFix, outHandler)
        }
    }

    private fun setupNonRoot(savedInstanceState: Bundle?) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Warning")
            .setMessage("This app required storage permission to work properly")
            .setPositiveButton("OK") { _, _ ->
                savedInstanceState?.let { storageHelper.onRestoreInstanceState(it) }
                setupStoragePermission()
            }
            .setNegativeButton("Cancel") { _, _ ->
                exitProcess(0)
            }
            .setCancelable(false)
            .show()
    }

    private fun setupStoragePermission() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M ..Build.VERSION_CODES.P) {
            permissionRequest.check()
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            storageHelper.requestStorageAccess()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!storageHelper.storage.isStorageAccessGranted(StorageId.PRIMARY)) {
                storageHelper.storage.requestFullStorageAccess()
            }
        }
    }

    private fun setupRootService() {
        if (remoteMessenger == null) {
            dumperConnection = MSGConnection(this)
            val intent = Intent(applicationContext, RootServices::class.java)
            RootService.bind(intent, dumperConnection)
        }
    }
}