package com.buge.files

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseScrollPosition(val index: Int, val offset: Int)

class BugeViewModel(application: Application) : AndroidViewModel(application) {
    private val fileRepository = FileRepository(application)
    val smbRepository = SmbRepository(application)
    private val advancedToolsRepository = AdvancedToolsRepository(application)
    private val apkRepository = ApkRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private var loadingJob: Job? = null
    private val browseScrollPositions = mutableMapOf<String, BrowseScrollPosition>()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _roots = MutableStateFlow<List<RootLocation>>(emptyList())
    val roots: StateFlow<List<RootLocation>> = _roots.asStateFlow()
    private var safRoots: List<RootLocation> = emptyList()

    private val _directStorageAvailable = MutableStateFlow(false)
    val directStorageAvailable: StateFlow<Boolean> = _directStorageAvailable.asStateFlow()

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
    var clipboard by mutableStateOf<ClipboardState?>(null)
        private set
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

    fun showSmbDialog() { smbDialogVisible = true }
    fun dismissSmbDialog() { smbDialogVisible = false }

    private val recentItems = mutableStateListOf<FileEntry>()
    val recents: List<FileEntry> get() = recentItems

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { newSettings ->
                _settings.value = newSettings
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
            selectRoot(RootLocation(uri, label))
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
            selectRoot(RootLocation(uri, label))
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

    private fun updateVisibleRoots(preferDirect: Boolean = false) {
        val direct = directRoot().takeIf { _directStorageAvailable.value }
        val locations = listOfNotNull(direct) + safRoots
        _roots.value = locations
        val current = _currentRoot.value
        when {
            preferDirect && direct != null -> selectRoot(direct)
            current == null && locations.isNotEmpty() -> selectRoot(locations.first())
            current != null && locations.none { it.uri == current.uri } -> {
                _currentRoot.value = locations.firstOrNull()
                navigationPath = locations.firstOrNull()?.let { listOf(it) }.orEmpty()
                refresh()
            }
        }
    }

    private fun directRoot(): RootLocation? {
        val root = runCatching { File("/sdcard").canonicalFile }.getOrNull() ?: return null
        return root.takeIf { it.exists() && it.isDirectory }?.let { RootLocation(Uri.fromFile(it), "Internal storage") }
    }
    fun selectRoot(location: RootLocation) {
        _currentRoot.value = location
        navigationPath = listOf(location)
        selection = emptySet()
        searchQuery = ""
        isSearching = false
        refresh()
    }

    fun openBookmark(location: RootLocation) {
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
        refresh()
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
        navigationPath = navigationPath + RootLocation(entry.uri, entry.name)
        selection = emptySet()
        refresh()
    }

    fun navigateTo(index: Int) {
        navigationPath = navigationPath.take(index + 1)
        selection = emptySet()
        refresh()
    }

    fun navigateUp(): Boolean {
        if (navigationPath.size <= 1) return false
        navigationPath = navigationPath.dropLast(1)
        selection = emptySet()
        refresh()
        return true
    }

    fun updateSearch(query: String) {
        searchQuery = query
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        val root = _currentRoot.value ?: return
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            isLoading = true
            _searchResults.value = if (SmbUri.isSmb(root.uri)) {
                smbRepository.search(root.uri, query, _settings.value.showHidden)
            } else {
                fileRepository.search(root.uri, query, _settings.value.showHidden)
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

    fun selectAll() { selection = _entries.value.map { it.uri }.toSet() }
    fun clearSelection() { selection = emptySet() }
    fun isSelected(entry: FileEntry) = entry.uri in selection
    fun selectedEntries(): List<FileEntry> = (_entries.value + _searchResults.value + recentItems).distinctBy { it.uri }.filter { it.uri in selection }

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
        val result = if (SmbUri.isSmb(destinationUri)) {
            smbRepository.saveExternal(uris, destinationUri)
        } else {
            fileRepository.saveExternal(uris, destinationUri)
        }
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
        val destination = navigationPath.lastOrNull() ?: return
        if (items.isEmpty()) return
        viewModelScope.launch {
            isLoading = true
            val result = advancedToolsRepository.compressToZip(items, destination.uri, archiveName)
            isLoading = false
            showMessage(result.message)
            if (result.success) { clearSelection(); refresh() }
        }
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
        clipboard = ClipboardState(items, mode)
        clearSelection()
        showMessage(if (mode == ClipboardMode.COPY) "Ready to copy ${items.size} item(s)" else "Ready to move ${items.size} item(s)")
    }

    fun paste() {
        val content = clipboard ?: return
        val location = navigationPath.lastOrNull() ?: return
        viewModelScope.launch {
            isLoading = true
            val smbEntries = content.entries.filter { SmbUri.isSmb(it.uri) }
            val localEntries = content.entries.filterNot { SmbUri.isSmb(it.uri) }
            val destinationIsSmb = SmbUri.isSmb(location.uri)
            val results = mutableListOf<OperationResult>()
            if (SmbUri.isSmb(location.uri) || smbEntries.isNotEmpty()) {
                if (SmbUri.isSmb(location.uri)) {
                    val forSmb = smbEntries + localEntries
                    results += smbRepository.paste(content.copy(entries = forSmb), location.uri)
                } else {
                    results += smbRepository.paste(content.copy(entries = smbEntries), location.uri)
                }
            }
            if (!destinationIsSmb && localEntries.isNotEmpty()) {
                results += fileRepository.paste(content.copy(entries = localEntries), location.uri)
            }
            isLoading = false
            val result = when {
                results.isEmpty() -> OperationResult(false, "No items were pasted")
                results.any { it.success } -> OperationResult(true, results.first { it.success }.message)
                else -> results.first()
            }
            showMessage(result.message)
            if (result.success) { clipboard = null; refresh() }
        }
    }

    fun toggleBookmark(location: RootLocation) = viewModelScope.launch { settingsRepository.toggleBookmark(location) }
    fun isBookmarked(location: RootLocation) = _bookmarks.value.any { it.uri == location.uri }

    fun showInfo(entry: FileEntry) { pendingInfo = entry }
    fun dismissInfo() { pendingInfo = null }
    fun showMessage(value: String) { message = value }
    fun consumeMessage() { message = null }

    fun openBuiltInTool(entry: FileEntry): Boolean = when {
        entry.isApkPackage() -> { inspectApk(entry); recordOpened(entry); true }
        entry.isImageFile() -> { imagePreview = entry; recordOpened(entry); true }
        entry.isZipContainer() -> { inspectArchive(entry); recordOpened(entry); true }
        entry.isEditableText() -> { openTextEditor(entry); recordOpened(entry); true }
        else -> false
    }

    fun openEntry(entry: FileEntry, onOpenFile: (FileEntry) -> Unit) {
        if (!SmbUri.isSmb(entry.uri) || entry.isDirectory) {
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
        if (!SmbUri.isSmb(entry.uri)) return entry
        val safeName = entry.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = File(getApplication<Application>().cacheDir, safeName)
        val ok = smbRepository.copyToLocal(entry.uri, target)
        if (!ok || !target.exists()) return null
        return entry.copy(uri = Uri.fromFile(target))
    }

    private val thumbnailCache = mutableMapOf<String, File>()

    fun thumbnailSource(entry: FileEntry): File? {
        if (entry.isDirectory) return null
        if (!entry.isImageFile() && !entry.isVideoFile()) return null
        if (SmbUri.isSmb(entry.uri)) return thumbnailCache[entry.uri.toString()]
        return entry.uri.path?.let(::File)
    }

    fun loadThumbnail(entry: FileEntry) {
        if (entry.isDirectory) return
        if (!entry.isImageFile() && !entry.isVideoFile()) return
        if (!SmbUri.isSmb(entry.uri)) return
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
        if (SmbUri.isSmb(entry.uri)) {
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
            val result = apkRepository.inspect(entry)
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
            val result = advancedToolsRepository.readText(entry.uri)
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
            val result = advancedToolsRepository.listZip(entry.uri)
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
        recentItems.removeAll { it.uri == entry.uri }
        recentItems.add(0, entry)
        while (recentItems.size > 40) recentItems.removeAt(recentItems.lastIndex)
    }

    fun refresh() {
        val location = navigationPath.lastOrNull() ?: run { _entries.value = emptyList(); return }
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            isLoading = true
            _entries.value = if (SmbUri.isSmb(location.uri)) {
                smbRepository.list(location.uri, sortOption, ascending, _settings.value.showHidden)
            } else {
                fileRepository.list(location.uri, sortOption, ascending, _settings.value.showHidden)
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
}
