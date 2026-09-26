package com.buge.files

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.Locale
import java.util.concurrent.TimeUnit

object SmbUri {
    const val SCHEME = "smb"

    fun isSmb(uri: Uri): Boolean = uri.scheme == SCHEME

    fun build(host: String, share: String, path: String, port: Int = 445): Uri {
        val normalized = path.trim('/').replace('\\', '/')
        val scheme = SCHEME
        val authority = if (port != 445) "$host:$port" else host
        val builder = Uri.Builder().scheme(scheme).authority(authority)
        builder.appendPath(share)
        if (normalized.isNotEmpty()) normalized.split('/').forEach { builder.appendPath(it) }
        return builder.build()
    }

    fun host(uri: Uri): String = uri.host.orEmpty()

    fun port(uri: Uri): Int = if (uri.port > 0) uri.port else 445

    fun pathSegments(uri: Uri): List<String> = uri.pathSegments.orEmpty().filter { it.isNotBlank() }

    fun share(uri: Uri): String = pathSegments(uri).firstOrNull().orEmpty()

    fun relativePath(uri: Uri): String = pathSegments(uri).drop(1).joinToString("/")
}

class SmbRepository(private val context: Context) {
    private val store = SmbConnectionStore(context)
    private val client = SMBClient(
        SmbConfig.builder()
            .withTimeout(15, TimeUnit.SECONDS)
            .withSoTimeout(30, TimeUnit.SECONDS)
            .build()
    )
    private val sessions = mutableMapOf<String, Pair<Connection, Session>>()

    private fun connectionKey(credentials: SmbCredentials) = "${credentials.host}:${credentials.port}/${credentials.share}"

    private fun shareFor(uri: Uri): DiskShare {
        val host = SmbUri.host(uri)
        val shareName = SmbUri.share(uri)
        val credentials = store.load(host, shareName)
            ?: throw IllegalStateException("No stored credentials for $host/$shareName")
        val key = connectionKey(credentials)
        sessions[key]?.let { (_, session) ->
            if (session.connection.isConnected) return (session.connectShare(shareName) as? DiskShare)
                ?: throw IllegalStateException("Share $shareName is not reachable")
        }
        val connection = client.connect(host, credentials.port)
        val auth = if (credentials.domain.isBlank()) {
            AuthenticationContext(credentials.username, credentials.password.toCharArray(), null)
        } else {
            AuthenticationContext(credentials.username, credentials.password.toCharArray(), credentials.domain)
        }
        val session = connection.authenticate(auth)
        sessions[key] = connection to session
        return (session.connectShare(shareName) as? DiskShare)
            ?: throw IllegalStateException("Share $shareName is not reachable")
    }

    private fun invalidate(credentials: SmbCredentials) {
        sessions.remove(connectionKey(credentials))?.let { (connection, session) ->
            runCatching { session.close() }
            runCatching { connection.close() }
        }
    }

    private fun resolveCredentials(uri: Uri): SmbCredentials =
        store.load(SmbUri.host(uri), SmbUri.share(uri))
            ?: throw IllegalStateException("No stored credentials for this location")

    suspend fun testConnection(credentials: SmbCredentials): OperationResult = withContext(Dispatchers.IO) {
        try {
            val connection = client.connect(credentials.host, credentials.port)
            val auth = if (credentials.domain.isBlank()) {
                AuthenticationContext(credentials.username, credentials.password.toCharArray(), null)
            } else {
                AuthenticationContext(credentials.username, credentials.password.toCharArray(), credentials.domain)
            }
            val session = connection.authenticate(auth)
            val share = session.connectShare(credentials.share)
            runCatching { share.close() }
            runCatching { session.close() }
            runCatching { connection.close() }
            OperationResult(true, "Connected to ${credentials.displayLabel}")
        } catch (error: Exception) {
            OperationResult(false, "Connection failed: ${error.message.orEmpty().take(120)}")
        }
    }

