package com.dumper.android.core

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.StorageId
import com.anggrayudi.storage.permission.ActivityPermissionRequest
import com.anggrayudi.storage.permission.PermissionCallback
import com.anggrayudi.storage.permission.PermissionReport
import com.anggrayudi.storage.permission.PermissionResult
import com.dumper.android.R
import com.dumper.android.core.RootServices.Companion.IS_FIX_NAME
import com.dumper.android.core.RootServices.Companion.IS_FLAG_CHECK
import com.dumper.android.core.RootServices.Companion.LIBRARY_ARCH_BOOL
import com.dumper.android.core.RootServices.Companion.LIBRARY_DIR_NAME
import com.dumper.android.core.RootServices.Companion.LIST_FILE
import com.dumper.android.core.RootServices.Companion.MSG_DUMP_PROCESS
import com.dumper.android.core.RootServices.Companion.MSG_GET_PROCESS_LIST
import com.dumper.android.core.RootServices.Companion.PROCESS_NAME
import com.dumper.android.databinding.ActivityMainBinding
import com.dumper.android.dumper.Dumper
import com.dumper.android.dumper.Fixer
import com.dumper.android.dumper.process.Process
import com.dumper.android.messager.MSGConnection
import com.dumper.android.messager.MSGReceiver
import com.dumper.android.ui.console.ConsoleViewModel
import com.dumper.android.ui.memory.MemoryFragment
import com.dumper.android.ui.memory.MemoryViewModel
import com.topjohnwu.superuser.ipc.RootService


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    var remoteMessenger: Messenger? = null
    private val receiver = Messenger(Handler(Looper.getMainLooper(), MSGReceiver(this)))
    private lateinit var dumperConnection: MSGConnection

    val memory: MemoryViewModel by viewModels()
    val console: ConsoleViewModel by viewModels()

    private val permissionRequest = ActivityPermissionRequest.Builder(this)
        .withPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        .withCallback(object : PermissionCallback {
            override fun onPermissionsChecked(result: PermissionResult, fromSystemDialog: Boolean) {
                val grantStatus = if (result.areAllPermissionsGranted) "granted" else "denied"
                Toast.makeText(
                    baseContext,
                    "Storage permissions are $grantStatus",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onShouldRedirectToSystemSettings(blockedPermissions: List<PermissionReport>) {
                SimpleStorageHelper.redirectToSystemSettings(this@MainActivity)
            }
        })
        .build()

    private val storageHelper = SimpleStorageHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setNavigationController()

        Fixer.extractLibs(this)

        if (intent.getBooleanExtra("IS_ROOT", false)) {
            setupRootService()
        } else {
            setupSimpleStorage(savedInstanceState)
            setupStoragePermission()
        }
    }

    fun sendRequestAllProcess() {

        if (intent.getBooleanExtra("IS_ROOT", false)) {
            val message = Message.obtain(null, MSG_GET_PROCESS_LIST)
            message.replyTo = receiver
            remoteMessenger?.send(message)
        } else {
            val processList = Process.getAllProcess(this, false)

            val navController = binding.navHostFragmentActivityMain.getFragment<NavHostFragment>()
            val fragments = navController.childFragmentManager.fragments

            fragments.find { it is MemoryFragment }?.let { fragment ->
                (fragment as MemoryFragment).showProcess(processList)
            }
        }
    }

    fun sendRequestDump(
        process: String,
        dumpFile: Array<String>,
        autoFix: Boolean,
        is32Bit: Boolean,
        flagCheck: Boolean
    ) {

        if (intent.getBooleanExtra("IS_ROOT", false)) {
            val message = Message.obtain(null, MSG_DUMP_PROCESS)

            message.data.apply {
                putString(PROCESS_NAME, process)
                putStringArray(LIST_FILE, dumpFile)
                putBoolean(IS_FLAG_CHECK, flagCheck)
                if (autoFix) {
                    putBoolean(IS_FIX_NAME, true)
                    putString(LIBRARY_DIR_NAME, "${filesDir.path}/SoFixer")
                    putBoolean(LIBRARY_ARCH_BOOL, is32Bit)
                }
            }

            message.replyTo = receiver
            remoteMessenger?.send(message)
        } else {
            val dumper = Dumper(process)

            dumpFile.forEach {
                dumper.file = it

                console.appendLine(

                    try {
                        dumper.dumpFile(autoFix, is32Bit, flagCheck, "/sdcard/Download")
                    } catch (e: Exception) {
                        e.message!!
                    }

                )
            }
        }
    }

    private fun setupSimpleStorage(savedInstanceState: Bundle?) {
        savedInstanceState?.let { storageHelper.onRestoreInstanceState(it) }
    }

    private fun setupStoragePermission() {
        if (Build.VERSION.SDK_INT in 23..28) {
            permissionRequest.check()
        } else if (Build.VERSION.SDK_INT == 29){
            storageHelper.requestStorageAccess()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!storageHelper.storage.isStorageAccessGranted(StorageId.PRIMARY)) {
                storageHelper.storage.requestFullStorageAccess()
            }
        }
    }

    private fun setNavigationController() {

        val navController =
            binding.navHostFragmentActivityMain.getFragment<NavHostFragment>().navController

        val appBarConfiguration =
            AppBarConfiguration(setOf(R.id.nav_memory_fragment, R.id.nav_console_fragment))

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    private fun setupRootService() {
        if (remoteMessenger == null) {
            dumperConnection = MSGConnection(this)
            val intent = Intent(applicationContext, RootServices::class.java)
            RootService.bind(intent, dumperConnection)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if (item.itemId == R.id.github) {
            startActivity(
                Intent(
                    ACTION_VIEW,
                    Uri.parse("https://github.com/BryanGIG/PADumper")
                )
            )
        }
        return true
    }
}