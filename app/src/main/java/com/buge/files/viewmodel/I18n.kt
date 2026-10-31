package com.buge.files

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object I18n {
    private var appContext: Context? = null
    private val resourcesByLanguage = mutableMapOf<AppLanguage, Resources>()
    private val values = mutableMapOf<Pair<AppLanguage, String>, String>()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        resourcesByLanguage.clear()
        values.clear()
    }

    private fun resources(language: AppLanguage): Resources? {
        val context = appContext ?: return null
        return resourcesByLanguage.getOrPut(language) {
            if (language == AppLanguage.SYSTEM) {
                context.resources
            } else {
                val locale = Locale.forLanguageTag(language.code.replace("-r", "-"))
                val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
                context.createConfigurationContext(configuration).resources
            }
        }
    }

    fun value(language: AppLanguage, key: String): String? {
        val cacheKey = language to key
        values[cacheKey]?.let { return it }
        val context = appContext ?: return null
        val resources = resources(language) ?: return null
        val id = resources.getIdentifier(key, "string", context.packageName)
        if (id == 0) return null
        val value = runCatching { resources.getString(id) }.getOrNull() ?: return null
        values[cacheKey] = value
        return value
    }
}

fun AppLanguage.t(key: String): String {
    val language = if (this == AppLanguage.SYSTEM) resolved() else this
    return I18n.value(language, key) ?: I18n.value(AppLanguage.ENGLISH, key) ?: key
}
