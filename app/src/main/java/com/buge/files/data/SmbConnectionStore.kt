package com.buge.files

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class SmbCredentials(
    val host: String,
    val share: String,
    val username: String,
    val password: String,
    val domain: String = "",
    val port: Int = 445
) {
    val authority: String get() = "$host/$share"
    val displayLabel: String get() = if (share.isBlank()) host else "$host/$share"
}

class SmbConnectionStore(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "buge_smb_connections",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(credentials: SmbCredentials) {
        prefs.edit()
            .putString(keyPrefix(credentials.authority) + "host", credentials.host)
            .putString(keyPrefix(credentials.authority) + "share", credentials.share)
            .putString(keyPrefix(credentials.authority) + "user", credentials.username)
            .putString(keyPrefix(credentials.authority) + "pass", credentials.password)
            .putString(keyPrefix(credentials.authority) + "domain", credentials.domain)
            .putInt(keyPrefix(credentials.authority) + "port", credentials.port)
            .putStringSet(indexKey, prefs.getStringSet(indexKey, emptySet()).orEmpty() + credentials.authority)
            .apply()
    }

    fun load(host: String, share: String): SmbCredentials? {
        val authority = "$host/$share"
        val prefix = keyPrefix(authority)
        val storedHost = prefs.getString(prefix + "host", null) ?: return null
        return SmbCredentials(
            host = storedHost,
            share = prefs.getString(prefix + "share", share).orEmpty(),
            username = prefs.getString(prefix + "user", "").orEmpty(),
            password = prefs.getString(prefix + "pass", "").orEmpty(),
            domain = prefs.getString(prefix + "domain", "").orEmpty(),
            port = prefs.getInt(prefix + "port", 445)
        )
    }

    fun remove(host: String, share: String) {
        val authority = "$host/$share"
        val prefix = keyPrefix(authority)
        prefs.edit()
            .remove(prefix + "host")
            .remove(prefix + "share")
            .remove(prefix + "user")
            .remove(prefix + "pass")
            .remove(prefix + "domain")
            .remove(prefix + "port")
            .putStringSet(indexKey, prefs.getStringSet(indexKey, emptySet()).orEmpty() - authority)
            .apply()
    }

    private fun keyPrefix(authority: String) = authority.replace(Regex("[^A-Za-z0-9]"), "_") + "::"

    private companion object {
        const val indexKey = "connection_index"
    }
}