    suspend fun list(directoryUri: Uri, sort: SortOption, ascending: Boolean, showHidden: Boolean): List<FileEntry> = withContext(Dispatchers.IO) {
        withShare(directoryUri) { share, credentials ->
            val relative = SmbUri.relativePath(directoryUri)
            val entries = share.list(relative).mapNotNull { item ->
                val name = item.fileName
                if (name == "." || name == "..") return@mapNotNull null
                if (!showHidden && name.startsWith('.')) return@mapNotNull null
                val isDirectory = item.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
                val childPath = if (relative.isBlank()) name else "$relative/$name"
                FileEntry(
                    uri = SmbUri.build(credentials.host, credentials.share, childPath, credentials.port),
                    name = name,
                    mimeType = if (isDirectory) null else mimeForName(name),
                    isDirectory = isDirectory,
                    size = if (isDirectory) 0L else item.endOfFile,
                    lastModified = item.lastWriteTime?.toEpochMillis() ?: 0L,
                    childCount = 0
                )
            }
            entries.sortedWith(entryComparator(sort, ascending))
        }
    }

    suspend fun createFolder(parentUri: Uri, name: String): OperationResult = withContext(Dispatchers.IO) {
        if (!validName(name)) return@withContext OperationResult(false, "Enter a valid folder name")
        try {
            withShare(parentUri) { share, credentials ->
                val relative = SmbUri.relativePath(parentUri)
                val target = if (relative.isBlank()) name else "$relative/$name"
                if (share.folderExists(target)) return@withShare OperationResult(false, "An item with that name already exists")
                share.mkdir(target)
                OperationResult(true, "Folder created")
            }
        } catch (error: Exception) {
            OperationResult(false, "Could not create folder: ${error.message.orEmpty().take(90)}")
        }
    }

    suspend fun createFile(parentUri: Uri, name: String): OperationResult = withContext(Dispatchers.IO) {
        if (!validName(name)) return@withContext OperationResult(false, "Enter a valid file name")
        try {
            withShare(parentUri) { share, credentials ->
                val relative = SmbUri.relativePath(parentUri)
                val target = if (relative.isBlank()) name else "$relative/$name"
                if (share.fileExists(target) || share.folderExists(target)) return@withShare OperationResult(false, "An item with that name already exists")
                share.openFile(
                    target,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_CREATE,
                    EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
                ).use { }
                OperationResult(true, "File created")
            }
        } catch (error: Exception) {
            OperationResult(false, "Could not create file: ${error.message.orEmpty().take(90)}")
        }
    }

    suspend fun rename(entry: FileEntry, newName: String): OperationResult = withContext(Dispatchers.IO) {
        if (!validName(newName)) return@withContext OperationResult(false, "Enter a valid name")
        try {
            withShare(entry.uri) { share, _ ->
                val relative = SmbUri.relativePath(entry.uri)
                val parent = relative.substringBeforeLast('/', "")
                val target = if (parent.isBlank()) newName else "$parent/$newName"
                if (share.fileExists(target) || share.folderExists(target)) return@withShare OperationResult(false, "An item with that name already exists")
                share.openFile(
                    relative,
                    EnumSet.of(AccessMask.GENERIC_ALL),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                ).use { it.rename(target) }
                OperationResult(true, "Renamed")
            }
        } catch (error: Exception) {
            OperationResult(false, "Could not rename item: ${error.message.orEmpty().take(90)}")
        }
    }

    suspend fun delete(entries: List<FileEntry>): OperationResult = withContext(Dispatchers.IO) {
        var failed = 0
        entries.forEach { entry ->
            val deleted = runCatching {
                withShare(entry.uri) { share, _ ->
                    val relative = SmbUri.relativePath(entry.uri)
                    if (entry.isDirectory) deleteTree(share, relative) else share.rm(relative)
                }
            }.isSuccess
            if (!deleted) failed++
        }
        operationSummary(entries.size, failed, "deleted")
    }

