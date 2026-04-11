package com.buge.files.model

import com.buge.files.R
import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase()
) {
    val isHidden: Boolean get() = name.startsWith(".")

    fun formattedSize(): String {
        if (isDirectory) return ""
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
            size < 1024 * 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024))
            else -> "%.2f GB".format(size / (1024.0 * 1024 * 1024))
        }
    }

    fun mimeType(): String = when (extension) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> "image/*"
        "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv" -> "video/*"
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "opus" -> "audio/*"
        "pdf" -> "application/pdf"
        "txt", "md", "log", "csv", "json", "xml" -> "text/plain"
        "html", "htm" -> "text/html"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        "tar" -> "application/x-tar"
        "gz" -> "application/gzip"
        "apk" -> "application/vnd.android.package-archive"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "*/*"
    }

    fun iconRes(): Int = when {
        isDirectory -> R.drawable.ic_folder
        extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic") ->
            R.drawable.ic_file_image
        extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv") ->
            R.drawable.ic_file_video
        extension in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus") ->
            R.drawable.ic_file_audio
        extension == "pdf" -> R.drawable.ic_file_pdf
        extension in listOf("zip", "rar", "7z", "tar", "gz") -> R.drawable.ic_file_archive
        extension == "apk" -> R.drawable.ic_file_apk
        extension in listOf("txt", "md", "log", "json", "xml", "html", "csv") ->
            R.drawable.ic_file_text
        else -> R.drawable.ic_file
    }

    // 图标背景色（用于 icon_bg_shape 的 tint）
    fun iconBgColor(): String = when {
        isDirectory -> "#1A0077B6"  // 蓝色半透明
        extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> "#1AFF9800"
        extension in listOf("mp4", "mkv", "avi", "mov", "webm") -> "#1AF44336"
        extension in listOf("mp3", "wav", "flac", "aac", "ogg") -> "#1A9C27B0"
        extension == "pdf" -> "#1AF44336"
        extension in listOf("zip", "rar", "7z") -> "#1A795548"
        extension == "apk" -> "#1A4CAF50"
        else -> "#1A607D8B"
    }
}