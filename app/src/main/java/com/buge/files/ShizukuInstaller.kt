package com.buge.files

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.os.ParcelFileDescriptor
import rikka.shizuku.Shizuku
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ShizukuInstaller {
    const val REQUEST_CODE = 4105

    fun isAvailable(): Boolean = !Shizuku.isPreV11() && runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun hasBinder(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun install(context: Context, uri: Uri, installerPackage: String): Result<String> = runCatching {
        check(isAvailable()) { "Shizuku permission is not granted" }
        val temporary = File.createTempFile("buge-install-", ".apk", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Unable to read APK")
            val descriptor = ParcelFileDescriptor.open(temporary, ParcelFileDescriptor.MODE_READ_ONLY)
            val latch = CountDownLatch(1)
            var result: Result<String> = Result.failure(IllegalStateException("Shizuku service unavailable"))
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    try {
                        IInstallerService.Stub.asInterface(service)!!.install(installerPackage, descriptor)
                        result = Result.success("Success")
                    } catch (t: Throwable) {
                        result = Result.failure(t)
                    } finally {
                        runCatching { descriptor.close() }
                        latch.countDown()
                    }
                }
                override fun onServiceDisconnected(name: ComponentName) {
                    latch.countDown()
                }
            }
            val args = Shizuku.UserServiceArgs(ComponentName(context, InstallerUserService::class.java))
                .daemon(false)
                .tag("buge-installer")
                .processNameSuffix("installer")
                .version(1)
            Shizuku.bindUserService(args, connection)
            check(latch.await(90, TimeUnit.SECONDS)) { "Timed out waiting for Shizuku" }
            runCatching { Shizuku.unbindUserService(args, connection, true) }
            result.getOrThrow()
        } finally {
            temporary.delete()
        }
    }
}

fun requestShizukuPermissionIfNeeded(onResult: (Boolean) -> Unit) {
    if (ShizukuInstaller.isAvailable()) {
        onResult(true)
        return
    }
    if (!ShizukuInstaller.hasBinder()) {
        onResult(false)
        return
    }
    runCatching { Shizuku.requestPermission(ShizukuInstaller.REQUEST_CODE) }
        .onFailure { onResult(false) }
}
