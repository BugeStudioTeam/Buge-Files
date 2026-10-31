// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Buge Studio

package com.buge.files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateRepository {
    suspend fun check(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val connection = try {
            (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Buge-Files")
            }
        } catch (error: Exception) {
            return@withContext UpdateCheckResult(currentVersion, error = error.message ?: "Network error")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext UpdateCheckResult(currentVersion, error = "GitHub returned HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").trim().ifBlank { json.optString("name").trim() }
            if (tag.isBlank()) {
                return@withContext UpdateCheckResult(currentVersion, error = "Release version was missing")
            }
            val latest = tag.removePrefix("v").removePrefix("V")
            val downloadUrl = "https://github.com/BugeStudioTeam/Buge-Files/releases/download/$tag/app-release.apk"
            UpdateCheckResult(
                currentVersion = currentVersion,
                latestVersion = latest,
                downloadUrl = downloadUrl,
                updateAvailable = isNewer(latest, currentVersion)
            )
        } catch (error: Exception) {
            UpdateCheckResult(currentVersion, error = error.message ?: "Network error")
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split('.', '-', '+').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.', '-', '+').mapNotNull { it.toIntOrNull() }
        for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
            val left = latestParts.getOrElse(index) { 0 }
            val right = currentParts.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private companion object {
        const val RELEASES_API = "https://api.github.com/repos/BugeStudioTeam/Buge-Files/releases/latest"
    }
}
