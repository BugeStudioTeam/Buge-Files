package com.buge.files

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Method

class ShizukuManager(private val context: Context) {

    private var shizukuBinder: IBinder? = null
    private var isBound: Boolean = false
    private var serviceConnection: ServiceConnection? = null
    private var shizukuServiceClass: Class<*>? = null

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.manager"
        private const val SHIZUKU_SERVICE = "moe.shizuku.manager.ShizukuService"
    }

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val binder = getShizukuBinder()
            if (binder != null) {
                shizukuBinder = binder
                return@withContext true
            }
            val intent = Intent().apply {
                component = ComponentName(SHIZUKU_PACKAGE, SHIZUKU_SERVICE)
            }
            val deferred = CompletableDeferred<Boolean>()
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    shizukuBinder = service
                    isBound = true
                    deferred.complete(true)
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    shizukuBinder = null
                    isBound = false
                    deferred.complete(false)
                }
            }
            val bound = context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
            if (!bound) {
                deferred.complete(false)
            }
            deferred.await()
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        try {
            serviceConnection?.let { context.unbindService(it) }
        } catch (e: Exception) {
        }
        isBound = false
        shizukuBinder = null
    }

    private fun getShizukuBinder(): IBinder? {
        return try {
            val clazz = Class.forName("moe.shizuku.manager.ShizukuManager")
            val method = clazz.getMethod("getBinder")
            val binder = method.invoke(null) as? IBinder
            binder
        } catch (e: Exception) {
            null
        }
    }

    fun isShizukuConnected(): Boolean = shizukuBinder != null

    suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!isShizukuConnected()) return@withContext "Shizuku not connected"
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (output.isNotEmpty()) output else error
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun executeShellCommand(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (!isShizukuConnected()) {
            return@withContext ShellResult(false, "Shizuku not connected", "")
        }
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode == 0, output, error)
        } catch (e: Exception) {
            ShellResult(false, "", e.message ?: "Unknown error")
        }
    }

    data class ShellResult(val success: Boolean, val output: String, val error: String)
}