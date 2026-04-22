package com.buge.files.ui.files

import android.app.Dialog
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.buge.files.R
import com.buge.files.databinding.DialogCopyMoveBinding
import com.buge.files.model.FileItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class CopyMoveDialog(
    private val sourceItems: List<FileItem>,
    private val isCopy: Boolean,
    private val onComplete: () -> Unit
) : DialogFragment() {

    private var _binding: DialogCopyMoveBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DirectoryAdapter
    private var currentPath = Environment.getExternalStorageDirectory().absolutePath
    private val directoryStack = ArrayDeque<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCopyMoveBinding.inflate(LayoutInflater.from(requireContext()))
        
        setupRecyclerView()
        setupSearch()
        loadDirectory(currentPath)
        
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnPasteHere.setOnClickListener {
            performOperation(currentPath)
            dismiss()
        }
        binding.btnUp.setOnClickListener { navigateUp() }

        return MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(if (isCopy) "Copy to..." else "Move to...")
            .setView(binding.root)
            .setNegativeButton("Cancel") { _, _ -> dismiss() }
            .create()
    }

    override fun onStart() {
        super.onStart()
        // 设置对话框宽度
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupRecyclerView() {
        adapter = DirectoryAdapter { item ->
            if (item.isDirectory) {
                directoryStack.addLast(currentPath)
                loadDirectory(item.path)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText.orEmpty())
                return true
            }
        })
    }

    private fun loadDirectory(path: String) {
        currentPath = path
        binding.currentPathText.text = path
        lifecycleScope.launch {
            binding.progressBar.isVisible = true
            val items = withContext(Dispatchers.IO) {
                val dir = File(path)
                if (!dir.canRead()) return@withContext emptyList()
                dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }
                    ?.map { FileItem(it) }?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
            }
            adapter.submitList(items)
            binding.progressBar.isVisible = false
        }
    }

    private fun navigateUp() {
        if (directoryStack.isNotEmpty()) {
            val parent = directoryStack.removeLast()
            loadDirectory(parent)
        } else {
            val parent = File(currentPath).parent
            if (parent != null) {
                loadDirectory(parent)
            }
        }
    }

    private fun performOperation(destPath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val destDir = File(destPath)
            var successCount = 0
            
            sourceItems.forEach { item ->
                val destFile = File(destDir, item.name)
                try {
                    if (isCopy) {
                        copyRecursively(item.file, destFile)
                        successCount++
                    } else {
                        if (item.file.renameTo(destFile)) {
                            successCount++
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "${if (isCopy) "Copied" else "Moved"} $successCount item(s)",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete()
            }
        }
    }

    private fun copyRecursively(src: File, dest: File) {
        if (src.isDirectory) {
            if (!dest.exists()) dest.mkdirs()
            src.listFiles()?.forEach { child ->
                copyRecursively(child, File(dest, child.name))
            }
        } else {
            FileInputStream(src).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DirectoryAdapter(
        private val onClick: (FileItem) -> Unit
    ) : RecyclerView.Adapter<DirectoryAdapter.ViewHolder>() {

        private var items = listOf<FileItem>()
        private var filteredItems = listOf<FileItem>()

        fun submitList(list: List<FileItem>) {
            items = list
            filteredItems = list
            notifyDataSetChanged()
        }

        fun filter(query: String) {
            filteredItems = if (query.isBlank()) items
            else items.filter { it.name.contains(query, ignoreCase = true) }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_directory, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = filteredItems[position]
            holder.name.text = item.name
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = filteredItems.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.directory_name)
        }
    }
}