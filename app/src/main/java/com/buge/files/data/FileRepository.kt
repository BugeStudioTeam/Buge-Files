package com.buge.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

/**
 * Handles two genuinely usable sources: direct shared-storage file paths (file://) and
 * persisted Storage Access Framework document trees (content://).
 */
class FileRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    private fun isDirect(uri: Uri) = uri.scheme == ContentResolver.SCHEME_FILE
    private fun direct(uri: Uri): File? = uri.path?.let(::File)?.takeIf { isDirect(uri) && it.exists() }
    private fun documentTree(uri: Uri): DocumentFile? = DocumentFile.fromTreeUri(context, uri)?.takeIf { it.exists() && it.isDirectory }
    private fun document(uri: Uri): DocumentFile? = DocumentFile.fromSingleUri(context, uri)?.takeIf { it.exists() }

    suspend fun list(directoryUri: Uri, sort: SortOption, ascending: Boolean, showHidden: Boolean): List<FileEntry> = withContext(Dispatchers.IO) {
        val items = when {
            isDirect(directoryUri) -> direct(directoryUri)?.listFiles().orEmpty().asSequence()
                .filter { showHidden || !it.name.startsWith('.') }
                .map(::toEntry)
                .toList()
            else -> documentTree(directoryUri)?.listFiles().orEmpty().asSequence()
                .filter { showHidden || !it.name.orEmpty().startsWith('.') }
                .map(::toEntry)
                .toList()
        }
        items.sortedWith(entryComparator(sort, ascending))
    }

    suspend fun search(rootUri: Uri, query: String, showHidden: Boolean): List<FileEntry> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val needle = query.trim().lowercase(Locale.ROOT)
        val results = mutableListOf<FileEntry>()
        if (isDirect(rootUri)) {
            val root = direct(rootUri) ?: return@withContext emptyList()
            val queue = ArrayDeque<File>(); queue += root
            var scanned = 0
            while (queue.isNotEmpty() && scanned < 20_000 && results.size < 500) {
                val current = queue.removeFirst()
                current.listFiles().orEmpty().forEach { child ->
                    scanned++
                    if (!showHidden && child.name.startsWith('.')) return@forEach
                    if (child.name.lowercase(Locale.ROOT).contains(needle)) results += toEntry(child)
                    if (child.isDirectory) queue += child
                }
            }
        } else {
            val root = documentTree(rootUri) ?: return@withContext emptyList()
            val queue = ArrayDeque<DocumentFile>(); queue += root
            var scanned = 0
            while (queue.isNotEmpty() && scanned < 20_000 && results.size < 500) {
                val current = queue.removeFirst()
                current.listFiles().forEach { child ->
                    scanned++
                    if (!showHidden && child.name.orEmpty().startsWith('.')) return@forEach
                    if (child.name.orEmpty().lowercase(Locale.ROOT).contains(needle)) results += toEntry(child)
                    if (child.isDirectory) queue += child
                }
            }
        }
        results.sortedWith(entryComparator(SortOption.NAME, true))
    }

    suspend fun calculateStorage(rootUri: Uri, showHidden: Boolean): StorageBreakdown = withContext(Dispatchers.IO) {
        var documents = 0L; var images = 0L; var videos = 0L; var audio = 0L; var archives = 0L; var other = 0L
        var files = 0; var folders = 0; var scanned = 0
        fun count(entry: FileEntry) {
            files++
            when (category(entry)) {
                "image" -> images += entry.size
                "video" -> videos += entry.size
                "audio" -> audio += entry.size
                "archive" -> archives += entry.size
                "document" -> documents += entry.size
                else -> other += entry.size
            }
        }
        if (isDirect(rootUri)) {
            val root = direct(rootUri) ?: return@withContext StorageBreakdown()
            val queue = ArrayDeque<File>(); queue += root
            while (queue.isNotEmpty() && scanned < 40_000) {
                val current = queue.removeFirst(); folders++
                current.listFiles().orEmpty().forEach { child ->
                    scanned++
                    if (!showHidden && child.name.startsWith('.')) return@forEach
                    if (child.isDirectory) queue += child else count(toEntry(child))
                }
            }
        } else {
            val root = documentTree(rootUri) ?: return@withContext StorageBreakdown()
            val queue = ArrayDeque<DocumentFile>(); queue += root
            while (queue.isNotEmpty() && scanned < 40_000) {
                val current = queue.removeFirst(); folders++
                current.listFiles().forEach { child ->
                    scanned++
                    if (!showHidden && child.name.orEmpty().startsWith('.')) return@forEach
                    if (child.isDirectory) queue += child else count(toEntry(child))
                }
            }
        }
        StorageBreakdown(documents, images, videos, audio, archives, other, files, (folders - 1).coerceAtLeast(0))
    }

    suspend fun createFolder(parentUri: Uri, name: String): OperationResult = withContext(Dispatchers.IO) {
        if (!validName(name)) return@withContext OperationResult(false, "Enter a valid folder name")
        if (isDirect(parentUri)) {
            val parent = direct(parentUri) ?: return@withContext OperationResult(false, "Storage location is unavailable")
            val target = File(parent, name)
            if (target.exists()) OperationResult(false, "An item with that name already exists")
            else if (target.mkdir()) OperationResult(true, "Folder created") else OperationResult(false, "Could not create folder")
        } else {
            val parent = documentTree(parentUri) ?: return@withContext OperationResult(false, "Folder access is no longer available")
            if (parent.findFile(name) != null) OperationResult(false, "An item with that name already exists")
            else if (parent.createDirectory(name) != null) OperationResult(true, "Folder created") else OperationResult(false, "Could not create folder")
        }
    }

    suspend fun createFile(parentUri: Uri, name: String): OperationResult = withContext(Dispatchers.IO) {
        if (!validName(name)) return@withContext OperationResult(false, "Enter a valid file name")
        if (isDirect(parentUri)) {
            val parent = direct(parentUri) ?: return@withContext OperationResult(false, "Storage location is unavailable")
            val target = File(parent, name)
            when {
                target.exists() -> OperationResult(false, "An item with that name already exists")
                target.createNewFile() -> OperationResult(true, "File created")
                else -> OperationResult(false, "Could not create file")
            }
        } else {
            val parent = documentTree(parentUri) ?: return@withContext OperationResult(false, "Folder access is no longer available")
            when {
                parent.findFile(name) != null -> OperationResult(false, "An item with that name already exists")
                parent.createFile(mimeForName(name), name) != null -> OperationResult(true, "File created")
                else -> OperationResult(false, "Could not create file")
            }
        }
    }

    suspend fun rename(entry: FileEntry, newName: String): OperationResult = withContext(Dispatchers.IO) {
        if (!validName(newName)) return@withContext OperationResult(false, "Enter a valid name")
        if (isDirect(entry.uri)) {
            val source = direct(entry.uri) ?: return@withContext OperationResult(false, "File is not available")
            val target = File(source.parentFile, newName)
            when {
                target.exists() -> OperationResult(false, "An item with that name already exists")
                source.renameTo(target) -> OperationResult(true, "Renamed")
                else -> OperationResult(false, "Could not rename item")
            }
        } else {
            val source = document(entry.uri) ?: return@withContext OperationResult(false, "File is not available")
            if (source.renameTo(newName)) OperationResult(true, "Renamed") else OperationResult(false, "Could not rename item")
        }
    }

    suspend fun delete(entries: List<FileEntry>): OperationResult = withContext(Dispatchers.IO) {
        var failed = 0
        entries.forEach { entry ->
            val deleted = if (isDirect(entry.uri)) direct(entry.uri)?.deleteRecursively() == true else document(entry.uri)?.delete() == true
            if (!deleted) failed++
        }
        operationSummary(entries.size, failed, "deleted")
    }

    suspend fun paste(clipboard: ClipboardState, destinationUri: Uri, onProgress: ((Long, Long, Int, Int) -> Unit)? = null): OperationResult = withContext(Dispatchers.IO) {
        var completed = 0
        var failed = 0
        val total = clipboard.entries.size
        var bytesDone = 0L
        var bytesTotal = 0L
        clipboard.entries.forEach { entry -> bytesTotal += entrySize(entry.uri) }
        val tracker = ProgressTracker(bytesTotal)
        fun progress() = onProgress?.invoke(bytesDone + tracker.done, bytesTotal, completed + failed, total)
        clipboard.entries.forEach { entry ->
            tracker.reset()
            val copied = if (isDirect(destinationUri)) {
                val destination = direct(destinationUri)
                when {
                    destination == null || !destination.isDirectory || sourceWouldContainDestination(entry.uri, destination) -> false
                    isDirect(entry.uri) -> direct(entry.uri)?.let { copyDirectToDirect(it, destination, tracker, ::progress) != null } ?: false
                    else -> document(entry.uri)?.let { copyDocumentToDirect(it, destination, tracker, ::progress) != null } ?: false
                }
            } else {
                val destination = documentTree(destinationUri)
                when {
                    destination == null || !destination.canWrite() || entry.uri == destination.uri -> false
                    isDirect(entry.uri) -> direct(entry.uri)?.let { copyDirectToDocument(it, destination, tracker, ::progress) != null } ?: false
                    else -> document(entry.uri)?.let { copyDocumentToDocument(it, destination, tracker, ::progress) != null } ?: false
                }
            }
            if (copied) {
                completed++
                if (clipboard.mode == ClipboardMode.MOVE && !deleteSource(entry.uri)) failed++
            } else failed++
            bytesDone += tracker.done
            progress()
        }
        when {
            completed == 0 -> OperationResult(false, "No items were pasted")
            failed == 0 -> OperationResult(true, if (clipboard.mode == ClipboardMode.MOVE) "Moved $completed item(s)" else "Copied $completed item(s)")
            else -> OperationResult(false, "Completed $completed item(s); $failed failed")
        }
    }

    suspend fun saveExternal(uris: List<Uri>, destinationUri: Uri, onProgress: ((Long, Long, Int, Int) -> Unit)? = null): OperationResult = withContext(Dispatchers.IO) {
        var completed = 0
        var failed = 0
        val total = uris.size
        var bytesDone = 0L
        var bytesTotal = 0L
        uris.forEach { source -> bytesTotal += externalSize(source) }
        val tracker = ProgressTracker(bytesTotal)
        fun progress() = onProgress?.invoke(bytesDone + tracker.done, bytesTotal, completed + failed, total)
        uris.forEach { source ->
            tracker.reset()
            val copied = runCatching {
                val name = externalName(source)
                val input = resolver.openInputStream(source) ?: return@runCatching false
                input.use { stream ->
                    if (isDirect(destinationUri)) {
                        val destination = direct(destinationUri) ?: return@runCatching false
                        if (!destination.isDirectory) return@runCatching false
                        val target = File(destination, uniqueDirectName(destination, name, false))
                        FileOutputStream(target).use { output -> copyStream(stream, output, tracker, ::progress) }
                        target.exists() && target.length() > 0
                    } else {
                        val destination = documentTree(destinationUri) ?: return@runCatching false
                        if (!destination.canWrite()) return@runCatching false
                        val target = destination.createFile(mimeForName(name), uniqueDocumentName(destination, name, false)) ?: return@runCatching false
                        resolver.openOutputStream(target.uri, "w")?.use { output -> copyStream(stream, output, tracker, ::progress) } ?: return@runCatching false
                        target.length() > 0
                    }
                }
            }.getOrDefault(false)
            if (copied) completed++ else failed++
            bytesDone += tracker.done
            progress()
        }
        when {
            completed == 0 -> OperationResult(false, "No items were saved")
            failed == 0 -> OperationResult(true, "Saved $completed item(s)")
            else -> OperationResult(false, "Saved $completed item(s); $failed failed")
        }
    }

    private fun externalSize(uri: Uri): Long {
        if (isDirect(uri)) return direct(uri)?.length() ?: 0L
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    private fun entrySize(uri: Uri): Long {
        if (isDirect(uri)) return direct(uri)?.let(::directSize) ?: 0L
        return directSizeDocument(document(uri))
    }

    private fun directSize(file: File): Long {
        if (!file.isDirectory) return file.length()
        var total = 0L
        val queue = ArrayDeque<File>(); queue += file
        var scanned = 0
        while (queue.isNotEmpty() && scanned < 40_000) {
            queue.removeFirst().listFiles().orEmpty().forEach { child ->
                scanned++
                if (child.isDirectory) queue += child else total += child.length()
            }
        }
        return total
    }

    private fun directSizeDocument(file: DocumentFile?): Long {
        if (file == null) return 0L
        if (!file.isDirectory) return file.length()
        var total = 0L
        val queue = ArrayDeque<DocumentFile>(); queue += file
        var scanned = 0
        while (queue.isNotEmpty() && scanned < 40_000) {
            queue.removeFirst().listFiles().forEach { child ->
                scanned++
                if (child.isDirectory) queue += child else total += child.length()
            }
        }
        return total
    }

    private fun copyStream(input: java.io.InputStream, output: java.io.OutputStream, tracker: ProgressTracker, progress: () -> Unit) {
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            output.write(buffer, 0, read)
            tracker.add(read.toLong())
            progress()
        }
        output.flush()
    }

    private class ProgressTracker(private val limit: Long) {
        var done: Long = 0L
            private set

        fun reset() { done = 0L }

        fun add(delta: Long) {
            done += delta
            if (limit > 0L && done > limit) done = limit
        }
    }

    private fun externalName(uri: Uri): String {
        if (isDirect(uri)) return direct(uri)?.name ?: "shared_file"
        val displayName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        val fallback = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        return displayName?.takeIf { it.isNotBlank() } ?: fallback ?: "shared_file"
    }

    private fun copyDirectToDirect(source: File, destination: File, tracker: ProgressTracker, progress: () -> Unit): File? {
        val target = File(destination, uniqueDirectName(destination, source.name, source.isDirectory))
        return try {
            if (source.isDirectory) {
                if (!target.mkdirs()) return null
                source.listFiles().orEmpty().forEach { child -> if (copyDirectToDirect(child, target, tracker, progress) == null) return null }
            } else {
                FileInputStream(source).use { input -> FileOutputStream(target).use { output -> copyStream(input, output, tracker, progress) } }
            }
            target
        } catch (_: Exception) { target.deleteRecursively(); null }
    }

    private fun copyDocumentToDirect(source: DocumentFile, destination: File, tracker: ProgressTracker, progress: () -> Unit): File? {
        val target = File(destination, uniqueDirectName(destination, source.name ?: "Untitled", source.isDirectory))
        return try {
            if (source.isDirectory) {
                if (!target.mkdirs()) return null
                source.listFiles().forEach { child -> if (copyDocumentToDirect(child, target, tracker, progress) == null) return null }
            } else {
                resolver.openInputStream(source.uri).use { input ->
                    if (input == null) return null
                    FileOutputStream(target).use { output -> BufferedInputStream(input).use { bufferedInput -> BufferedOutputStream(output).use { bufferedOutput -> copyStream(bufferedInput, bufferedOutput, tracker, progress) } } }
                }
            }
            target
        } catch (_: Exception) { target.deleteRecursively(); null }
    }

    private fun copyDirectToDocument(source: File, destination: DocumentFile, tracker: ProgressTracker, progress: () -> Unit): DocumentFile? {
        val name = uniqueDocumentName(destination, source.name, source.isDirectory)
        return try {
            if (source.isDirectory) {
                val target = destination.createDirectory(name) ?: return null
                source.listFiles().orEmpty().forEach { child -> if (copyDirectToDocument(child, target, tracker, progress) == null) return null }
                target
            } else {
                val target = destination.createFile(mimeForName(source.name), name) ?: return null
                FileInputStream(source).use { input -> resolver.openOutputStream(target.uri, "w").use { output -> if (output == null) return null; copyStream(input, output, tracker, progress) } }
                target
            }
        } catch (_: Exception) { null }
    }

    private fun copyDocumentToDocument(source: DocumentFile, destination: DocumentFile, tracker: ProgressTracker, progress: () -> Unit): DocumentFile? {
        val name = uniqueDocumentName(destination, source.name ?: "Untitled", source.isDirectory)
        return try {
            if (source.isDirectory) {
                val target = destination.createDirectory(name) ?: return null
                source.listFiles().forEach { child -> if (copyDocumentToDocument(child, target, tracker, progress) == null) return null }
                target
            } else {
                val target = destination.createFile(source.type ?: mimeForName(source.name.orEmpty()), name) ?: return null
                resolver.openInputStream(source.uri).use { input -> resolver.openOutputStream(target.uri, "w").use { output -> if (input == null || output == null) return null; BufferedInputStream(input).use { bufferedInput -> BufferedOutputStream(output).use { bufferedOutput -> copyStream(bufferedInput, bufferedOutput, tracker, progress) } } } }
                target
            }
        } catch (_: Exception) { null }
    }

    private fun deleteSource(uri: Uri): Boolean = if (isDirect(uri)) direct(uri)?.deleteRecursively() == true else document(uri)?.delete() == true

    private fun sourceWouldContainDestination(sourceUri: Uri, destination: File): Boolean {
        if (!isDirect(sourceUri)) return false
        val source = direct(sourceUri) ?: return true
        return source.isDirectory && runCatching { destination.canonicalPath.startsWith(source.canonicalPath + File.separator) }.getOrDefault(true)
    }

    private fun validName(name: String) = name.isNotBlank() && !name.contains('/') && !name.contains(File.separator)
    private fun uniqueDirectName(parent: File, requested: String, directory: Boolean): String {
        if (!File(parent, requested).exists()) return requested
        val stem = if (directory) requested else requested.substringBeforeLast('.', requested)
        val extension = if (directory || !requested.contains('.')) "" else ".${requested.substringAfterLast('.')}"
        var index = 1
        while (File(parent, "$stem ($index)$extension").exists()) index++
        return "$stem ($index)$extension"
    }
    private fun uniqueDocumentName(parent: DocumentFile, requested: String, directory: Boolean): String {
        if (parent.findFile(requested) == null) return requested
        val stem = if (directory) requested else requested.substringBeforeLast('.', requested)
        val extension = if (directory || !requested.contains('.')) "" else ".${requested.substringAfterLast('.')}"
        var index = 1
        while (parent.findFile("$stem ($index)$extension") != null) index++
        return "$stem ($index)$extension"
    }

    private fun toEntry(file: File) = FileEntry(Uri.fromFile(file), file.name, if (file.isDirectory) null else mimeForName(file.name), file.isDirectory, if (file.isFile) file.length().coerceAtLeast(0) else 0, file.lastModified().coerceAtLeast(0), if (file.isDirectory) file.listFiles()?.size ?: 0 else 0)
    private fun toEntry(file: DocumentFile) = FileEntry(file.uri, file.name ?: "Untitled", file.type, file.isDirectory, file.length().coerceAtLeast(0), file.lastModified().coerceAtLeast(0), if (file.isDirectory) file.listFiles().size else 0)

    private fun entryComparator(sort: SortOption, ascending: Boolean): Comparator<FileEntry> {
        val comparator = when (sort) {
            SortOption.NAME -> compareBy<FileEntry> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
            SortOption.DATE -> compareBy<FileEntry> { !it.isDirectory }.thenByDescending { it.lastModified }
            SortOption.SIZE -> compareBy<FileEntry> { !it.isDirectory }.thenByDescending { it.size }
            SortOption.TYPE -> compareBy<FileEntry> { !it.isDirectory }.thenBy { it.extension }.thenBy { it.name.lowercase(Locale.ROOT) }
        }
        return if (ascending) comparator else comparator.reversed()
    }

    private fun category(file: FileEntry): String {
        val type = file.mimeType.orEmpty(); val ext = file.extension
        return when {
            type.startsWith("image/") -> "image"
            type.startsWith("video/") -> "video"
            type.startsWith("audio/") -> "audio"
            ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> "archive"
            type.startsWith("text/") || type.contains("pdf") || ext in setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "epub", "md", "json", "csv", "xml") -> "document"
            else -> "other"
        }
    }

    private fun mimeForName(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (extension) {
            "md", "log" -> "text/plain"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }

    private fun operationSummary(total: Int, failed: Int, verb: String): OperationResult = when {
        total == 0 -> OperationResult(false, "Nothing selected")
        failed == 0 -> OperationResult(true, if (total == 1) "Item $verb" else "$total items $verb")
        failed == total -> OperationResult(false, "Could not $verb selected items")
        else -> OperationResult(false, "${total - failed} item(s) $verb; $failed failed")
    }

    companion object {
        private const val COPY_BUFFER_SIZE = 128 * 1024
    }
}