    private fun deleteTree(share: DiskShare, path: String) {
        share.list(path).forEach { item ->
            val name = item.fileName
            if (name == "." || name == "..") return@forEach
            val child = if (path.isBlank()) name else "$path/$name"
            val isDirectory = item.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
            if (isDirectory) deleteTree(share, child) else share.rm(child)
        }
        share.rmdir(path, true)
    }

    suspend fun openInputStream(uri: Uri): InputStream? = withContext(Dispatchers.IO) {
        runCatching {
            withShare(uri) { share, _ ->
                share.openFile(
                    SmbUri.relativePath(uri),
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
                ).inputStream
            }
        }.getOrNull()
    }

    suspend fun copyToLocal(uri: Uri, destination: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val input = openInputStream(uri) ?: return@withContext false
            input.use { stream ->
                destination.outputStream().use { output -> stream.copyTo(output) }
            }
            true
        }.getOrDefault(false)
    }

    suspend fun paste(clipboard: ClipboardState, destinationUri: Uri): OperationResult = withContext(Dispatchers.IO) {
        var completed = 0
        var failed = 0
        val destinationIsSmb = SmbUri.isSmb(destinationUri)
        clipboard.entries.forEach { entry ->
            val copied = runCatching {
                if (destinationIsSmb) {
                    copyIntoSmb(entry, destinationUri)
                } else {
                    copyFromSmb(entry, destinationUri)
                }
            }.getOrDefault(false)
            if (copied) {
                completed++
                if (clipboard.mode == ClipboardMode.MOVE && !deleteOne(entry)) failed++
            } else failed++
        }
        when {
            completed == 0 -> OperationResult(false, "No items were pasted")
            failed == 0 -> OperationResult(true, if (clipboard.mode == ClipboardMode.MOVE) "Moved $completed item(s)" else "Copied $completed item(s)")
            else -> OperationResult(false, "Completed $completed item(s); $failed failed")
        }
    }

    private fun copyIntoSmb(entry: FileEntry, destinationUri: Uri): Boolean {
        if (entry.isDirectory) {
            return withShare(destinationUri) { share, _ ->
                val relative = SmbUri.relativePath(destinationUri)
                val name = uniqueSmbName(share, relative, entry.name, true)
                val target = if (relative.isBlank()) name else "$relative/$name"
                share.mkdir(target)
                true
            }
        }
        val input = openEntryStream(entry) ?: return false
        return input.use { stream ->
            withShare(destinationUri) { share, _ ->
                val relative = SmbUri.relativePath(destinationUri)
                val name = uniqueSmbName(share, relative, entry.name, false)
                val target = if (relative.isBlank()) name else "$relative/$name"
                share.openFile(
                    target,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
                ).outputStream.use { output -> stream.copyTo(output) }
                true
            }
        }
    }

    private fun openEntryStream(entry: FileEntry): InputStream? {
        if (entry.isDirectory) return null
        if (SmbUri.isSmb(entry.uri)) return openBlockingStream(entry.uri)
        return runCatching {
            when (entry.uri.scheme) {
                ContentResolver.SCHEME_FILE -> entry.uri.path?.let { FileInputStream(File(it)) }
                else -> context.contentResolver.openInputStream(entry.uri)
            }
        }.getOrNull()
    }

    private fun copyFromSmb(entry: FileEntry, destinationUri: Uri): Boolean {
        val destinationRoot = destinationUri.path?.let(::File)?.takeIf { it.exists() && it.isDirectory } ?: return false
        val target = File(destinationRoot, uniqueLocalName(destinationRoot, entry.name, entry.isDirectory))
        return runCatching {
            if (entry.isDirectory) {
                if (!target.mkdirs()) return false
                withShare(entry.uri) { share, _ ->
                    val relative = SmbUri.relativePath(entry.uri)
                    share.list(relative).forEach { item ->
                        val name = item.fileName
                        if (name == "." || name == "..") return@forEach
                        val isDirectory = item.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
                        val childPath = if (relative.isBlank()) name else "$relative/$name"
                        val childEntry = FileEntry(
                            uri = SmbUri.build(SmbUri.host(entry.uri), SmbUri.share(entry.uri), childPath, SmbUri.port(entry.uri)),
                            name = name,
                            mimeType = if (isDirectory) null else mimeForName(name),
                            isDirectory = isDirectory,
                            size = if (isDirectory) 0L else item.endOfFile,
                            lastModified = item.lastWriteTime?.toEpochMillis() ?: 0L
                        )
                        copyFromSmb(childEntry, Uri.fromFile(target))
                    }
                }
                true
            } else {
                val input = openBlockingStream(entry.uri) ?: return false
                input.use { stream -> target.outputStream().use { output -> stream.copyTo(output) } }
                true
            }
        }.getOrDefault(false)
    }

    private fun openBlockingStream(uri: Uri): InputStream? = runCatching {
        withShare(uri) { share, _ ->
            share.openFile(
                SmbUri.relativePath(uri),
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
            ).inputStream
        }
    }.getOrNull()

    private fun deleteOne(entry: FileEntry): Boolean = runCatching {
        withShare(entry.uri) { share, _ ->
            val relative = SmbUri.relativePath(entry.uri)
            if (entry.isDirectory) deleteTree(share, relative) else share.rm(relative)
        }
        true
    }.getOrDefault(false)

    suspend fun search(rootUri: Uri, query: String, showHidden: Boolean): List<FileEntry> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val needle = query.trim().lowercase(Locale.ROOT)
        val results = mutableListOf<FileEntry>()
        runCatching {
            withShare(rootUri) { share, credentials ->
                val queue = ArrayDeque<String>()
                queue += SmbUri.relativePath(rootUri)
                var scanned = 0
                while (queue.isNotEmpty() && scanned < 20_000 && results.size < 500) {
                    val current = queue.removeFirst()
                    share.list(current).forEach { item ->
                        val name = item.fileName
                        if (name == "." || name == "..") return@forEach
                        scanned++
                        if (!showHidden && name.startsWith('.')) return@forEach
                        val isDirectory = item.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
                        val childPath = if (current.isBlank()) name else "$current/$name"
                        if (name.lowercase(Locale.ROOT).contains(needle)) {
                            results += FileEntry(
                                uri = SmbUri.build(credentials.host, credentials.share, childPath, credentials.port),
                                name = name,
                                mimeType = if (isDirectory) null else mimeForName(name),
                                isDirectory = isDirectory,
                                size = if (isDirectory) 0L else item.endOfFile,
                                lastModified = item.lastWriteTime?.toEpochMillis() ?: 0L
                            )
                        }
                        if (isDirectory) queue += childPath
                    }
                }
            }
        }
        results.sortedWith(entryComparator(SortOption.NAME, true))
    }

    suspend fun calculateStorage(rootUri: Uri, showHidden: Boolean): StorageBreakdown = withContext(Dispatchers.IO) {
        var documents = 0L; var images = 0L; var videos = 0L; var audio = 0L; var archives = 0L; var other = 0L
        var files = 0; var folders = 0
        runCatching {
            withShare(rootUri) { share, _ ->
                val queue = ArrayDeque<String>()
                queue += SmbUri.relativePath(rootUri)
                var scanned = 0
                while (queue.isNotEmpty() && scanned < 40_000) {
                    val current = queue.removeFirst()
                    if (current.isNotBlank()) folders++
                    share.list(current).forEach { item ->
                        val name = item.fileName
                        if (name == "." || name == "..") return@forEach
                        scanned++
                        if (!showHidden && name.startsWith('.')) return@forEach
                        val isDirectory = item.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
                        val childPath = if (current.isBlank()) name else "$current/$name"
                        if (isDirectory) {
                            queue += childPath
                        } else {
                            files++
                            val size = item.endOfFile
                            when (categoryFor(name, mimeForName(name))) {
                                "image" -> images += size
                                "video" -> videos += size
                                "audio" -> audio += size
                                "archive" -> archives += size
                                "document" -> documents += size
                                else -> other += size
                            }
                        }
                    }
                }
            }
        }
        StorageBreakdown(documents, images, videos, audio, archives, other, files, folders)
    }

    suspend fun saveCredentials(credentials: SmbCredentials): OperationResult = withContext(Dispatchers.IO) {
        val result = testConnection(credentials)
        if (result.success) store.save(credentials)
        result
    }

    fun credentialsFor(uri: Uri): SmbCredentials? = store.load(SmbUri.host(uri), SmbUri.share(uri))

    fun forget(uri: Uri) {
        val credentials = store.load(SmbUri.host(uri), SmbUri.share(uri)) ?: return
        invalidate(credentials)
        store.remove(credentials.host, credentials.share)
    }

    private inline fun <T> withShare(uri: Uri, block: (DiskShare, SmbCredentials) -> T): T {
        val credentials = resolveCredentials(uri)
        val share = shareFor(uri)
        return try {
            block(share, credentials)
        } catch (error: Exception) {
            invalidate(credentials)
            throw error
        } finally {
            runCatching { share.close() }
        }
    }

    private fun uniqueSmbName(share: DiskShare, parent: String, requested: String, directory: Boolean): String {
        fun exists(candidate: String): Boolean {
            val path = if (parent.isBlank()) candidate else "$parent/$candidate"
            return share.fileExists(path) || share.folderExists(path)
        }
        if (!exists(requested)) return requested
        val stem = if (directory) requested else requested.substringBeforeLast('.', requested)
        val extension = if (directory || !requested.contains('.')) "" else ".${requested.substringAfterLast('.')}"
        var index = 1
        while (exists("$stem ($index)$extension")) index++
        return "$stem ($index)$extension"
    }

    private fun uniqueLocalName(parent: File, requested: String, directory: Boolean): String {
        if (!File(parent, requested).exists()) return requested
        val stem = if (directory) requested else requested.substringBeforeLast('.', requested)
        val extension = if (directory || !requested.contains('.')) "" else ".${requested.substringAfterLast('.')}"
        var index = 1
        while (File(parent, "$stem ($index)$extension").exists()) index++
        return "$stem ($index)$extension"
    }

    private fun validName(name: String) = name.isNotBlank() && !name.contains('/') && !name.contains('\\')

    private fun categoryFor(name: String, mime: String): String {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when {
            mime.startsWith("image/") -> "image"
            mime.startsWith("video/") -> "video"
            mime.startsWith("audio/") -> "audio"
            extension in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> "archive"
            mime.startsWith("text/") || mime.contains("pdf") || extension in setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "epub", "md", "json", "csv", "xml") -> "document"
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

    private fun entryComparator(sort: SortOption, ascending: Boolean): Comparator<FileEntry> {
        val comparator = when (sort) {
            SortOption.NAME -> compareBy<FileEntry> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
            SortOption.DATE -> compareBy<FileEntry> { !it.isDirectory }.thenByDescending { it.lastModified }
            SortOption.SIZE -> compareBy<FileEntry> { !it.isDirectory }.thenByDescending { it.size }
            SortOption.TYPE -> compareBy<FileEntry> { !it.isDirectory }.thenBy { it.extension }.thenBy { it.name.lowercase(Locale.ROOT) }
        }
        return if (ascending) comparator else comparator.reversed()
    }

    private fun operationSummary(total: Int, failed: Int, verb: String): OperationResult = when {
        total == 0 -> OperationResult(false, "Nothing selected")
        failed == 0 -> OperationResult(true, if (total == 1) "Item $verb" else "$total items $verb")
        failed == total -> OperationResult(false, "Could not $verb selected items")
        else -> OperationResult(false, "${total - failed} item(s) $verb; $failed failed")
    }

    private fun Any.toEpochMillis(): Long = (this as com.hierynomus.msdtyp.FileTime).toEpochMillis()
}