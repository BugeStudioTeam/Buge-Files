package com.buge.files

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Method

class ShizukuInstaller(private val context: Context) {

    private var ipmInstance: Any? = null
    private var installMethod: Method? = null
    private var uninstallMethod: Method? = null
    private var isInitialized = false

    companion object {
        const val REQUEST_CODE = 1001
        const val INSTALL_SUCCEEDED = 1
        const val DELETE_SUCCEEDED = 1
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuPreV11(): Boolean {
        return try {
            Shizuku.isPreV11()
        } catch (e: Exception) {
            true
        }
    }

    fun hasPermission(): Boolean {
        return try {
            if (isShizukuPreV11()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun shouldShowRequestPermissionRationale(): Boolean {
        return try {
            if (isShizukuPreV11()) return false
            Shizuku.shouldShowRequestPermissionRationale()
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int = REQUEST_CODE) {
        try {
            if (isShizukuPreV11()) return
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
        }
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isShizukuAvailable()) {
                return@withContext false
            }

            if (isShizukuPreV11()) {
                return@withContext false
            }

            if (!hasPermission()) {
                return@withContext false
            }

            if (isInitialized && ipmInstance != null) {
                return@withContext true
            }

            val ipmClass = Class.forName("android.content.pm.IPackageManager")
            val ipmStub = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterfaceMethod = ipmStub.getMethod("asInterface", IBinder::class.java)

            val serviceBinder = SystemServiceHelper.getSystemService("package")
            ipmInstance = asInterfaceMethod.invoke(
                null,
                ShizukuBinderWrapper(serviceBinder)
            )

            if (ipmInstance == null) {
                return@withContext false
            }

            installMethod = ipmClass.getMethod(
                "installPackage",
                String::class.java,
                Class.forName("android.content.pm.IPackageInstallObserver"),
                Int::class.java,
                String::class.java
            )

            try {
                uninstallMethod = ipmClass.getMethod(
                    "deletePackage",
                    String::class.java,
                    Class.forName("android.content.pm.IPackageDeleteObserver"),
                    Int::class.java
                )
            } catch (e: NoSuchMethodException) {
                uninstallMethod = null
            }

            isInitialized = true
            return@withContext true
        } catch (e: Exception) {
            isInitialized = false
            ipmInstance = null
            installMethod = null
            uninstallMethod = null
            return@withContext false
        }
    }

    suspend fun installApk(uri: Uri, installerName: String = ""): InstallResult = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) {
            return@withContext InstallResult(false, "Shizuku is not available")
        }

        if (isShizukuPreV11()) {
            return@withContext InstallResult(false, "Shizuku version is too old")
        }

        if (!hasPermission()) {
            return@withContext InstallResult(false, "Shizuku permission not granted")
        }

        if (!isInitialized || ipmInstance == null) {
            val initialized = initialize()
            if (!initialized) {
                return@withContext InstallResult(false, "Failed to initialize Shizuku")
            }
        }

        try {
            val tempFile = copyToTempFile(uri)
            if (tempFile == null || !tempFile.exists()) {
                return@withContext InstallResult(false, "Failed to prepare APK file")
            }

            val resultHolder = InstallResultHolder()

            val observer = createInstallObserver { success ->
                resultHolder.success = success
                resultHolder.completed = true
            }

            val flags = 0x00000002 or 0x00000004

            val method = installMethod
            if (method == null) {
                tempFile.delete()
                return@withContext InstallResult(false, "Install method not available")
            }

            val args = arrayOf(
                tempFile.absolutePath,
                observer,
                flags,
                installerName
            )

            method.invoke(ipmInstance, *args)

            var attempts = 0
            while (!resultHolder.completed && attempts < 60) {
                Thread.sleep(500)
                attempts++
            }

            tempFile.delete()

            if (resultHolder.success) {
                InstallResult(true, "APK installed via Shizuku")
            } else {
                InstallResult(false, "Installation failed or timed out")
            }
        } catch (e: Exception) {
            InstallResult(false, "Installation error: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun uninstallPackage(packageName: String): InstallResult = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) {
            return@withContext InstallResult(false, "Shizuku is not available")
        }

        if (isShizukuPreV11()) {
            return@withContext InstallResult(false, "Shizuku version is too old")
        }

        if (!hasPermission()) {
            return@withContext InstallResult(false, "Shizuku permission not granted")
        }

        if (!isInitialized || ipmInstance == null) {
            val initialized = initialize()
            if (!initialized) {
                return@withContext InstallResult(false, "Failed to initialize Shizuku")
            }
        }

        val method = uninstallMethod
        if (method == null) {
            return@withContext InstallResult(false, "Uninstall method not available")
        }

        try {
            val resultHolder = InstallResultHolder()

            val observer = createDeleteObserver { success ->
                resultHolder.success = success
                resultHolder.completed = true
            }

            val flags = 0

            method.invoke(ipmInstance, packageName, observer, flags)

            var attempts = 0
            while (!resultHolder.completed && attempts < 40) {
                Thread.sleep(500)
                attempts++
            }

            if (resultHolder.success) {
                InstallResult(true, "Package uninstalled via Shizuku")
            } else {
                InstallResult(false, "Uninstall failed or timed out")
            }
        } catch (e: Exception) {
            InstallResult(false, "Uninstall error: ${e.message ?: "Unknown error"}")
        }
    }

    private class InstallResultHolder {
        var success = false
        var completed = false
    }

    private fun createInstallObserver(onResult: (Boolean) -> Unit): Any {
        val observerClass = Class.forName("android.content.pm.IPackageInstallObserver")
        return java.lang.reflect.Proxy.newProxyInstance(
            observerClass.classLoader,
            arrayOf(observerClass)
        ) { _, method, args ->
            if (method.name == "packageInstalled") {
                val returnCode = args?.getOrNull(1) as? Int ?: 0
                onResult(returnCode == INSTALL_SUCCEEDED)
            }
            null
        }
    }

    private fun createDeleteObserver(onResult: (Boolean) -> Unit): Any {
        val observerClass = Class.forName("android.content.pm.IPackageDeleteObserver")
        return java.lang.reflect.Proxy.newProxyInstance(
            observerClass.classLoader,
            arrayOf(observerClass)
        ) { _, method, args ->
            if (method.name == "packageDeleted") {
                val returnCode = args?.getOrNull(1) as? Int ?: 0
                onResult(returnCode == DELETE_SUCCEEDED)
            }
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
        isInitialized = false
        ipmInstance = null
        installMethod = null
        uninstallMethod = null
    }
}

data class InstallResult(val success: Boolean, val message: String)