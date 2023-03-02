package com.dumper.android.core

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.topjohnwu.superuser.Shell

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shell.getShell {
            val methodStr = if (it.isRoot) "root" else "non-root"
            Toast.makeText(this@SplashActivity, "Using $methodStr method", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.putExtra("IS_ROOT", it.isRoot)

            startActivity(intent)
            finish()
        }
    }
}