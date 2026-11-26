// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Buge Studio

package com.buge.files

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "buge_preferences")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val colorSource = stringPreferencesKey("color_source")
        val language = stringPreferencesKey("language")
        val viewMode = stringPreferencesKey("view_mode")
        val compact = stringPreferencesKey("compact")
        val hidden = stringPreferencesKey("hidden")
        val thumbnails = stringPreferencesKey("thumbnails")
        val haptics = stringPreferencesKey("haptics")
        val rootAccess = booleanPreferencesKey("root_access")
        val rootCommand = stringPreferencesKey("root_command")
        val shizukuAccess = booleanPreferencesKey("shizuku_access")
        val installEnabled = booleanPreferencesKey("install_enabled")
        val installMethod = stringPreferencesKey("install_method")
        val installerPackage = stringPreferencesKey("installer_package")
        val allowDowngrade = booleanPreferencesKey("allow_downgrade")
        val allowTestPackages = booleanPreferencesKey("allow_test_packages")
        val installAllUsers = booleanPreferencesKey("install_all_users")
        val showRecents = booleanPreferencesKey("show_recents")
        val fabStyle = stringPreferencesKey("fab_style")
        val pureBlack = booleanPreferencesKey("pure_black")
        val useSystemFont = booleanPreferencesKey("use_system_font")
        val autoRemount = booleanPreferencesKey("auto_remount")
        val roots = stringSetPreferencesKey("roots")
        val bookmarks = stringSetPreferencesKey("bookmarks")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            theme = enumValue(prefs[Keys.theme], ThemePreference.SYSTEM),
            colorSource = enumValue(prefs[Keys.colorSource], ColorSource.DYNAMIC),
            language = AppLanguage.fromCode(prefs[Keys.language]),
            viewMode = enumValue(prefs[Keys.viewMode], ViewMode.LIST),
            compactMode = prefs[Keys.compact]?.toBoolean() ?: false,
            showHidden = prefs[Keys.hidden]?.toBoolean() ?: false,
            showThumbnails = prefs[Keys.thumbnails]?.toBoolean() ?: false,
            hapticFeedback = prefs[Keys.haptics]?.toBoolean() ?: true,
            rootAccess = prefs[Keys.rootAccess] ?: false,
            rootCommand = prefs[Keys.rootCommand]?.takeIf { it.isNotBlank() } ?: "su",
            shizukuAccess = prefs[Keys.shizukuAccess] ?: false,
            installEnabled = prefs[Keys.installEnabled] ?: false,
            installMethod = enumValue(prefs[Keys.installMethod], InstallMethod.SHIZUKU),
            installerPackage = prefs[Keys.installerPackage].orEmpty(),
            allowDowngrade = prefs[Keys.allowDowngrade] ?: true,
            allowTestPackages = prefs[Keys.allowTestPackages] ?: false,
            installAllUsers = prefs[Keys.installAllUsers] ?: false,
            showRecents = prefs[Keys.showRecents] ?: true,
            fabStyle = enumValue(prefs[Keys.fabStyle], FabStyle.EXTENDED),
            pureBlack = prefs[Keys.pureBlack] ?: false,
            useSystemFont = prefs[Keys.useSystemFont] ?: false,
            autoRemount = prefs[Keys.autoRemount] ?: false
        )
    }

    val roots: Flow<List<RootLocation>> = context.dataStore.data.map { prefs ->
        prefs[Keys.roots].orEmpty().mapNotNull(::decodeLocation).sortedBy { it.label.lowercase() }
    }

    val bookmarks: Flow<List<RootLocation>> = context.dataStore.data.map { prefs ->
        prefs[Keys.bookmarks].orEmpty().mapNotNull(::decodeLocation).sortedBy { it.label.lowercase() }
    }

    suspend fun update(settings: AppSettings) = context.dataStore.edit { prefs ->
        prefs[Keys.theme] = settings.theme.name
        prefs[Keys.colorSource] = settings.colorSource.name
        prefs[Keys.language] = settings.language.code
        prefs[Keys.viewMode] = settings.viewMode.name
        prefs[Keys.compact] = settings.compactMode.toString()
        prefs[Keys.hidden] = settings.showHidden.toString()
        prefs[Keys.thumbnails] = settings.showThumbnails.toString()
        prefs[Keys.haptics] = settings.hapticFeedback.toString()
        prefs[Keys.rootAccess] = settings.rootAccess
        prefs[Keys.rootCommand] = settings.rootCommand.ifBlank { "su" }
        prefs[Keys.shizukuAccess] = settings.shizukuAccess
        prefs[Keys.installEnabled] = settings.installEnabled
        prefs[Keys.installMethod] = settings.installMethod.name
        prefs[Keys.installerPackage] = settings.installerPackage
        prefs[Keys.allowDowngrade] = settings.allowDowngrade
        prefs[Keys.allowTestPackages] = settings.allowTestPackages
        prefs[Keys.installAllUsers] = settings.installAllUsers
        prefs[Keys.showRecents] = settings.showRecents
        prefs[Keys.fabStyle] = settings.fabStyle.name
        prefs[Keys.pureBlack] = settings.pureBlack
        prefs[Keys.useSystemFont] = settings.useSystemFont
        prefs[Keys.autoRemount] = settings.autoRemount
    }

    suspend fun addRoot(location: RootLocation) = context.dataStore.edit { prefs ->
        prefs[Keys.roots] = prefs[Keys.roots].orEmpty() + encodeLocation(location)
    }

    suspend fun removeRoot(uri: Uri) = context.dataStore.edit { prefs ->
        prefs[Keys.roots] = prefs[Keys.roots].orEmpty().filterNot { decodeLocation(it)?.uri == uri }.toSet()
        prefs[Keys.bookmarks] = prefs[Keys.bookmarks].orEmpty().filterNot { decodeLocation(it)?.uri == uri }.toSet()
    }

    suspend fun toggleBookmark(location: RootLocation) = context.dataStore.edit { prefs ->
        val existing = prefs[Keys.bookmarks].orEmpty()
        val encoded = encodeLocation(location)
        val contains = existing.any { decodeLocation(it)?.uri == location.uri }
        prefs[Keys.bookmarks] = if (contains) existing.filterNot { decodeLocation(it)?.uri == location.uri }.toSet() else existing + encoded
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private fun encodeLocation(location: RootLocation) = "${location.uri}\u001F${location.label.replace("\u001F", " ")}" 
    private fun decodeLocation(encoded: String): RootLocation? {
        val split = encoded.split("\u001F", limit = 2)
        if (split.size != 2) return null
        return runCatching { RootLocation(Uri.parse(split[0]), split[1]) }.getOrNull()
    }
}
