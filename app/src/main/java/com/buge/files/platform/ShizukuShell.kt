// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Buge Studio

package com.buge.files

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

data class ShellResult(val exitCode: Int, val output: String)

object ShizukuShell {
    const val REQUEST_CODE = 4102

    fun isAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        return runCatching {
            Shizuku.isPreV11() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    fun requestPermission() {
        if (!isAvailable()) return
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
    }

    fun addBinderListener(listener: Shizuku.OnBinderReceivedListener) {
        runCatching { Shizuku.addBinderReceivedListenerSticky(listener) }
    }

    fun removeBinderListener(listener: Shizuku.OnBinderReceivedListener) {
        runCatching { Shizuku.removeBinderReceivedListener(listener) }
    }

    fun addPermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        runCatching { Shizuku.addRequestPermissionResultListener(listener) }
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        runCatching { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    fun exec(command: String): ShellResult? {
        val binder = Shizuku.getBinder() ?: return null
        return runCatching {
            val service = IShizukuService.Stub.asInterface(ShizukuBinderWrapper(binder))
            val remote = service.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = ParcelFileDescriptor.AutoCloseInputStream(remote.getInputStream()).bufferedReader().use { it.readText() }
            ShellResult(remote.waitFor(), output)
        }.getOrNull()
    }
}
