package com.buge.files.ui.files

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buge.files.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SortOrder {
    NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, SIZE_LARGEST, SIZE_SMALLEST
}

class FilesViewModel : ViewModel() {

    private val _files = MutableLiveData<List<FileItem>>()
    val files: LiveData<List<FileItem>> = _files

    private val _currentPath = MutableLiveData<String>()
    val currentPath: LiveData<String> = _currentPath

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 搜索相关
    private val _searchResults = MutableLiveData<List<FileItem>?>()
    val searchResults: LiveData<List<FileItem>?> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private var searchJob: Job? = null

    private val pathStack = ArrayDeque<String>()
    var showHidden = false
    var sortOrder = SortOrder.NAME_ASC
    var isGridView = false

    fun loadRoot() {
        val root = Environment.getExternalStorageDirectory().absolutePath
        loadPath(root, clearStack = true)
    }

    fun loadPath(path: String, clearStack: Boolean = false) {
        if (clearStack) pathStack.clear()
        _currentPath.value = path
        viewModelScope.launch {
            _isLoading.value = true
            val items = withContext(Dispatchers.IO) { readDirectory(path) }
            _files.value = items
            _isLoading.value = false
        }
    }

    fun navigateTo(item: FileItem) {
        if (item.isDirectory) {
            val parent = _currentPath.value ?: return
            pathStack.addLast(parent)
            loadPath(item.path)
        }
    }

    fun navigateUp(): Boolean {
        if (pathStack.isEmpty()) return false
        val parent = pathStack.removeLast()
        loadPath(parent)
        return true
    }

    fun canNavigateUp(): Boolean = pathStack.isNotEmpty()

    fun applySortOrder(order: SortOrder) {
        sortOrder = order
        _currentPath.value?.let { loadPath(it) }
    }

    // 搜索：在当前目录及所有子目录中递归搜索
    fun search(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }
        val rootPath = _currentPath.value ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _isLoading.value = true
            val results = withContext(Dispatchers.IO) {
                searchFiles(File(rootPath), query.trim().lowercase())
            }
            _searchResults.value = results
            _isLoading.value = false
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = null
        _isSearching.value = false
    }

    private fun searchFiles(dir: File, query: String): List<FileItem> {
        if (!dir.canRead()) return emptyList()
        val results = mutableListOf<FileItem>()
        dir.listFiles()?.forEach { file ->
            if (!showHidden && file.name.startsWith(".")) return@forEach
            if (file.name.lowercase().contains(query)) {
                results.add(FileItem(file))
            }
            if (file.isDirectory) {
                results.addAll(searchFiles(file, query))
            }
        }
        return results.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun createFolder(name: String): Boolean {
        val parent = _currentPath.value ?: return false
        val newDir = File(parent, name)
        return if (newDir.mkdir()) { loadPath(parent); true } else false
    }

    fun deleteFile(item: FileItem): Boolean {
        val success = item.file.deleteRecursively()
        if (success) _currentPath.value?.let { loadPath(it) }
        return success
    }

    fun renameFile(item: FileItem, newName: String): Boolean {
        val newFile = File(item.file.parent ?: return false, newName)
        val success = item.file.renameTo(newFile)
        if (success) _currentPath.value?.let { loadPath(it) }
        return success
    }

    private fun readDirectory(path: String): List<FileItem> {
        val dir = File(path)
        if (!dir.exists() || !dir.canRead()) return emptyList()
        val raw = dir.listFiles()?.map { FileItem(it) } ?: emptyList()
        val filtered = if (showHidden) raw else raw.filter { !it.isHidden }
        val (dirs, files) = filtered.partition { it.isDirectory }
        return sortItems(dirs) + sortItems(files)
    }

    private fun sortItems(items: List<FileItem>): List<FileItem> = when (sortOrder) {
        SortOrder.NAME_ASC -> items.sortedBy { it.name.lowercase() }
        SortOrder.NAME_DESC -> items.sortedByDescending { it.name.lowercase() }
        SortOrder.DATE_NEWEST -> items.sortedByDescending { it.lastModified }
        SortOrder.DATE_OLDEST -> items.sortedBy { it.lastModified }
        SortOrder.SIZE_LARGEST -> items.sortedByDescending { it.size }
        SortOrder.SIZE_SMALLEST -> items.sortedBy { it.size }
    }
}