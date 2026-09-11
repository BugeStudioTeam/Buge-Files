package com.buge.files

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuManager {

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    fun isPreV11(): Boolean = try {
        Shizuku.isPreV11()
    } catch (e: Exception) {
        true
    }

    fun getVersion(): Int = try {
        Shizuku.getVersion()
    } catch (e: Exception) {
        0
    }

    fun hasPermission(): Boolean = try {
        if (isPreV11()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }

    fun shouldShowRationale(): Boolean = try {
        if (isPreV11()) false
        else Shizuku.shouldShowRequestPermissionRationale()
    } catch (e: Exception) {
        false
    }

    fun requestPermission(requestCode: Int) {
        try {
            if (!isPreV11()) Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
        }
    }

    fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (e: Exception) {
        }
    }

    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (e: Exception) {
        }
    }

    fun addBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
        try {
            Shizuku.addBinderReceivedListener(listener)
        } catch (e: Exception) {
        }
    }

    fun addBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
        try {
            Shizuku.addBinderDeadListener(listener)
        } catch (e: Exception) {
        }
    }

    fun removeBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
        try {
            Shizuku.removeBinderReceivedListener(listener)
        } catch (e: Exception) {
        }
    }

    fun removeBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
        try {
            Shizuku.removeBinderDeadListener(listener)
        } catch (e: Exception) {
        }
    }
}