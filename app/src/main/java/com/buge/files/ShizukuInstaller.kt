package com.buge.files

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ShizukuInstaller(private val context: Context) {

    private val shizukuManager = ShizukuManager(context)
    private var isInitialized = false

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        isInitialized = shizukuManager.connect()
        isInitialized
    }

    fun isReady(): Boolean = shizukuManager.isShizukuConnected()

    suspend fun installApk(uri: Uri, installerName: String = ""): InstallResult = withContext(Dispatchers.IO) {
        if (!isReady()) {
            val reconnected = initialize()
            if (!reconnected) {
                return@withContext InstallResult(false, "Shizuku not available")
            }
        }

        try {
            val tempFile = copyToTempFile(uri) ?: return@withContext InstallResult(false, "Failed to prepare APK")

            val installCmd = if (installerName.isNotEmpty()) {
                "pm install -i \"$installerName\" -r \"${tempFile.absolutePath}\""
            } else {
                "pm install -r \"${tempFile.absolutePath}\""
            }

            val result = shizukuManager.executeShellCommand(installCmd)

            tempFile.delete()

            if (result.success && result.output.contains("Success")) {
                InstallResult(true, "APK installed via Shizuku")
            } else {
                val error = result.error.ifEmpty { result.output }
                InstallResult(false, "Installation failed: $error")
            }
        } catch (e: Exception) {
            InstallResult(false, "Installation error: ${e.message}")
        }
    }

    suspend fun uninstallPackage(packageName: String): InstallResult = withContext(Dispatchers.IO) {
        if (!isReady()) {
            val reconnected = initialize()
            if (!reconnected) {
                return@withContext InstallResult(false, "Shizuku not available")
            }
        }
        try {
            val result = shizukuManager.executeShellCommand("pm uninstall $packageName")
            if (result.success && result.output.contains("Success")) {
                InstallResult(true, "Package uninstalled via Shizuku")
            } else {
                InstallResult(false, "Uninstall failed: ${result.error}")
            }
        } catch (e: Exception) {
            InstallResult(false, "Uninstall error: ${e.message}")
        }
    }

    suspend fun listInstalledPackages(): List<String> = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext emptyList()
        try {
            val result = shizukuManager.executeShellCommand("pm list packages")
            if (result.success) {
                result.output.split("\n")
                    .map { it.trim() }
                    .filter { it.startsWith("package:") }
                    .map { it.removePrefix("package:") }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPackageInfo(packageName: String): String? = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext null
        try {
            val result = shizukuManager.executeShellCommand("pm dump $packageName | grep -E 'versionName|versionCode|package' | head -3")
            if (result.success && result.output.isNotEmpty()) {
                result.output
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun copyToTempFile(uri: Uri): File? {
        return try {
            val tempFile = File(context.cacheDir, "shizuku_apk_${System.currentTimeMillis()}.apk")
            if (uri.scheme == "file") {
                val source = uri.path?.let { File(it) }
                if (source != null && source.exists() && source.isFile) {
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

    fun cleanup() {
        shizukuManager.disconnect()
        isInitialized = false
    }
}