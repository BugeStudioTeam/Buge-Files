// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Buge Studio

package com.buge.files

import android.content.ContentResolver
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppInstaller(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun install(file: FileEntry, settings: AppSettings): OperationResult = withContext(Dispatchers.IO) {
        val apk = prepare(file) ?: return@withContext OperationResult(false, "APK is not available")
        try {
            val flags = buildList {
                add("-r")
                if (settings.allowDowngrade) add("-d")
                if (settings.allowTestPackages) add("-t")
                if (settings.installAllUsers) { add("--user"); add("all") }
                if (settings.installerPackage.isNotBlank()) {
                    add("-i")
                    add(settings.installerPackage.trim())
                }
            }
            val command = "pm install ${flags.joinToString(" ")} ${shellQuote(apk.path)}"
            val result = when (settings.installMethod) {
                InstallMethod.ROOT -> runAsRoot(settings.rootCommand, command)
                InstallMethod.SHIZUKU -> ShizukuShell.exec(command)
            } ?: return@withContext OperationResult(false, "Installation is unavailable")
            val output = result.output.trim()
            val lowered = output.lowercase()
            val success = result.exitCode == 0 && !lowered.contains("failure") && !lowered.contains("error") && !lowered.contains("exception")
            OperationResult(success, if (success) "Installed" else output.ifBlank { "Installation failed" }.take(220))
        } finally {
            apk.delete()
        }
    }

    private fun prepare(file: FileEntry): File? = runCatching {
        val directory = context.externalCacheDir ?: context.cacheDir
        val target = File(directory, "install_${System.currentTimeMillis()}.apk")
        val input = if (file.uri.scheme == ContentResolver.SCHEME_FILE) {
            file.uri.path?.let(::File)?.takeIf { it.exists() }?.inputStream()
        } else {
            resolver.openInputStream(file.uri)
        } ?: return null
        input.use { source -> target.outputStream().use { output -> source.copyTo(output) } }
        target
    }.getOrNull()

    private fun runAsRoot(rootCommand: String, command: String): ShellResult? = runCatching {
        val executable = rootCommand.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.ifEmpty { listOf("su") }
        val process = ProcessBuilder(executable + listOf("-c", command)).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        ShellResult(process.waitFor(), output)
    }.getOrNull()

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
