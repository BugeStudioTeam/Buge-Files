package com.buge.files

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.util.Locale

enum class AppDestination { BROWSE, RECENTS, FAVORITES, STORAGE, SETTINGS }
enum class ViewMode { LIST, GRID }
enum class SortOption { NAME, DATE, SIZE, TYPE }
enum class ClipboardMode { COPY, MOVE }
enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class ColorSource { DYNAMIC, INDIGO, OCEAN, FOREST, SUNSET, ORCHID }
enum class AppLanguage(val code: String, val nativeName: String) {
    SYSTEM("system", "System default"),
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    RUSSIAN("ru", "Русский"),
    PORTUGUESE("pt", "Português"),
    PORTUGUESE_BRAZIL("pt-rBR", "Português (Brasil)"),
    SPANISH("es", "Español"),
    CHINESE("zh", "中文 (简体)"),
    CHINESE_TRADITIONAL("zh-rTW", "中文 (繁体)"),
    ARABIC("ar", "العربية"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어");

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

fun AppLanguage.resolved(): AppLanguage {
    if (this != AppLanguage.SYSTEM) return this
    val locale = Locale.getDefault()
    val exact = AppLanguage.values().firstOrNull { it != AppLanguage.SYSTEM && it.code.equals(locale.toLanguageTag(), ignoreCase = true) }
    if (exact != null) return exact
    return when (locale.language.lowercase(Locale.ROOT)) {
        "fr" -> AppLanguage.FRENCH
        "de" -> AppLanguage.GERMAN
        "ru" -> AppLanguage.RUSSIAN
        "pt" -> if (locale.country.equals("BR", true)) AppLanguage.PORTUGUESE_BRAZIL else AppLanguage.PORTUGUESE
        "es" -> AppLanguage.SPANISH
        "zh" -> if (locale.country.equals("TW", true) || locale.country.equals("HK", true)) AppLanguage.CHINESE_TRADITIONAL else AppLanguage.CHINESE
        "ar" -> AppLanguage.ARABIC
        "ja" -> AppLanguage.JAPANESE
        "ko" -> AppLanguage.KOREAN
        else -> AppLanguage.ENGLISH
    }
}

@Immutable
data class FileEntry(
    val uri: Uri,
    val name: String,
    val mimeType: String?,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val childCount: Int = 0
) {
    val extension: String get() = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
}

@Immutable
data class StorageBreakdown(
    val documents: Long = 0,
    val images: Long = 0,
    val videos: Long = 0,
    val audio: Long = 0,
    val archives: Long = 0,
    val other: Long = 0,
    val files: Int = 0,
    val folders: Int = 0
) {
    val total: Long get() = documents + images + videos + audio + archives + other
}

@Immutable
data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val colorSource: ColorSource = ColorSource.DYNAMIC,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val viewMode: ViewMode = ViewMode.LIST,
    val compactMode: Boolean = false,
    val showHidden: Boolean = false,
    val showThumbnails: Boolean = false,
    val hapticFeedback: Boolean = true
)

@Immutable
data class RootLocation(val uri: Uri, val label: String)

@Immutable
data class ClipboardState(val entries: List<FileEntry>, val mode: ClipboardMode)

data class OperationResult(val success: Boolean, val message: String)
