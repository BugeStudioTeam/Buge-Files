package com.buge.files.ui.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buge.files.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RecentViewModel : ViewModel() {

    private val _recentFiles = MutableLiveData<List<FileItem>>()
    val recentFiles: LiveData<List<FileItem>> = _recentFiles

    fun load(root: File) {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                collectFiles(root)
                    .sortedByDescending { it.lastModified }
                    .take(50)
            }
            _recentFiles.value = files
        }
    }

    private fun collectFiles(dir: File): List<FileItem> {
        if (!dir.canRead()) return emptyList()
        val result = mutableListOf<FileItem>()
        dir.listFiles()?.forEach { file ->
            if (file.isFile) result.add(FileItem(file))
            else if (file.isDirectory && !file.name.startsWith(".")) {
                result.addAll(collectFiles(file))
            }
        }
        return result
    }
}