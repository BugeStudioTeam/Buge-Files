package com.buge.files

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class CopyProgressNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var lastUpdateAt = 0L

    private val notificationId = NOTIFICATION_ID

    private fun canPost(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "File transfers", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows copy and move progress"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun build(title: String, text: String, bytesDone: Long, bytesTotal: Long, itemsDone: Int, itemsTotal: Int, indeterminate: Boolean): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(if (indeterminate) 0 else 100, percentage(bytesDone, bytesTotal), indeterminate)
        if (itemsTotal > 0) builder.setSubText("$itemsDone/$itemsTotal")
        return builder.build()
    }

    private fun percentage(bytesDone: Long, bytesTotal: Long): Int {
        if (bytesTotal <= 0L) return 0
        return ((bytesDone.coerceAtLeast(0L) * 100L) / bytesTotal).coerceIn(0L, 100L).toInt()
    }

    fun start(label: String, title: String) {
        ensureChannel()
        if (!canPost()) return
        val text = if (label.isBlank()) title else label
        runCatching { manager.notify(notificationId, build(title, text, 0L, 0L, 0, 0, true)) }
    }

    fun update(label: String, title: String, bytesDone: Long, bytesTotal: Long, itemsDone: Int, itemsTotal: Int) {
        if (!canPost()) return
        val now = System.currentTimeMillis()
        val finished = itemsTotal > 0 && itemsDone >= itemsTotal
        if (!finished && now - lastUpdateAt < UPDATE_INTERVAL_MS) return
        lastUpdateAt = now
        val text = if (label.isBlank()) "${percentage(bytesDone, bytesTotal)}%" else "$label · ${percentage(bytesDone, bytesTotal)}%"
        runCatching { manager.notify(notificationId, build(title, text, bytesDone, bytesTotal, itemsDone, itemsTotal, false)) }
    }

    fun finish(success: Boolean, title: String, message: String) {
        runCatching { manager.cancel(notificationId) }
        if (!canPost()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        runCatching { manager.notify(notificationId, builder.build()) }
    }

    companion object {
        private const val CHANNEL_ID = "file_transfers"
        private const val NOTIFICATION_ID = 0x42554745
        private const val UPDATE_INTERVAL_MS = 200L
    }
}
