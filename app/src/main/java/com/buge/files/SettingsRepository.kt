package com.buge.files

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
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
        val haptics = stringPreferencesKey("haptics")
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
            hapticFeedback = prefs[Keys.haptics]?.toBoolean() ?: true
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
        prefs[Keys.haptics] = settings.hapticFeedback.toString()
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
