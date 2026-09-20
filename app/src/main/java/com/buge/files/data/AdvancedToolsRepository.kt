package com.buge.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val MAX_EDITOR_BYTES = 2 * 1024 * 1024
private const val MAX_ARCHIVE_ENTRIES = 10_000

data class TextLoadResult(val text: String? = null, val message: String? = null)
data class ArchiveItem(val name: String, val size: Long, val compressedSize: Long, val isDirectory: Boolean)
data class ArchiveReadResult(val items: List<ArchiveItem> = emptyList(), val message: String? = null)

/** Implements tool actions for direct file paths and persisted SAF content URIs. */
class AdvancedToolsRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun readText(uri: Uri): TextLoadResult = withContext(Dispatchers.IO) {
        try {
            input(uri)?.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val count = stream.read(buffer)
                    if (count <= 0) break
                    total += count
                    if (total > MAX_EDITOR_BYTES) return@withContext TextLoadResult(message = "Files larger than 2 MB are not opened in the editor")
                    output.write(buffer, 0, count)
                }
                TextLoadResult(text = output.toString(StandardCharsets.UTF_8.name()))
            } ?: TextLoadResult(message = "File is no longer available")
        } catch (error: Exception) {
            TextLoadResult(message = "Could not read file: ${error.message.orEmpty().take(80)}")
        }
    }

    suspend fun saveText(uri: Uri, content: String): OperationResult = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == ContentResolver.SCHEME_FILE) {
                val file = uri.path?.let(::File) ?: return@withContext OperationResult(false, "File is no longer available")
                val temporary = File(file.parentFile, ".${file.name}.buge-tmp")
                FileOutputStream(temporary).bufferedWriter(StandardCharsets.UTF_8).use { it.write(content) }
                if (!temporary.renameTo(file)) {
                    temporary.delete()
                    return@withContext OperationResult(false, "Could not save changes")
                }
            } else {
                resolver.openOutputStream(uri, "wt")?.bufferedWriter(StandardCharsets.UTF_8)?.use { it.write(content) }
                    ?: return@withContext OperationResult(false, "This file cannot be written")
            }
            OperationResult(true, "Changes saved")
        } catch (error: Exception) {
            OperationResult(false, "Could not save: ${error.message.orEmpty().take(80)}")
        }
    }

    suspend fun listZip(uri: Uri): ArchiveReadResult = withContext(Dispatchers.IO) {
        try {
            val stream = input(uri) ?: return@withContext ArchiveReadResult(message = "Archive is no longer available")
            val items = mutableListOf<ArchiveItem>()
            ZipInputStream(BufferedInputStream(stream)).use { zip ->
                while (items.size < MAX_ARCHIVE_ENTRIES) {
                    val entry = zip.nextEntry ?: break
                    items += ArchiveItem(entry.name, entry.size.coerceAtLeast(0), entry.compressedSize.coerceAtLeast(0), entry.isDirectory)
                    zip.closeEntry()
                }
            }
            ArchiveReadResult(items = items.sortedWith(compareBy<ArchiveItem> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }))
        } catch (error: Exception) {
            ArchiveReadResult(message = "Could not read ZIP archive: ${error.message.orEmpty().take(90)}")
        }
    }

    /** Extracts ZIP/APK/JAR/EPUB containers and blocks directory-traversal (Zip Slip) entry names. */
    suspend fun extractZip(uri: Uri, destinationUri: Uri): OperationResult = withContext(Dispatchers.IO) {
        val stream = input(uri) ?: return@withContext OperationResult(false, "Archive is no longer available")
        try {
            var extracted = 0
            ZipInputStream(BufferedInputStream(stream)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!safeArchivePath(entry.name)) return@withContext OperationResult(false, "Unsafe archive path was blocked")
                    val result = if (destinationUri.scheme == ContentResolver.SCHEME_FILE) {
                        extractToDirect(zip, entry.name, entry.isDirectory, destinationUri)
                    } else {
                        extractToDocument(zip, entry.name, entry.isDirectory, destinationUri)
                    }
                    if (!result) return@withContext OperationResult(false, "Could not extract ${entry.name}")
                    if (!entry.isDirectory) extracted++
                    zip.closeEntry()
                }
            }
            OperationResult(true, "Extracted $extracted file(s)")
        } catch (error: Exception) {
            OperationResult(false, "Extraction failed: ${error.message.orEmpty().take(90)}")
        }
    }

    /** Compresses the explicitly selected entries into one ZIP in the current direct-storage or SAF folder. */
    suspend fun compressToZip(entries: List<FileEntry>, destinationUri: Uri, requestedName: String): OperationResult = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext OperationResult(false, "Nothing selected")
        val zipName = requestedName.trim().let { if (it.lowercase(Locale.ROOT).endsWith(".zip")) it else "$it.zip" }
        if (zipName.isBlank() || zipName.contains('/') || zipName.contains(File.separator)) return@withContext OperationResult(false, "Enter a valid archive name")
        var directOutput: File? = null
        var documentOutput: DocumentFile? = null
        try {
            val output: OutputStream = if (destinationUri.scheme == ContentResolver.SCHEME_FILE) {
                val parent = destinationUri.path?.let(::File)?.takeIf { it.exists() && it.isDirectory }
                    ?: return@withContext OperationResult(false, "Current folder is unavailable")
                val target = File(parent, zipName)
                if (target.exists()) return@withContext OperationResult(false, "An item with that name already exists")
                directOutput = target
                FileOutputStream(target)
            } else {
                val parent = documentDirectory(destinationUri) ?: return@withContext OperationResult(false, "Current folder is unavailable")
                if (parent.findFile(zipName) != null) return@withContext OperationResult(false, "An item with that name already exists")
                val target = parent.createFile("application/zip", zipName)
                    ?: return@withContext OperationResult(false, "Could not create archive")
                documentOutput = target
                resolver.openOutputStream(target.uri, "w") ?: run {
                    target.delete()
                    return@withContext OperationResult(false, "Could not write archive")
                }
            }
            var packed = 0
            val usedRoots = mutableSetOf<String>()
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                entries.forEach { entry ->
                    val root = uniqueArchiveRoot(entry.name, usedRoots)
                    packed += addToZip(entry, root, zip)
                }
            }
            OperationResult(true, "Compressed $packed file(s) into $zipName")
        } catch (error: Exception) {
            directOutput?.delete()
            documentOutput?.delete()
            OperationResult(false, "Compression failed: ${error.message.orEmpty().take(90)}")
        }
    }

    suspend fun sha256(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            input(uri)?.use { stream ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val count = stream.read(buffer)
                    if (count <= 0) break
                    digest.update(buffer, 0, count)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) { null }
    }

    private fun input(uri: Uri): InputStream? = if (uri.scheme == ContentResolver.SCHEME_FILE) {
        uri.path?.let(::File)?.takeIf { it.exists() }?.let(::FileInputStream)
    } else resolver.openInputStream(uri)

    private fun documentDirectory(uri: Uri): DocumentFile? = (DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri))
        ?.takeIf { it.exists() && it.isDirectory && it.canWrite() }

    private fun children(entry: FileEntry): List<FileEntry> = if (entry.uri.scheme == ContentResolver.SCHEME_FILE) {
        entry.uri.path?.let(::File)?.takeIf { it.exists() && it.isDirectory }?.listFiles().orEmpty().map { file ->
            FileEntry(Uri.fromFile(file), file.name, if (file.isDirectory) null else mimeForName(file.name), file.isDirectory, if (file.isFile) file.length() else 0L, file.lastModified(), if (file.isDirectory) file.listFiles()?.size ?: 0 else 0)
        }
    } else {
        (DocumentFile.fromSingleUri(context, entry.uri) ?: DocumentFile.fromTreeUri(context, entry.uri))?.takeIf { it.isDirectory }?.listFiles().orEmpty().map { file ->
            FileEntry(file.uri, file.name ?: "Untitled", file.type, file.isDirectory, file.length(), file.lastModified(), if (file.isDirectory) file.listFiles().size else 0)
        }
    }

    private fun addToZip(entry: FileEntry, archivePath: String, zip: ZipOutputStream): Int {
        val normalized = archivePath.trim('/').replace('\\', '/')
        if (normalized.isBlank() || normalized.split('/').any { it == ".." }) throw IllegalArgumentException("Unsafe file name")
        return if (entry.isDirectory) {
            zip.putNextEntry(ZipEntry("$normalized/"))
            zip.closeEntry()
            children(entry).sumOf { child -> addToZip(child, "$normalized/${child.name}", zip) }
        } else {
            zip.putNextEntry(ZipEntry(normalized))
            input(entry.uri)?.use { stream -> BufferedInputStream(stream).use { it.copyTo(zip) } }
                ?: throw IllegalStateException("Could not read ${entry.name}")
            zip.closeEntry()
            1
        }
    }

    private fun uniqueArchiveRoot(requested: String, used: MutableSet<String>): String {
        val clean = requested.replace('/', '_').replace('\\', '_').ifBlank { "Untitled" }
        if (used.add(clean)) return clean
        val stem = clean.substringBeforeLast('.', clean)
        val extension = if (clean.contains('.')) ".${clean.substringAfterLast('.')}" else ""
        var index = 1
        while (!used.add("$stem ($index)$extension")) index++
        return "$stem ($index)$extension"
    }

    private fun extractToDirect(zip: ZipInputStream, name: String, directory: Boolean, destinationUri: Uri): Boolean {
        val root = destinationUri.path?.let(::File)?.canonicalFile ?: return false
        val target = File(root, name).canonicalFile
        if (target.path != root.path && !target.path.startsWith(root.path + File.separator)) return false
        return if (directory) target.exists() || target.mkdirs() else {
            target.parentFile?.mkdirs()
            FileOutputStream(target).use { output -> BufferedOutputStream(output).use { bufferedOutput -> zip.copyTo(bufferedOutput) } }
            true
        }
    }

    private fun extractToDocument(zip: ZipInputStream, name: String, directory: Boolean, destinationUri: Uri): Boolean {
        val root = DocumentFile.fromTreeUri(context, destinationUri)?.takeIf { it.exists() && it.isDirectory } ?: return false
        val parts = name.trimEnd('/').split('/').filter { it.isNotBlank() }
        if (parts.isEmpty()) return true
        var parent = root
        val folders = if (directory) parts else parts.dropLast(1)
        folders.forEach { part -> parent = parent.findFile(part)?.takeIf { it.isDirectory } ?: parent.createDirectory(part) ?: return false }
        if (directory) return true
        val requested = parts.last()
        parent.findFile(requested)?.delete()
        val output = parent.createFile(mimeForName(requested), requested) ?: return false
        resolver.openOutputStream(output.uri, "w")?.use { stream -> BufferedOutputStream(stream).use { bufferedOutput -> zip.copyTo(bufferedOutput) } } ?: return false
        return true
    }

    private fun safeArchivePath(name: String): Boolean {
        val normalized = name.replace('\\', '/')
        return normalized.isNotBlank() && !normalized.startsWith('/') && !normalized.split('/').any { it == ".." }
    }

    private fun mimeForName(name: String): String = when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "txt", "md", "log", "csv", "xml", "yaml", "yml", "kt", "java", "py", "js", "ts", "json" -> "text/plain"
        else -> "application/octet-stream"
    }
}

fun FileEntry.isImageFile(): Boolean = mimeType.orEmpty().startsWith("image/") || extension in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
fun FileEntry.isZipContainer(): Boolean = extension in setOf("zip", "apk", "jar", "epub", "cbz")
fun FileEntry.isEditableText(): Boolean = mimeType.orEmpty().startsWith("text/") || extension in setOf("kt", "kts", "java", "xml", "json", "yaml", "yml", "properties", "gradle", "md", "txt", "csv", "html", "css", "js", "ts", "tsx", "jsx", "py", "sh", "sql", "c", "cpp", "h", "hpp", "go", "rs", "toml", "ini", "log")
