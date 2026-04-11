package com.buge.files.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PrefsManager(context: Context) {

    private val prefs = context.getSharedPreferences("buge_files_prefs", Context.MODE_PRIVATE)

    var themeMode: Int
        get() = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit().putInt("theme_mode", value).apply()

    var showHiddenFiles: Boolean
        get() = prefs.getBoolean("show_hidden", false)
        set(value) = prefs.edit().putBoolean("show_hidden", value).apply()

    var isGridView: Boolean
        get() = prefs.getBoolean("is_grid_view", false)
        set(value) = prefs.edit().putBoolean("is_grid_view", value).apply()

    var favoritesPaths: Set<String>
        get() = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("favorites", value).apply()

    fun addFavorite(path: String) {
        favoritesPaths = favoritesPaths.toMutableSet().apply { add(path) }
    }

    fun removeFavorite(path: String) {
        favoritesPaths = favoritesPaths.toMutableSet().apply { remove(path) }
    }

    fun isFavorite(path: String): Boolean = favoritesPaths.contains(path)
}