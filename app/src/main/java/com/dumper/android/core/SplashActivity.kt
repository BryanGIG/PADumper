package com.dumper.android.core

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.edit
import com.dumper.android.BuildConfig
import com.dumper.android.ui.config.IGNORE_KSU
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {
    companion object {
        init {
            if (Shell.getCachedShell() != null) {
                Shell.enableVerboseLogging = BuildConfig.DEBUG;
                Shell.setDefaultBuilder(
                    Shell.Builder.create()
                        .setFlags(Shell.FLAG_REDIRECT_STDERR)
                        .setTimeout(10)
                )
            }
        }
    }

    private val sharedPreferences by lazy {
        getSharedPreferences("settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        kotlin.runCatching {
            val launcher = packageManager.getLaunchIntentForPackage("me.weishu.kernelsu")
            if (launcher != null) {
                requestKSUPermission(launcher)
                return
            }
        }

        initShell()
    }

    private fun requestKSUPermission(launcher: Intent) {
        sharedPreferences.getBoolean(IGNORE_KSU, false).also {
            if (it) {
                initShell()
                return
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Warning")
            .setMessage(
                "KernelSU is detected, please grant root permission manually!\n\n" +
                        "If you want to use Non Root method, ignore this 'warning and continue."
            )
            .setPositiveButton("Launch") { _, _ ->
                startActivity(launcher)
                exitProcess(0)
            }
            .setNegativeButton("Ignore") { _, _ ->
                sharedPreferences.edit(commit = true) {
                    putBoolean(IGNORE_KSU, true)
                }
                initShell()
            }
            .setOnCancelListener {
                sharedPreferences.edit(commit = true) {
                    putBoolean(IGNORE_KSU, true)
                }
                initShell()
            }
            .show()
    }

    private fun initShell() {
        Shell.getShell { shell ->
            val methodStr = if (shell.isRoot) "root" else "non-root"
            Toast.makeText(applicationContext, "Using $methodStr method", Toast.LENGTH_SHORT)
                .show()
            launchActivity()
        }
    }

    private fun launchActivity() {
        Intent(applicationContext, MainActivity::class.java).also {
            startActivity(it)
            finish()
        }
    }
}