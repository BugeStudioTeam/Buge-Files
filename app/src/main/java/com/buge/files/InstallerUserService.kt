package com.buge.files

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

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

            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val stdoutThread = Thread { runCatching { stdout.append(process.inputStream.bufferedReader().readText()) } }
            val stderrThread = Thread { runCatching { stderr.append(process.errorStream.bufferedReader().readText()) } }
            stdoutThread.start()
            stderrThread.start()

            val writer = Thread {
                runCatching {
                    FileInputStream(apk.fileDescriptor).use { input ->
                        process.outputStream.use { output -> input.copyTo(output) }
                    }
                }.onFailure { process.destroyForcibly() }
            }
            writer.start()

            val finished = process.waitFor(60, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            runCatching { stdoutThread.join(5_000) }
            runCatching { stderrThread.join(5_000) }
            runCatching { writer.join(5_000) }

            val output = stdout.toString()
            val error = stderr.toString()
            val exitCode = if (finished) process.exitValue() else -1
            check(finished && exitCode == 0 && output.contains("Success", ignoreCase = true)) {
                (error.ifBlank { output }).trim().ifBlank { "Shizuku installation failed ($exitCode)" }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder.asBinder()
}
