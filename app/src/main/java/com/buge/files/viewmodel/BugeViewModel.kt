// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Buge Studio

package com.buge.files

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class BrowseScrollPosition(val index: Int, val offset: Int)

private data class ArchiveReturn(val root: RootLocation?, val path: List<RootLocation>)

class BugeViewModel(application: Application) : AndroidViewModel(application) {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    var shizukuAvailable by mutableStateOf(ShizukuShell.isAvailable())
        private set
    var shizukuPermission by mutableStateOf(ShizukuShell.hasPermission())
        private set

    private val shizukuBinderListener = Shizuku.OnBinderReceivedListener {
        refreshShizuku()
        updateVisibleRoots()
    }

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == ShizukuShell.REQUEST_CODE) {
            refreshShizuku()
            if (grantResult != PackageManager.PERMISSION_GRANTED) showMessage("Shizuku permission was not granted")
            updateVisibleRoots()
            refresh()
        }
    }

    private val fileRepository = FileRepository(
        application,
        { _settings.value.rootAccess },
        { _settings.value.rootCommand },
        { _settings.value.shizukuAccess },
        { _settings.value.autoRemount }
    )
    val smbRepository = SmbRepository(application)
    private val advancedToolsRepository = AdvancedToolsRepository(application)
    private val copyProgressNotifier = CopyProgressNotifier(application)
    private val apkRepository = ApkRepository(application)
    private val updateRepository = UpdateRepository()
    private val settingsRepository = SettingsRepository(application)
    private var loadingJob: Job? = null
    private var lastRootCommand: String? = "su"
    private val browseScrollPositions = mutableMapOf<String, BrowseScrollPosition>()
    private val archiveStack = ArrayDeque<ArchiveReturn>()
    private var lastLoadedPathKey: String? = null

    private val _roots = MutableStateFlow<List<RootLocation>>(emptyList())
    val roots: StateFlow<List<RootLocation>> = _roots.asStateFlow()
    private var safRoots: List<RootLocation> = emptyList()

    private val _directStorageAvailable = MutableStateFlow(false)
    val directStorageAvailable: StateFlow<Boolean> = _directStorageAvailable.asStateFlow()

    private val _rootAvailable = MutableStateFlow(false)
    val rootAvailable: StateFlow<Boolean> = _rootAvailable.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<RootLocation>>(emptyList())
    val bookmarks: StateFlow<List<RootLocation>> = _bookmarks.asStateFlow()

    private val _currentRoot = MutableStateFlow<RootLocation?>(null)
    val currentRoot: StateFlow<RootLocation?> = _currentRoot.asStateFlow()

    private val _entries = MutableStateFlow<List<FileEntry>>(emptyList())
    val entries: StateFlow<List<FileEntry>> = _entries.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileEntry>>(emptyList())
    val searchResults: StateFlow<List<FileEntry>> = _searchResults.asStateFlow()

    private val _storage = MutableStateFlow(StorageBreakdown())
    val storage: StateFlow<StorageBreakdown> = _storage.asStateFlow()

    var destination by mutableStateOf(AppDestination.BROWSE)
        private set
    var navigationDirection by mutableStateOf(NavigationDirection.FORWARD)
        private set
    var navigationPath by mutableStateOf<List<RootLocation>>(emptyList())
        private set
    var sortOption by mutableStateOf(SortOption.NAME)
        private set
    var ascending by mutableStateOf(true)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var isSearching by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isStorageLoading by mutableStateOf(false)
        private set
    var selection by mutableStateOf<Set<Uri>>(emptySet())
        private set
    private val _tasks = MutableStateFlow<List<FileTask>>(emptyList())
    val tasks: StateFlow<List<FileTask>> = _tasks.asStateFlow()
    var activeTaskId by mutableStateOf<Long?>(null)
        private set
    var taskRunning by mutableStateOf(false)
        private set
    var installInProgress by mutableStateOf(false)
        private set
    private var nextTaskId = 1L
    var message by mutableStateOf<String?>(null)
        private set
    var pendingInfo by mutableStateOf<FileEntry?>(null)
        private set

    fun browseScrollPosition(path: List<RootLocation>): BrowseScrollPosition? =
        browseScrollPositions[browsePathKey(path)]

    fun saveBrowseScrollPosition(path: List<RootLocation>, index: Int, offset: Int) {
        browseScrollPositions[browsePathKey(path)] = BrowseScrollPosition(index, offset)
    }

    private fun browsePathKey(path: List<RootLocation>): String =
        path.joinToString("/") { it.uri.toString() }

    var editorTarget by mutableStateOf<FileEntry?>(null)
        private set
    var editorText by mutableStateOf("")
        private set
    var editorLoading by mutableStateOf(false)
        private set
    var archiveTarget by mutableStateOf<FileEntry?>(null)
        private set
    var archiveItems by mutableStateOf<List<ArchiveItem>>(emptyList())
        private set
    var archiveLoading by mutableStateOf(false)
        private set
    var imagePreview by mutableStateOf<FileEntry?>(null)
        private set
    var checksumTarget by mutableStateOf<FileEntry?>(null)
        private set
    var checksumValue by mutableStateOf<String?>(null)
        private set
    var checksumLoading by mutableStateOf(false)
        private set
    var apkTarget by mutableStateOf<FileEntry?>(null)
        private set
    var apkMetadata by mutableStateOf<ApkMetadata?>(null)
        private set
    var apkLoading by mutableStateOf(false)
        private set
    var smbDialogVisible by mutableStateOf(false)
        private set
    var testingSmb by mutableStateOf(false)
        private set
    var updateDialogVisible by mutableStateOf(false)
        private set
    var updateChecking by mutableStateOf(false)
        private set
    var updateResult by mutableStateOf<UpdateCheckResult?>(null)
        private set

    fun showSmbDialog() { smbDialogVisible = true }
    fun dismissSmbDialog() { smbDialogVisible = false }

    fun checkForUpdates() {
        if (updateChecking) return
        updateDialogVisible = true
        updateChecking = true
        updateResult = null
        viewModelScope.launch {
            updateResult = updateRepository.check(BuildConfig.VERSION_NAME)
            updateChecking = false
        }
    }

    fun dismissUpdateDialog() {
        updateDialogVisible = false
        updateResult = null
    }

    private val recentItems = mutableStateListOf<FileEntry>()
    val recents: List<FileEntry> get() = recentItems

    init {
        ShizukuShell.addBinderListener(shizukuBinderListener)
        ShizukuShell.addPermissionListener(shizukuListener)
        viewModelScope.launch {
            settingsRepository.settings.collect { newSettings ->
                _settings.value = newSettings
                if (newSettings.rootCommand != lastRootCommand) {
                    lastRootCommand = newSettings.rootCommand
                    detectRootAccess()
                }
                if (!newSettings.showRecents) {
                    recentItems.clear()
                    if (destination == AppDestination.RECENTS) destination = AppDestination.BROWSE
                }
                updateVisibleRoots()
                refresh()
            }
        }
        viewModelScope.launch {
            settingsRepository.roots.collect { locations ->
                safRoots = locations
                updateVisibleRoots()
            }
        }
        refreshDirectStorageAccess(preferDirect = false)
        detectRootAccess()
        viewModelScope.launch { settingsRepository.bookmarks.collect { _bookmarks.value = it } }
    }

    fun selectDestination(value: AppDestination) {
        destination = value
        isSearching = false
        searchQuery = ""
        clearSelection()
        if (value == AppDestination.STORAGE) refreshStorage()
    }

    fun addRoot(uri: Uri) {
        val document = DocumentFile.fromTreeUri(getApplication(), uri)
        val label = document?.name?.takeIf { it.isNotBlank() } ?: "Storage"
        viewModelScope.launch {
            settingsRepository.addRoot(RootLocation(uri, label))
            selectRoot(RootLocation(uri, label), reportFailure = true)
            showMessage("Added $label")
        }
    }

    fun addSmbRoot(host: String, share: String, username: String, password: String, domain: String, port: Int) {
        viewModelScope.launch {
            val credentials = SmbCredentials(
                host = host.trim(),
                share = share.trim().trimStart('/'),
                username = username,
                password = password,
                domain = domain.trim(),
                port = if (port > 0) port else 445
            )
            testingSmb = true
            val result = smbRepository.saveCredentials(credentials)
            testingSmb = false
            if (!result.success) {
                showMessage(result.message)
                return@launch
            }
            val uri = SmbUri.build(credentials.host, credentials.share, "", credentials.port)
            val label = credentials.displayLabel
            settingsRepository.addRoot(RootLocation(uri, label))
            selectRoot(RootLocation(uri, label), reportFailure = true)
            showMessage("Added $label")
        }
    }

    fun forgetSmbRoot(location: RootLocation) = viewModelScope.launch {
        smbRepository.forget(location.uri)
        settingsRepository.removeRoot(location.uri)
        showMessage("Removed ${location.label}")
    }

    fun removeRoot(location: RootLocation) = viewModelScope.launch {
        if (location.uri.scheme == "file") return@launch
        if (SmbUri.isSmb(location.uri)) smbRepository.forget(location.uri)
        settingsRepository.removeRoot(location.uri)
        showMessage("Removed ${location.label}")
    }

    /** Refreshes the special all-files-access state after the user returns from system settings. */
    fun refreshDirectStorageAccess(preferDirect: Boolean = true) {
        val available = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val app = getApplication<Application>()
            app.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || app.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        }
        _directStorageAvailable.value = available
        updateVisibleRoots(preferDirect)
    }

    fun refreshShizuku() {
        shizukuAvailable = ShizukuShell.isAvailable()
        shizukuPermission = ShizukuShell.hasPermission()
    }

    fun shizukuReady(): Boolean = shizukuAvailable && shizukuPermission && _settings.value.shizukuAccess

    fun requestShizukuPermission() {
        refreshShizuku()
        if (!shizukuAvailable) {
            showMessage(_settings.value.language.resolved().t("shizuku_unavailable"))
            return
        }
        ShizukuShell.requestPermission()
    }

    private fun detectRootAccess() {
        viewModelScope.launch {
            val available = withContext(Dispatchers.IO) { hasRootAccess() }
            _rootAvailable.value = available
            updateVisibleRoots()
        }
    }

    private fun hasRootAccess(): Boolean {
        val executable = _settings.value.rootCommand.trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { "su" }
        val binaries = listOf(
            executable,
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su", "/vendor/bin/su",
            "/data/adb/magisk", "/data/adb/ksu", "/data/adb/ap"
        ).distinct()
        if (binaries.any { runCatching { File(it).exists() }.getOrDefault(false) }) return true
        return runCatching {
            val process = ProcessBuilder("sh", "-c", "command -v ${shellQuote(executable)}").redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor() == 0
        }.getOrDefault(false)
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private fun rootDirectoryRoot(): RootLocation? =
        if (_rootAvailable.value && _settings.value.rootAccess) RootLocation(Uri.fromFile(File("/")), "Root directory") else null

    fun refreshStorageRoots() {
        updateVisibleRoots()
    }

    private fun externalRoots(): List<RootLocation> {
        val app = getApplication<Application>()
        val internal = runCatching { File("/sdcard").canonicalPath }.getOrNull()
        val manager = app.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        val volumes = runCatching { manager?.storageVolumes.orEmpty() }.getOrDefault(emptyList())
        val descriptions = volumes.mapNotNull { volume ->
            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) volume.directory else null
            if (dir == null) null else dir.absolutePath to runCatching { volume.getDescription(app) }.getOrNull()
        }.toMap()
        val roots = LinkedHashMap<String, RootLocation>()
        app.getExternalFilesDirs(null).filterNotNull().forEach { directory ->
            val path = directory.absolutePath
            val marker = path.indexOf("/Android/")
            if (marker <= 0) return@forEach
            val rootPath = path.substring(0, marker)
            val root = File(rootPath)
            val canonical = runCatching { root.canonicalPath }.getOrNull() ?: rootPath
            if (internal != null && canonical == internal) return@forEach
            if (!root.exists()) return@forEach
            val label = descriptions[rootPath] ?: descriptions[canonical] ?: "External storage"
            roots[canonical] = RootLocation(Uri.fromFile(File(canonical)), label)
        }
        if (roots.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            volumes.filter { it.isRemovable }.forEach { volume ->
                val directory = volume.directory ?: return@forEach
                val canonical = runCatching { directory.canonicalPath }.getOrNull() ?: return@forEach
                if (internal != null && canonical == internal) return@forEach
                if (!directory.exists()) return@forEach
                val label = runCatching { volume.getDescription(app) }.getOrNull() ?: "External storage"
                roots[canonical] = RootLocation(Uri.fromFile(directory), label)
            }
        }
        return roots.values.toList()
    }

    private fun updateVisibleRoots(preferDirect: Boolean = false) {
        val direct = directRoot().takeIf { _directStorageAvailable.value || shizukuReady() }
        val locations = (listOfNotNull(direct, rootDirectoryRoot()) + safRoots + externalRoots()).distinctBy { it.uri }
        _roots.value = locations
        val current = _currentRoot.value
        when {
            preferDirect && direct != null -> selectRoot(direct)
            current == null && locations.isNotEmpty() -> selectRoot(locations.first())
            current != null && locations.none { it.uri == current.uri } -> {
                archiveStack.clear()
                _currentRoot.value = locations.firstOrNull()
                navigationPath = locations.firstOrNull()?.let { listOf(it) }.orEmpty()
                refresh()
            }
        }
    }

    private fun directRoot(): RootLocation? {
        val root = runCatching { File("/sdcard").canonicalFile }.getOrNull() ?: return null
        val accessible = (root.exists() && root.isDirectory) || _directStorageAvailable.value || shizukuReady()
        return root.takeIf { accessible }?.let { RootLocation(Uri.fromFile(it), "Internal storage") }
    }
    fun selectRoot(location: RootLocation, reportFailure: Boolean = false) {
        navigationDirection = NavigationDirection.FORWARD
        archiveStack.clear()
        _currentRoot.value = location
        navigationPath = listOf(location)
        selection = emptySet()
        searchQuery = ""
        isSearching = false
        refresh(reportFailure = reportFailure)
    }

    fun openBookmark(location: RootLocation) {
        navigationDirection = NavigationDirection.FORWARD
        archiveStack.clear()
        destination = AppDestination.BROWSE
        val owner = owningRoot(location.uri)
        val rootUri = owner?.uri ?: location.uri
        val rootLabel = owner?.label ?: location.label
        val segments = childSegments(rootUri, location.uri)
        _currentRoot.value = owner ?: RootLocation(rootUri, rootLabel)
        val path = mutableListOf(RootLocation(rootUri, rootLabel))
        var currentUri = rootUri
        segments.forEachIndexed { index, name ->
            currentUri = childUri(currentUri, location.uri, index, segments.size)
            path += RootLocation(currentUri, name)
        }
        navigationPath = path
        selection = emptySet()
        searchQuery = ""
        isSearching = false
        refresh(reportFailure = true)
    }

    private fun owningRoot(uri: Uri): RootLocation? {
        val available = _roots.value
        if (SmbUri.isSmb(uri)) {
            val host = SmbUri.host(uri)
            val share = SmbUri.share(uri)
            return available.firstOrNull { SmbUri.isSmb(it.uri) && SmbUri.host(it.uri) == host && SmbUri.share(it.uri) == share }
        }
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val target = runCatching { File(uri.path.orEmpty()).canonicalPath }.getOrNull() ?: return null
            return available
                .filter { it.uri.scheme == ContentResolver.SCHEME_FILE }
                .filter { root -> runCatching { File(root.uri.path.orEmpty()).canonicalPath }.getOrNull()?.let { target == it || target.startsWith("$it/") } == true }
                .maxByOrNull { it.uri.path.orEmpty().length }
        }
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val treeId = documentId(uri) ?: return null
            return available
                .filter { it.uri.scheme == ContentResolver.SCHEME_CONTENT }
                .filter { documentId(it.uri)?.let { id -> treeId == id || treeId.startsWith("$id/") } == true }
                .maxByOrNull { documentId(it.uri).orEmpty().length }
        }
        return available.firstOrNull { it.uri == uri }
    }

    private fun documentId(uri: Uri): String? = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()

    private fun childSegments(rootUri: Uri, uri: Uri): List<String> {
        if (rootUri == uri) return emptyList()
        if (SmbUri.isSmb(uri)) {
            val relative = SmbUri.relativePath(uri)
            return if (relative.isBlank()) emptyList() else relative.split('/').filter { it.isNotBlank() }
        }
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val rootPath = rootUri.path.orEmpty()
            val childPath = uri.path.orEmpty()
            return childPath.removePrefix(rootPath).trim('/').split('/').filter { it.isNotBlank() }
        }
        val rootId = documentId(rootUri).orEmpty()
        val childId = documentId(uri).orEmpty()
        return childId.removePrefix(rootId).trim('/').split('/').filter { it.isNotBlank() }
    }

    private fun childUri(rootUri: Uri, target: Uri, index: Int, total: Int): Uri {
        if (index == total - 1) return target
        if (SmbUri.isSmb(target)) {
            return SmbUri.build(SmbUri.host(target), SmbUri.share(target), childSegments(rootUri, target).take(index + 1).joinToString("/"), SmbUri.port(target))
        }
        if (target.scheme == ContentResolver.SCHEME_FILE) {
            return Uri.fromFile(File(rootUri.path.orEmpty(), childSegments(rootUri, target).take(index + 1).joinToString("/")))
        }
        val currentId = childSegments(rootUri, target).take(index + 1).joinToString("/")
        return DocumentsContract.buildDocumentUriUsingTree(target, currentId)
    }


    fun openDirectory(entry: FileEntry) {
        if (!entry.isDirectory) return
        navigationDirection = NavigationDirection.FORWARD
        navigationPath = navigationPath + RootLocation(entry.uri, entry.name)
        selection = emptySet()
        refresh(reportFailure = true)
    }

    fun navigateTo(index: Int) {
        navigationDirection = if (index + 1 < navigationPath.size) NavigationDirection.BACK else NavigationDirection.FORWARD
        navigationPath = navigationPath.take(index + 1)
        selection = emptySet()
        refresh(reportFailure = true)
    }

    fun navigateUp(): Boolean {
        if (navigationPath.size <= 1) return false
        navigationDirection = NavigationDirection.BACK
        navigationPath = navigationPath.dropLast(1)
        selection = emptySet()
        refresh(reportFailure = true)
        return true
    }

    fun updateSearch(query: String) {
        searchQuery = query
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        val root = _currentRoot.value ?: return
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            isLoading = true
            _searchResults.value = runCatching {
                if (SmbUri.isSmb(root.uri)) {
                    smbRepository.search(root.uri, query, _settings.value.showHidden)
                } else {
                    fileRepository.search(root.uri, query, _settings.value.showHidden)
                }
            }.getOrElse { error ->
                showMessage(connectionMessage(error))
                emptyList()
            }
            isLoading = false
        }
    }

    fun setSearchActive(value: Boolean) {
        isSearching = value
        if (!value) { searchQuery = ""; _searchResults.value = emptyList() }
    }

    fun toggleSelection(entry: FileEntry) {
        selection = if (entry.uri in selection) selection - entry.uri else selection + entry.uri
    }

    fun selectAll() {
        val visible = if (isSearching) _searchResults.value else _entries.value
        selection = visible.map { it.uri }.toSet()
    }
    fun clearSelection() { selection = emptySet() }
    fun isSelected(entry: FileEntry) = entry.uri in selection
    fun selectedEntries(): List<FileEntry> {
        val known = (_entries.value + _searchResults.value + recentItems).distinctBy { it.uri }.associateBy { it.uri }
        return selection.map { uri -> known[uri] ?: entryFromUri(uri) }
    }

    private fun entryFromUri(uri: Uri): FileEntry {
        val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { uri.toString() }
        val isDirectory = !name.contains('.')
        return FileEntry(
            uri = uri,
            name = name,
            mimeType = null,
            isDirectory = isDirectory,
            size = 0L,
            lastModified = 0L,
            childCount = 0
        )
    }

    fun setSort(option: SortOption) {
        if (option == sortOption) ascending = !ascending else { sortOption = option; ascending = true }
        refresh()
    }

    fun setViewMode(mode: ViewMode) = updateSettings(_settings.value.copy(viewMode = mode))
    fun toggleHidden() = updateSettings(_settings.value.copy(showHidden = !_settings.value.showHidden))
    fun updateSettings(value: AppSettings) = viewModelScope.launch { settingsRepository.update(value) }

    fun createFolder(name: String) {
        val location = navigationPath.lastOrNull() ?: return
        viewModelScope.launch {
            val result = if (SmbUri.isSmb(location.uri)) {
                smbRepository.createFolder(location.uri, name)
            } else {
                fileRepository.createFolder(location.uri, name)
            }
            showMessage(result.message)
            if (result.success) refresh()
        }
    }

    fun createFile(name: String) {
        val location = navigationPath.lastOrNull() ?: return
        viewModelScope.launch {
            val result = if (SmbUri.isSmb(location.uri)) {
                smbRepository.createFile(location.uri, name)
            } else {
                fileRepository.createFile(location.uri, name)
            }
            showMessage(result.message)
            if (result.success) refresh()
        }
    }

    suspend fun saveIncoming(uris: List<Uri>, destinationUri: Uri): OperationResult? {
        if (uris.isEmpty()) return OperationResult(false, "Nothing to save")
        val language = _settings.value.language.resolved()
        val label = language.t("saving")
        copyProgressNotifier.start(label, label)
        val onProgress: (Long, Long, Int, Int) -> Unit = { done, total, itemsDone, itemsTotal ->
            copyProgressNotifier.update(label, label, done, total, itemsDone, itemsTotal)
        }
        val result = try {
            if (SmbUri.isSmb(destinationUri)) {
                smbRepository.saveExternal(uris, destinationUri, onProgress)
            } else {
                fileRepository.saveExternal(uris, destinationUri, onProgress)
            }
        } catch (error: Throwable) {
            copyProgressNotifier.finish(false, language.t("transfer_failed"), error.message.orEmpty())
            throw error
        }
        copyProgressNotifier.finish(result.success, language.t(if (result.success) "transfer_complete" else "transfer_failed"), result.message)
        showMessage(result.message)
        if (result.success) refresh()
        return result
    }

    fun rename(entry: FileEntry, name: String) = viewModelScope.launch {
        val result = if (SmbUri.isSmb(entry.uri)) {
            smbRepository.rename(entry, name)
        } else {
            fileRepository.rename(entry, name)
        }
        showMessage(result.message)
        if (result.success) { clearSelection(); refresh() }
    }

    fun compressSelected(archiveName: String) {
        val items = selectedEntries()
        if (items.isEmpty()) return
        enqueueTask(TaskType.COMPRESS, items, archiveName)
    }

    private fun enqueueTask(type: TaskType, items: List<FileEntry>, archiveName: String? = null) {
        val task = FileTask(id = nextTaskId++, type = type, entries = items, archiveName = archiveName)
        _tasks.value = _tasks.value + task
        activeTaskId = task.id
        clearSelection()
        showMessage(_settings.value.language.resolved().t("tasks_added"))
    }

    fun setActiveTask(id: Long) { activeTaskId = id }

    fun removeTask(id: Long) {
        _tasks.value = _tasks.value.filterNot { it.id == id }
        if (activeTaskId == id) activeTaskId = _tasks.value.lastOrNull()?.id
    }

    fun clearTasks() {
        _tasks.value = emptyList()
        activeTaskId = null
    }

    private fun updateTask(id: Long, transform: (FileTask) -> FileTask) {
        _tasks.value = _tasks.value.map { if (it.id == id) transform(it) else it }
    }

    fun openArchive(entry: FileEntry) {
        viewModelScope.launch {
            isLoading = true
            val source = materialize(entry)
            if (source == null) {
                isLoading = false
                showMessage("Archive is not available")
                return@launch
            }
            val directory = File(getApplication<Application>().cacheDir, "archives/${entry.uri.hashCode()}")
            directory.deleteRecursively()
            directory.mkdirs()
            val result = advancedToolsRepository.extractZip(source.uri, Uri.fromFile(directory))
            isLoading = false
            if (!result.success) {
                showMessage(result.message)
                return@launch
            }
            navigationDirection = NavigationDirection.FORWARD
            archiveStack.addLast(ArchiveReturn(_currentRoot.value, navigationPath))
            val location = RootLocation(Uri.fromFile(directory), entry.name)
            _currentRoot.value = location
            navigationPath = listOf(location)
            selection = emptySet()
            searchQuery = ""
            isSearching = false
            destination = AppDestination.BROWSE
            refresh()
        }
    }

    fun isInsideArchive(): Boolean = archiveStack.isNotEmpty() && navigationPath.size == 1

    fun leaveArchive(): Boolean {
        if (archiveStack.isEmpty()) return false
        navigationDirection = NavigationDirection.BACK
        val state = archiveStack.removeLast()
        _currentRoot.value = state.root
        navigationPath = state.path
        selection = emptySet()
        searchQuery = ""
        isSearching = false
        refresh()
        return true
    }

    fun deleteSelected() = delete(selectedEntries())
    fun delete(entries: List<FileEntry>) = viewModelScope.launch {
        val smbEntries = entries.filter { SmbUri.isSmb(it.uri) }
        val localEntries = entries.filterNot { SmbUri.isSmb(it.uri) }
        val results = mutableListOf<OperationResult>()
        if (smbEntries.isNotEmpty()) results += smbRepository.delete(smbEntries)
        if (localEntries.isNotEmpty()) results += fileRepository.delete(localEntries)
        val result = results.firstOrNull { !it.success } ?: results.firstOrNull() ?: OperationResult(true, "Nothing to delete")
        showMessage(result.message)
        if (results.all { it.success }) { clearSelection(); refresh() }
    }

    fun copySelected(mode: ClipboardMode) {
        val items = selectedEntries()
        if (items.isEmpty()) return
        enqueueTask(if (mode == ClipboardMode.MOVE) TaskType.MOVE else TaskType.COPY, items)
    }

    fun activeTask(): FileTask? = _tasks.value.firstOrNull { it.id == activeTaskId }

    fun runTask(id: Long) {
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        val location = navigationPath.lastOrNull() ?: return
        if (taskRunning) return
        if (task.type == TaskType.COMPRESS) {
            runCompressTask(task, location.uri)
        } else {
            runPasteTask(task, location.uri)
        }
    }

    fun paste() {
        activeTask()?.let { runTask(it.id) }
    }

    private fun runCompressTask(task: FileTask, destination: Uri) {
        viewModelScope.launch {
            taskRunning = true
            updateTask(task.id) { it.copy(state = TaskState.RUNNING) }
            isLoading = true
            val prepared = task.entries.map { entry ->
                if (fileRepository.needsElevation(entry.uri)) materialize(entry) ?: entry else entry
            }
            val result = advancedToolsRepository.compressToZip(prepared, destination, task.archiveName.orEmpty())
            isLoading = false
            taskRunning = false
            showMessage(result.message)
            if (result.success) {
                removeTask(task.id)
                refresh()
            } else {
                updateTask(task.id) { it.copy(state = TaskState.FAILED, detail = result.message) }
            }
        }
    }

    private fun runPasteTask(task: FileTask, destination: Uri) {
        val content = ClipboardState(task.entries, if (task.type == TaskType.MOVE) ClipboardMode.MOVE else ClipboardMode.COPY)
        viewModelScope.launch {
            taskRunning = true
            updateTask(task.id) { it.copy(state = TaskState.RUNNING) }
            isLoading = true
            val moving = content.mode == ClipboardMode.MOVE
            val language = _settings.value.language.resolved()
            val label = language.t(if (moving) "moving" else "copying")
            copyProgressNotifier.start(label, label)
            var notifierFailure: String? = null
            val onProgress: (Long, Long, Int, Int) -> Unit = { done, total, itemsDone, itemsTotal ->
                copyProgressNotifier.update(label, label, done, total, itemsDone, itemsTotal)
            }
            val smbEntries = content.entries.filter { SmbUri.isSmb(it.uri) }
            val localEntries = content.entries.filterNot { SmbUri.isSmb(it.uri) }
            val destinationIsSmb = SmbUri.isSmb(destination)
            val results = mutableListOf<OperationResult>()
            try {
                if (SmbUri.isSmb(destination) || smbEntries.isNotEmpty()) {
                    if (SmbUri.isSmb(destination)) {
                        val forSmb = smbEntries + localEntries
                        results += smbRepository.paste(content.copy(entries = forSmb), destination, onProgress)
                    } else {
                        results += smbRepository.paste(content.copy(entries = smbEntries), destination, onProgress)
                    }
                }
                if (!destinationIsSmb && localEntries.isNotEmpty()) {
                    results += fileRepository.paste(content.copy(entries = localEntries), destination, onProgress)
                }
            } catch (error: Throwable) {
                notifierFailure = error.message
            }
            isLoading = false
            taskRunning = false
            val result = when {
                results.isEmpty() -> OperationResult(false, "No items were pasted")
                results.any { it.success } -> OperationResult(true, results.first { it.success }.message)
                else -> results.first()
            }
            if (notifierFailure != null) {
                copyProgressNotifier.finish(false, language.t("transfer_failed"), notifierFailure)
            } else {
                copyProgressNotifier.finish(result.success, language.t(if (result.success) "transfer_complete" else "transfer_failed"), result.message)
            }
            showMessage(result.message)
            when {
                !result.success -> updateTask(task.id) { it.copy(state = TaskState.FAILED, detail = result.message) }
                task.type == TaskType.MOVE -> removeTask(task.id)
                else -> updateTask(task.id) { it.copy(state = TaskState.COMPLETED, runs = it.runs + 1, detail = result.message) }
            }
            if (result.success) refresh()
        }
    }

    fun currentLocationPath(): String {
        val location = navigationPath.lastOrNull() ?: return ""
        return if (location.uri.scheme == ContentResolver.SCHEME_FILE) location.uri.path ?: location.uri.toString()
        else location.uri.toString()
    }

    fun toggleBookmark(location: RootLocation) = viewModelScope.launch { settingsRepository.toggleBookmark(location) }
    fun isBookmarked(location: RootLocation) = _bookmarks.value.any { it.uri == location.uri }

    fun setInstalling(value: Boolean) { installInProgress = value }
    fun showInfo(entry: FileEntry) { pendingInfo = entry }
    fun dismissInfo() { pendingInfo = null }
    fun showMessage(value: String) { message = value }
    fun consumeMessage() { message = null }

    private fun connectionMessage(error: Throwable): String {
        val language = _settings.value.language.resolved()
        val detail = error.message?.trim().orEmpty()
        val base = language.t("connection_failed")
        return if (detail.isEmpty()) base else "$base · ${language.t("connection_failed_detail")}"
    }

    private fun readFailureMessage(error: Throwable): String {
        val language = _settings.value.language.resolved()
        val detail = error.message?.trim().orEmpty()
        return if (detail.isEmpty()) language.t("read_failed") else "$detail"
    }

    fun openBuiltInTool(entry: FileEntry): Boolean = when {
        entry.isApkPackage() -> { inspectApk(entry); recordOpened(entry); true }
        entry.isImageFile() -> { showImage(entry); true }
        entry.isZipContainer() -> { openArchive(entry); recordOpened(entry); true }
        entry.isEditableText() -> { openTextEditor(entry); recordOpened(entry); true }
        else -> false
    }

    private fun needsMaterialize(entry: FileEntry): Boolean =
        SmbUri.isSmb(entry.uri) || (fileRepository.needsElevation(entry.uri) && fileRepository.canElevate())

    fun openEntry(entry: FileEntry, onOpenFile: (FileEntry) -> Unit) {
        if (!needsMaterialize(entry) || entry.isDirectory) {
            if (!openBuiltInTool(entry)) { recordOpened(entry); onOpenFile(entry) }
            return
        }
        viewModelScope.launch {
            isLoading = true
            val local = materialize(entry)
            isLoading = false
            if (local == null) { showMessage("Could not open ${entry.name}"); return@launch }
            if (!openBuiltInTool(local)) { recordOpened(entry); onOpenFile(local) }
        }
    }

    private suspend fun materialize(entry: FileEntry): FileEntry? {
        val safeName = entry.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val app = getApplication<Application>()
        if (fileRepository.needsElevation(entry.uri)) {
            if (!fileRepository.canElevate()) return null
            val directory = app.externalCacheDir ?: app.cacheDir
            val target = File(directory, "${entry.uri.hashCode()}_$safeName")
            target.delete()
            if (!fileRepository.copyToLocalCopy(entry.uri, target)) return null
            return entry.copy(uri = Uri.fromFile(target))
        }
        if (!SmbUri.isSmb(entry.uri)) return entry
        val target = File(app.cacheDir, safeName)
        val language = _settings.value.language.resolved()
        val label = language.t("downloading")
        copyProgressNotifier.start(label, label)
        val ok = smbRepository.copyToLocal(entry.uri, target) { done, total, itemsDone, itemsTotal ->
            copyProgressNotifier.update(label, label, done, total, itemsDone, itemsTotal)
        }
        copyProgressNotifier.finish(ok, language.t(if (ok) "transfer_complete" else "transfer_failed"), entry.name)
        if (!ok || !target.exists()) return null
        return entry.copy(uri = Uri.fromFile(target))
    }

    private val thumbnailCache = mutableMapOf<String, File>()

    fun thumbnailSource(entry: FileEntry): File? {
        if (entry.isDirectory) return null
        if (!entry.isImageFile() && !entry.isVideoFile()) return null
        if (needsMaterialize(entry)) return thumbnailCache[entry.uri.toString()]
        return entry.uri.path?.let(::File)
    }

    fun loadThumbnail(entry: FileEntry) {
        if (entry.isDirectory) return
        if (!entry.isImageFile() && !entry.isVideoFile()) return
        if (!needsMaterialize(entry)) return
        val key = entry.uri.toString()
        if (thumbnailCache.containsKey(key)) return
        viewModelScope.launch {
            val local = materialize(entry) ?: return@launch
            local.uri.path?.let { thumbnailCache[key] = File(it) }
            thumbnailVersion++
        }
    }

    var thumbnailVersion by mutableStateOf(0)
        private set

    fun showImage(entry: FileEntry) {
        recordOpened(entry)
        if (needsMaterialize(entry)) {
            viewModelScope.launch {
                val local = materialize(entry)
                if (local != null) imagePreview = local
            }
        } else {
            imagePreview = entry
        }
    }

    fun inspectApk(entry: FileEntry) {
        apkTarget = entry
        apkMetadata = null
        apkLoading = true
        viewModelScope.launch {
            val local = if (needsMaterialize(entry)) materialize(entry) else entry
            if (local == null) {
                apkLoading = false
                apkTarget = null
                return@launch
            }
            apkTarget = local
            val result = apkRepository.inspect(local)
            apkLoading = false
            apkMetadata = result.metadata
            result.message?.let(::showMessage)
            if (result.metadata == null) apkTarget = null
        }
    }

    fun dismissApk() { apkTarget = null; apkMetadata = null; apkLoading = false }

    fun openTextEditor(entry: FileEntry) {
        editorTarget = entry
        editorText = ""
        editorLoading = true
        viewModelScope.launch {
            val local = if (needsMaterialize(entry)) materialize(entry) else entry
            if (local == null) {
                editorLoading = false
                editorTarget = null
                return@launch
            }
            editorTarget = local
            val result = advancedToolsRepository.readText(local.uri)
            editorLoading = false
            result.text?.let { editorText = it }
            result.message?.let(::showMessage)
            if (result.text == null) editorTarget = null
        }
    }

    fun updateEditorText(value: String) { editorText = value }
    fun dismissEditor() { editorTarget = null; editorText = ""; editorLoading = false }
    fun saveEditor() {
        val target = editorTarget ?: return
        viewModelScope.launch {
            editorLoading = true
            val result = advancedToolsRepository.saveText(target.uri, editorText)
            editorLoading = false
            showMessage(result.message)
            if (result.success) refresh()
        }
    }

    fun inspectArchive(entry: FileEntry) {
        archiveTarget = entry
        archiveItems = emptyList()
        archiveLoading = true
        viewModelScope.launch {
            val local = if (needsMaterialize(entry)) materialize(entry) else entry
            if (local == null) {
                archiveLoading = false
                archiveTarget = null
                return@launch
            }
            archiveTarget = local
            val result = advancedToolsRepository.listZip(local.uri)
            archiveItems = result.items
            archiveLoading = false
            result.message?.let(::showMessage)
        }
    }

    fun dismissArchive() { archiveTarget = null; archiveItems = emptyList(); archiveLoading = false }
    fun extractArchiveToCurrentLocation() {
        val archive = archiveTarget ?: return
        val destination = navigationPath.lastOrNull() ?: return
        viewModelScope.launch {
            archiveLoading = true
            val result = advancedToolsRepository.extractZip(archive.uri, destination.uri)
            archiveLoading = false
            showMessage(result.message)
            if (result.success) refresh()
        }
    }

    fun dismissImage() { imagePreview = null }

    fun calculateChecksum(entry: FileEntry) {
        checksumTarget = entry
        checksumValue = null
        checksumLoading = true
        viewModelScope.launch {
            val target = materialize(entry) ?: entry
            checksumValue = advancedToolsRepository.sha256(target.uri)
            checksumLoading = false
            if (checksumValue == null) showMessage("Could not calculate SHA-256")
        }
    }
    fun dismissChecksum() { checksumTarget = null; checksumValue = null; checksumLoading = false }

    fun recordOpened(entry: FileEntry) {
        if (!_settings.value.showRecents) return
        recentItems.removeAll { it.uri == entry.uri }
        recentItems.add(0, entry)
        while (recentItems.size > 40) recentItems.removeAt(recentItems.lastIndex)
    }

    fun refresh(reportFailure: Boolean = false) {
        val location = navigationPath.lastOrNull() ?: run { _entries.value = emptyList(); return }
        val pathKey = browsePathKey(navigationPath)
        if (pathKey != lastLoadedPathKey) {
            lastLoadedPathKey = pathKey
            _entries.value = emptyList()
        }
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            isLoading = true
            val remote = SmbUri.isSmb(location.uri)
            _entries.value = runCatching {
                if (remote) {
                    smbRepository.list(location.uri, sortOption, ascending, _settings.value.showHidden)
                } else {
                    fileRepository.list(location.uri, sortOption, ascending, _settings.value.showHidden)
                }
            }.getOrElse { error ->
                if (reportFailure) showMessage(if (remote) connectionMessage(error) else readFailureMessage(error))
                emptyList()
            }
            isLoading = false
        }
    }

    fun refreshStorage() {
        val root = _currentRoot.value ?: return
        viewModelScope.launch {
            isStorageLoading = true
            _storage.value = if (SmbUri.isSmb(root.uri)) {
                smbRepository.calculateStorage(root.uri, _settings.value.showHidden)
            } else {
                fileRepository.calculateStorage(root.uri, _settings.value.showHidden)
            }
            isStorageLoading = false
        }
    }

    override fun onCleared() {
        ShizukuShell.removeBinderListener(shizukuBinderListener)
        ShizukuShell.removePermissionListener(shizukuListener)
        super.onCleared()
    }
}
