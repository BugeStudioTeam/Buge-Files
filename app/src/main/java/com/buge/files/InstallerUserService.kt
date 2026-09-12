package com.buge.files

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.io.FileInputStream

class InstallerUserService : Service() {
    private val binder: IInstallerService = object : IInstallerService.Stub() {
        override fun install(installerPackage: String?, apk: ParcelFileDescriptor?) {
            requireNotNull(apk) { "APK descriptor is missing" }
            val installer = installerPackage?.trim()?.takeIf { it.isNotEmpty() }
            val size = FileInputStream(apk.fileDescriptor).channel.size()
            val command = buildString {
                append("pm install")
                if (installer != null) append(" -i '").append(installer.replace("'", "'\\''")).append("'")
                append(" -r -S ").append(size)
            }
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            FileInputStream(apk.fileDescriptor).use { input ->
                process.outputStream.use { output -> input.copyTo(output) }
            }
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            check(exitCode == 0 && output.contains("Success", ignoreCase = true)) {
                (error.ifBlank { output }).trim().ifBlank { "Shizuku installation failed ($exitCode)" }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder.asBinder()
}
