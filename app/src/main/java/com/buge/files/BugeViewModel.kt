package com.buge.files

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
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

class BugeViewModel(application: Application) : AndroidViewModel(application) {
    private val fileRepository = FileRepository(application)
    private val advancedToolsRepository = AdvancedToolsRepository(application)
    private val apkRepository = ApkRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val shizukuInstaller = ShizukuInstaller(application)
    private var loadingJob: Job? = null

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

    private val _shizukuReady = MutableStateFlow(false)
    val shizukuReady: StateFlow<Boolean> = _shizukuReady.asStateFlow()

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
    var isInstallingViaShizuku by mutableStateOf(false)
        private set

    private val recentItems = mutableStateListOf<FileEntry>()
    val recents: List<FileEntry> get() = recentItems

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { newSettings ->
                _settings.value = newSettings
                refresh()
                if (newSettings.shizukuEnabled) {
                    initializeShizuku()
                }
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

    fun removeRoot(location: RootLocation) = viewModelScope.launch {
        if (location.uri.scheme == "file") return@launch
        settingsRepository.removeRoot(location.uri)
        showMessage("Removed ${location.label}")
    }

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
            _searchResults.value = fileRepository.search(root.uri, query, _settings.value.showHidden)
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
            val result = fileRepository.createFolder(location.uri, name)
            showMessage(result.message)
            if (result.success) refresh()
        }
    }

    fun createFile(name: String) {
        val location = navigationPath.lastOrNull() ?: return
        viewModelScope.launch {
            val result = fileRepository.createFile(location.uri, name)
            showMessage(result.message)
            if (result.success) refresh()
        }
    }

    fun rename(entry: FileEntry, name: String) = viewModelScope.launch {
        val result = fileRepository.rename(entry, name)
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
        val result = fileRepository.delete(entries)
        showMessage(result.message)
        if (result.success) { clearSelection(); refresh() }
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
            val result = fileRepository.paste(content, location.uri)
            isLoading = false
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

    fun showImage(entry: FileEntry) { imagePreview = entry; recordOpened(entry) }
    fun dismissImage() { imagePreview = null }

    fun calculateChecksum(entry: FileEntry) {
        checksumTarget = entry
        checksumValue = null
        checksumLoading = true
        viewModelScope.launch {
            checksumValue = advancedToolsRepository.sha256(entry.uri)
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
            _entries.value = fileRepository.list(location.uri, sortOption, ascending, _settings.value.showHidden)
            isLoading = false
        }
    }

    fun refreshStorage() {
        val root = _currentRoot.value ?: return
        viewModelScope.launch {
            isStorageLoading = true
            _storage.value = fileRepository.calculateStorage(root.uri, _settings.value.showHidden)
            isStorageLoading = false
        }
    }

    fun initializeShizuku() = viewModelScope.launch {
        try {
            val ready = shizukuInstaller.initialize()
            _shizukuReady.value = ready
            if (ready) {
                showMessage("Shizuku connected")
            }
        } catch (e: Exception) {
            _shizukuReady.value = false
        }
    }

    fun installApkWithShizuku(file: FileEntry): Boolean {
        val settings = _settings.value
        if (!settings.shizukuEnabled || !_shizukuReady.value) return false
        isInstallingViaShizuku = true
        viewModelScope.launch {
            try {
                val installerName = if (settings.shizukuInstaller.isNotEmpty()) settings.shizukuInstaller else null
                val result = shizukuInstaller.installApk(file.uri, installerName ?: "")
                isInstallingViaShizuku = false
                showMessage(result.message)
                if (result.success) {
                    dismissApk()
                }
            } catch (e: Exception) {
                isInstallingViaShizuku = false
                showMessage("Shizuku install failed: ${e.message}")
            }
        }
        return true
    }

    fun cleanupShizuku() {
        shizukuInstaller.cleanup()
        _shizukuReady.value = false
    }

    override fun onCleared() {
        super.onCleared()
        cleanupShizuku()
    }
}