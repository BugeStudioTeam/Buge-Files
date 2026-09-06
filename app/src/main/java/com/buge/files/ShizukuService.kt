package com.buge.files

import android.content.Context
import android.content.pm.PackageInstaller
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Method

class ShizukuService(private val context: Context) {

    private var shizukuAvailable: Boolean = false
    private var shizukuVersion: Int = 0
    private var binderToken: Any? = null
    private var installMethod: Method? = null
    private var uninstallMethod: Method? = null

    fun isShizukuReady(): Boolean = shizukuAvailable && binderToken != null

    suspend fun bindShizuku(): Boolean = withContext(Dispatchers.IO) {
        try {
            val clazz = Class.forName("moe.shizuku.manager.ShizukuManager")
            val getBinderMethod = clazz.getMethod("getBinder")
            val binder = getBinderMethod.invoke(null)
            if (binder != null) {
                binderToken = binder
                shizukuAvailable = true
                val versionField = clazz.getField("VERSION_CODE")
                shizukuVersion = versionField.getInt(null)
                prepareInstallMethods()
                return@withContext true
            }
        } catch (e: Exception) {
            shizukuAvailable = false
        }
        false
    }

    private fun prepareInstallMethods() {
        try {
            val pmClass = Class.forName("android.content.pm.IPackageManager")
            val getPmMethod = Class.forName("android.app.ActivityThread")
                .getMethod("getPackageManager")
            val pm = getPmMethod.invoke(null)
            val installMethod = pmClass.getMethod(
                "installPackage",
                String::class.java,
                Class.forName("android.content.pm.IPackageInstallObserver"),
                Int::class.java,
                String::class.java
            )
            this.installMethod = installMethod
        } catch (e: Exception) {
            this.installMethod = null
        }
    }

    suspend fun installApkWithShizuku(uri: Uri, installerName: String = ""): InstallResult = withContext(Dispatchers.IO) {
        if (!isShizukuReady()) return@withContext InstallResult(false, "Shizuku is not available")

        try {
            val tempFile = copyUriToTempFile(uri)
            if (tempFile == null || !tempFile.exists()) {
                return@withContext InstallResult(false, "Failed to prepare APK file")
            }

            val success = if (installMethod != null) {
                installViaShizukuApi(tempFile, installerName)
            } else {
                installViaShell(tempFile, installerName)
            }

            tempFile.delete()

            if (success) {
                InstallResult(true, "APK installed via Shizuku")
            } else {
                InstallResult(false, "Installation failed via Shizuku")
            }
        } catch (e: Exception) {
            InstallResult(false, "Shizuku installation error: ${e.message}")
        }
    }

    private fun installViaShizukuApi(apkFile: File, installerName: String): Boolean {
        return try {
            val path = apkFile.absolutePath
            val observer = createInstallObserver()
            val flags = PackageInstaller.INSTALL_REPLACE_EXISTING or PackageInstaller.INSTALL_ALLOW_DOWNGRADE
            val args = arrayOf(path, observer, flags, installerName)
            installMethod?.invoke(null, *args) ?: return false
            waitForInstallComplete()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun installViaShell(apkFile: File, installerName: String): Boolean {
        return try {
            val command = if (installerName.isNotEmpty()) {
                "pm install -i $installerName -r ${apkFile.absolutePath}"
            } else {
                "pm install -r ${apkFile.absolutePath}"
            }
            val result = runShellCommand(command)
            result.contains("Success")
        } catch (e: Exception) {
            false
        }
    }

    private fun runShellCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (output.isNotEmpty()) output else error
        } catch (e: Exception) {
            e.message ?: ""
        }
    }

    private fun createInstallObserver(): Any {
        return object {
            fun packageInstalled(packageName: String, returnCode: Int) {
            }

            fun packageInstalled(packageName: String, returnCode: Int, extras: android.os.Bundle?) {
            }
        }
    }

    private fun waitForInstallComplete() {
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val tempFile = File(context.cacheDir, "shizuku_apk_${System.currentTimeMillis()}.apk")
            if (uri.scheme == "file") {
                val source = uri.path?.let { File(it) }
                if (source != null && source.exists()) {
                    FileInputStream(source).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    return tempFile
                }
                return null
            }
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            FileOutputStream(tempFile).use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uninstallPackageWithShizuku(packageName: String): InstallResult = withContext(Dispatchers.IO) {
        if (!isShizukuReady()) return@withContext InstallResult(false, "Shizuku is not available")
        try {
            val command = "pm uninstall $packageName"
            val result = runShellCommand(command)
            if (result.contains("Success")) {
                InstallResult(true, "Package uninstalled via Shizuku")
            } else {
                InstallResult(false, "Uninstall failed: $result")
            }
        } catch (e: Exception) {
            InstallResult(false, "Uninstall error: ${e.message}")
        }
    }
}

data class InstallResult(val success: Boolean, val message: String)