package com.buge.files

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class ApkMetadata(
    val displayName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val requestedPermissions: List<String>,
    val certificateSha256: List<String>,
    val installedVersionName: String? = null,
    val installedVersionCode: Long? = null
) {
    val isInstalled: Boolean get() = installedVersionCode != null
    val isNewerThanInstalled: Boolean get() = installedVersionCode?.let { versionCode > it } ?: false
}

data class ApkReadResult(val metadata: ApkMetadata? = null, val message: String? = null)

/** Reads immutable metadata from a user-selected APK without installing or executing it. */
class ApkRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun loadIcon(entry: FileEntry): Bitmap? = withContext(Dispatchers.IO) {
        var temporary: File? = null
        try {
            val archive = if (entry.uri.scheme == ContentResolver.SCHEME_FILE) {
                entry.uri.path?.let(::File)?.takeIf { it.exists() && it.isFile } ?: return@withContext null
            } else {
                temporary = File.createTempFile("buge-apk-icon-", ".apk", context.cacheDir)
                context.contentResolver.openInputStream(entry.uri)?.use { input -> temporary!!.outputStream().use { output -> input.copyTo(output) } } ?: return@withContext null
                temporary
            }
            val info = archivePackageInfo(archive.absolutePath) ?: return@withContext null
            val application = info.applicationInfo ?: return@withContext null
            application.sourceDir = archive.absolutePath
            application.publicSourceDir = archive.absolutePath
            val drawable = application.loadIcon(packageManager)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        } catch (_: Exception) { null } finally { temporary?.delete() }
    }
    suspend fun inspect(entry: FileEntry): ApkReadResult = withContext(Dispatchers.IO) {
        var temporary: File? = null
        try {
            val archive = if (entry.uri.scheme == ContentResolver.SCHEME_FILE) {
                entry.uri.path?.let(::File)?.takeIf { it.exists() && it.isFile }
                    ?: return@withContext ApkReadResult(message = "APK is no longer available")
            } else {
                temporary = File.createTempFile("buge-apk-", ".apk", context.cacheDir)
                context.contentResolver.openInputStream(entry.uri)?.use { input ->
                    temporary!!.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext ApkReadResult(message = "APK could not be read")
                temporary
            }
            val packageInfo = archivePackageInfo(archive.absolutePath)
                ?: return@withContext ApkReadResult(message = "This is not a valid Android APK")
            val application = packageInfo.applicationInfo
                ?: return@withContext ApkReadResult(message = "APK has no application metadata")
            application.sourceDir = archive.absolutePath
            application.publicSourceDir = archive.absolutePath
            val packageName = packageInfo.packageName ?: return@withContext ApkReadResult(message = "APK has no package name")
            val installed = installedPackageInfo(packageName)
            ApkReadResult(
                metadata = ApkMetadata(
                    displayName = runCatching { application.loadLabel(packageManager).toString() }.getOrDefault(entry.name.removeSuffix(".apk")),
                    packageName = packageName,
                    versionName = packageInfo.versionName ?: "—",
                    versionCode = packageInfo.versionCodeCompat(),
                    minSdk = application.minSdkVersion,
                    targetSdk = application.targetSdkVersion,
                    requestedPermissions = packageInfo.requestedPermissions?.toList()?.sorted().orEmpty(),
                    certificateSha256 = signingCertificates(packageInfo),
                    installedVersionName = installed?.versionName,
                    installedVersionCode = installed?.versionCodeCompat()
                )
            )
        } catch (error: Exception) {
            ApkReadResult(message = "Could not inspect APK: ${error.message.orEmpty().take(100)}")
        } finally {
            temporary?.delete()
        }
    }

    private fun archivePackageInfo(path: String): PackageInfo? {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_PERMISSIONS
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") packageManager.getPackageArchiveInfo(path, flags)
        }
    }

    private fun installedPackageInfo(packageName: String): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0)
        }
    }.getOrNull()

    private fun signingCertificates(info: PackageInfo): List<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            @Suppress("DEPRECATION") info.signatures?.toList().orEmpty()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02X".format(it) }
        }
    }

    private fun PackageInfo.versionCodeCompat(): Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else {
        @Suppress("DEPRECATION") versionCode.toLong()
    }
}

fun FileEntry.isApkPackage(): Boolean = !isDirectory && extension == "apk"
