package com.buge.files.ui.files

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.buge.files.FabClickListener
import com.buge.files.R
import com.buge.files.databinding.FragmentFilesBinding
import com.buge.files.model.FileItem
import com.buge.files.util.PrefsManager
import com.buge.files.util.ZipHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File

class FilesFragment : Fragment(), FabClickListener {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FilesViewModel by viewModels()
    private lateinit var adapter: FileAdapter
    private lateinit var prefs: PrefsManager
    private var searchView: SearchView? = null
    private var isInSearchMode = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) loadFiles()
        else showPermissionDenied()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())
        viewModel.showHidden = prefs.showHiddenFiles
        viewModel.isGridView = prefs.isGridView

        adapter = FileAdapter(
            onItemClick = { handleClick(it) },
            onItemLongClick = { item -> handleMultiSelectLongClick(item) },
            onMoreClick = { item, anchor -> showContextMenu(item, anchor) },
            onSelectionChanged = { updateSelectionUI(it) }
        )

        setupRecyclerView()
        observeViewModel()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        adapter.isMultiSelectEnabled() -> exitMultiSelectMode()
                        isInSearchMode -> {
                            searchView?.setQuery("", false)
                            searchView?.isIconified = true
                            viewModel.clearSearch()
                        }
                        !viewModel.navigateUp() -> {
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        )

        checkPermissionsAndLoad()
    }

    override fun onFabClick() {
        if (adapter.isMultiSelectEnabled()) {
            exitMultiSelectMode()
        } else {
            showCreateDialog()
        }
    }

    private fun updateSelectionUI(selected: Set<FileItem>) {
        val count = selected.size
        if (count > 0) {
            activity?.title = "$count selected"
        } else if (adapter.isMultiSelectEnabled()) {
            exitMultiSelectMode()
        } else if (!isInSearchMode) {
            activity?.title = viewModel.currentPath.value?.substringAfterLast("/")?.ifEmpty { "Files" } ?: "Files"
        }
    }

    // 多选模式下长按处理 - 批量操作
    private fun handleMultiSelectLongClick(item: FileItem) {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) return
        
        val options = mutableListOf<String>()
        options.add("Batch Copy")
        options.add("Batch Move")
        options.add("Batch Delete")
        options.add("Batch Compress")
        options.add("Batch Properties")
        
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Batch Operations (${selectedItems.size} items)")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> batchCopy(selectedItems)
                    1 -> batchMove(selectedItems)
                    2 -> batchDelete(selectedItems)
                    3 -> batchCompress(selectedItems)
                    4 -> batchProperties(selectedItems)
                }
            }
            .show()
    }

    private fun batchCopy(items: List<FileItem>) {
        if (items.isEmpty()) return
        CopyMoveDialog(items, true) {
            viewModel.loadPath(viewModel.currentPath.value ?: return@CopyMoveDialog)
            exitMultiSelectMode()
            showMd3Dialog("Success", "Copied ${items.size} item(s)")
        }.show(parentFragmentManager, "copy_move_dialog")
    }

    private fun batchMove(items: List<FileItem>) {
        if (items.isEmpty()) return
        CopyMoveDialog(items, false) {
            viewModel.loadPath(viewModel.currentPath.value ?: return@CopyMoveDialog)
            exitMultiSelectMode()
            showMd3Dialog("Success", "Moved ${items.size} item(s)")
        }.show(parentFragmentManager, "copy_move_dialog")
    }

    private fun batchDelete(items: List<FileItem>) {
        if (items.isEmpty()) return
        showMd3ConfirmDialog(
            title = "Batch Delete",
            message = "Delete ${items.size} item(s)?"
        ) {
            var successCount = 0
            items.forEach { item ->
                if (viewModel.deleteFile(item)) successCount++
            }
            exitMultiSelectMode()
            showMd3Dialog("Success", "Deleted $successCount item(s)")
        }
    }

    private fun batchCompress(items: List<FileItem>) {
        if (items.isEmpty()) return
        
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = "Archive name"
            setPadding(48, 24, 48, 8)
        }
        val input = TextInputEditText(inputLayout.context)
        input.setText("archive.zip")
        inputLayout.addView(input)
        
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Batch Compress")
            .setMessage("Compress ${items.size} item(s) into ZIP")
            .setView(inputLayout)
            .setPositiveButton("Compress") { _, _ ->
                val archiveName = input.text?.toString()?.trim() ?: "archive.zip"
                if (!archiveName.endsWith(".zip")) {
                    showMd3Dialog("Error", "Name must end with .zip")
                    return@setPositiveButton
                }
                
                val currentPath = viewModel.currentPath.value ?: return@setPositiveButton
                val outputFile = File(currentPath, archiveName)
                val zipHelper = ZipHelper()
                
                val progressDialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                    .setTitle("Compressing...")
                    .setMessage("Please wait")
                    .setCancelable(false)
                    .create()
                progressDialog.show()
                
                zipHelper.zipFiles(
                    items.map { it.file },
                    outputFile,
                    object : ZipHelper.ZipCallback {
                        override fun onProgress(current: Int, total: Int) {
                            progressDialog.setMessage("Compressing: $current / $total")
                        }
                        
                        override fun onComplete(success: Boolean, message: String) {
                            progressDialog.dismiss()
                            showMd3Dialog(if (success) "Success" else "Error", message)
                            if (success) {
                                viewModel.loadPath(currentPath)
                                exitMultiSelectMode()
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun batchProperties(items: List<FileItem>) {
        // 计算总大小
        var totalSize = 0L
        var fileCount = 0
        var folderCount = 0
        
        items.forEach { item ->
            if (item.isDirectory) {
                folderCount++
                totalSize += getFolderSize(item.file)
            } else {
                fileCount++
                totalSize += item.size
            }
        }
        
        val sizeText = when {
            totalSize < 1024 -> "$totalSize B"
            totalSize < 1024 * 1024 -> "%.1f KB".format(totalSize / 1024.0)
            totalSize < 1024 * 1024 * 1024 -> "%.1f MB".format(totalSize / (1024.0 * 1024))
            else -> "%.2f GB".format(totalSize / (1024.0 * 1024 * 1024))
        }
        
        val message = """
            Total items: ${items.size}
            Folders: $folderCount
            Files: $fileCount
            Total size: $sizeText
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Batch Properties")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun getFolderSize(dir: File): Long {
        var size = 0L
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    size += getFolderSize(file)
                } else {
                    size += file.length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    private fun enterMultiSelectMode() {
        activity?.findViewById<FloatingActionButton>(R.id.fab)?.hide()
        requireActivity().invalidateOptionsMenu()
    }

    private fun exitMultiSelectMode() {
        adapter.enableMultiSelectMode(false)
        activity?.findViewById<FloatingActionButton>(R.id.fab)?.show()
        requireActivity().invalidateOptionsMenu()
        if (!isInSearchMode) {
            activity?.title = viewModel.currentPath.value?.substringAfterLast("/")?.ifEmpty { "Files" } ?: "Files"
        }
    }

    private fun copySelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) return
        CopyMoveDialog(selectedItems, true) {
            viewModel.loadPath(viewModel.currentPath.value ?: return@CopyMoveDialog)
            exitMultiSelectMode()
            showMd3Dialog("Success", "Copied ${selectedItems.size} item(s)")
        }.show(parentFragmentManager, "copy_move_dialog")
    }

    private fun moveSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) return
        CopyMoveDialog(selectedItems, false) {
            viewModel.loadPath(viewModel.currentPath.value ?: return@CopyMoveDialog)
            exitMultiSelectMode()
            showMd3Dialog("Success", "Moved ${selectedItems.size} item(s)")
        }.show(parentFragmentManager, "copy_move_dialog")
    }

    private fun deleteSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) return
        showMd3ConfirmDialog(
            title = "Delete",
            message = "Delete ${selectedItems.size} item(s)?"
        ) {
            var successCount = 0
            selectedItems.forEach { item ->
                if (viewModel.deleteFile(item)) successCount++
            }
            exitMultiSelectMode()
            showMd3Dialog("Success", "Deleted $successCount item(s)")
        }
    }

    private fun compressFiles(items: List<FileItem>) {
        if (items.isEmpty()) return
        
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = "Archive name"
            setPadding(48, 24, 48, 8)
        }
        val input = TextInputEditText(inputLayout.context)
        input.setText("archive.zip")
        inputLayout.addView(input)
        
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Compress Files")
            .setMessage("Create ZIP archive from ${items.size} item(s)")
            .setView(inputLayout)
            .setPositiveButton("Compress") { _, _ ->
                val archiveName = input.text?.toString()?.trim() ?: "archive.zip"
                if (!archiveName.endsWith(".zip")) {
                    showMd3Dialog("Error", "Name must end with .zip")
                    return@setPositiveButton
                }
                
                val currentPath = viewModel.currentPath.value ?: return@setPositiveButton
                val outputFile = File(currentPath, archiveName)
                val zipHelper = ZipHelper()
                
                val progressDialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                    .setTitle("Compressing...")
                    .setMessage("Please wait")
                    .setCancelable(false)
                    .create()
                progressDialog.show()
                
                zipHelper.zipFiles(
                    items.map { it.file },
                    outputFile,
                    object : ZipHelper.ZipCallback {
                        override fun onProgress(current: Int, total: Int) {
                            progressDialog.setMessage("Compressing: $current / $total")
                        }
                        
                        override fun onComplete(success: Boolean, message: String) {
                            progressDialog.dismiss()
                            showMd3Dialog(if (success) "Success" else "Error", message)
                            if (success) {
                                viewModel.loadPath(currentPath)
                                if (adapter.isMultiSelectEnabled()) exitMultiSelectMode()
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun extractZipFile(zipItem: FileItem) {
        if (zipItem.extension.lowercase() != "zip") return
        
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = "Destination folder name"
            setPadding(48, 24, 48, 8)
        }
        val input = TextInputEditText(inputLayout.context)
        input.setText(zipItem.name.replace(".zip", ""))
        inputLayout.addView(input)
        
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Extract Archive")
            .setMessage("Extract to: ${zipItem.name}")
            .setView(inputLayout)
            .setPositiveButton("Extract") { _, _ ->
                val folderName = input.text?.toString()?.trim() ?: zipItem.name.replace(".zip", "")
                val currentPath = viewModel.currentPath.value ?: return@setPositiveButton
                val destDir = File(currentPath, folderName)
                val zipHelper = ZipHelper()
                
                val progressDialog = MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                    .setTitle("Extracting...")
                    .setMessage("Please wait")
                    .setCancelable(false)
                    .create()
                progressDialog.show()
                
                zipHelper.unzipFile(
                    zipItem.file,
                    destDir,
                    object : ZipHelper.ZipCallback {
                        override fun onProgress(current: Int, total: Int) {
                            progressDialog.setMessage("Extracting: $current / $total")
                        }
                        
                        override fun onComplete(success: Boolean, message: String) {
                            progressDialog.dismiss()
                            showMd3Dialog(if (success) "Success" else "Error", message)
                            if (success) {
                                viewModel.loadPath(currentPath)
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProperties(item: FileItem) {
        PropertiesDialog(item).show(parentFragmentManager, "properties_dialog")
    }

    // MD3 大圆角弹窗
    private fun showMd3Dialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showMd3ConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.files.observe(viewLifecycleOwner) { files ->
            if (!isInSearchMode) {
                adapter.submitList(files)
                binding.emptyView.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
                binding.searchResultsHeader.visibility = View.GONE
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results == null) {
                isInSearchMode = false
                binding.searchResultsHeader.visibility = View.GONE
                viewModel.files.value?.let { files ->
                    adapter.submitList(files)
                    binding.emptyView.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
                }
            } else {
                isInSearchMode = true
                adapter.submitList(results)
                val count = results.size
                binding.searchResultsHeader.visibility = View.VISIBLE
                binding.searchResultsHeader.text = "$count result${if (count != 1) "s" else ""} found"
                binding.emptyView.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                if (results.isEmpty()) binding.emptyView.text = getString(R.string.label_no_search_results)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.currentPath.observe(viewLifecycleOwner) { path ->
            if (!isInSearchMode && !adapter.isMultiSelectEnabled()) {
                activity?.title = path.substringAfterLast("/").ifEmpty { "Files" }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = if (viewModel.isGridView) {
            GridLayoutManager(requireContext(), 3)
        } else {
            LinearLayoutManager(requireContext())
        }
        binding.recyclerView.setHasFixedSize(true)
    }

    private fun checkPermissionsAndLoad() {
        if (hasStoragePermission()) {
            loadFiles()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showManageStorageRationale()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showManageStorageRationale() {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.label_permission_required)
            .setMessage(R.string.label_permission_required)
            .setPositiveButton(R.string.btn_grant_permission) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showPermissionDenied() {
        binding.permissionView.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.permissionButton.setOnClickListener { checkPermissionsAndLoad() }
    }

    private fun loadFiles() {
        binding.permissionView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        if (viewModel.currentPath.value == null) {
            viewModel.loadRoot()
        }
    }

    private fun handleClick(item: FileItem) {
        if (isInSearchMode && item.isDirectory) {
            searchView?.setQuery("", false)
            searchView?.isIconified = true
            viewModel.clearSearch()
            viewModel.loadPath(item.path)
        } else if (item.isDirectory) {
            viewModel.navigateTo(item)
        } else {
            openFile(item)
        }
    }

    private fun openFile(item: FileItem) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                item.file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, item.name))
        } catch (e: Exception) {
            showMd3Dialog("Error", getString(R.string.error_cannot_open))
        }
    }

    private fun showContextMenu(item: FileItem, anchor: View?) {
        val options = mutableListOf<String>()
        options.add(getString(R.string.action_open))
        options.add(getString(R.string.action_copy))
        options.add(getString(R.string.action_move))
        
        if (item.extension.lowercase() == "zip") {
            options.add("Extract")
        } else {
            options.add("Compress")
        }
        
        options.add(getString(R.string.action_rename))
        options.add(getString(R.string.action_delete))
        options.add(getString(R.string.action_share))
        options.add(getString(R.string.action_properties))
        options.add(if (prefs.isFavorite(item.path)) getString(R.string.action_remove_favorite) else getString(R.string.action_add_favorite))
        
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(item.name)
            .setItems(options.toTypedArray()) { _, which ->
                val selectedOption = options[which]
                when {
                    selectedOption == "Extract" -> extractZipFile(item)
                    selectedOption == "Compress" -> compressFiles(listOf(item))
                    selectedOption == getString(R.string.action_open) -> handleClick(item)
                    selectedOption == getString(R.string.action_copy) -> {
                        CopyMoveDialog(listOf(item), true) {
                            viewModel.loadPath(viewModel.currentPath.value ?: return@CopyMoveDialog)
                            showMd3Dialog("Success", "Copied")
                        }.show(parentFragmentManager, "copy_move_dialog")
                    }
                    selectedOption == getString(R.string.action_move) -> {
                        CopyMoveDialog(listOf(item), false) {
                            viewModel.loadPath(viewModel.currentPath.value ?: return@CopyMoveDialog)
                            showMd3Dialog("Success", "Moved")
                        }.show(parentFragmentManager, "copy_move_dialog")
                    }
                    selectedOption == getString(R.string.action_rename) -> showRenameDialog(item)
                    selectedOption == getString(R.string.action_delete) -> showDeleteDialog(item)
                    selectedOption == getString(R.string.action_share) -> shareFile(item)
                    selectedOption == getString(R.string.action_properties) -> showProperties(item)
                    selectedOption == getString(R.string.action_add_favorite) -> toggleFavorite(item)
                    selectedOption == getString(R.string.action_remove_favorite) -> toggleFavorite(item)
                }
            }
            .show()
    }

    private fun showCreateDialog() {
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = getString(R.string.dialog_new_folder_hint)
            setPadding(48, 24, 48, 8)
        }
        val input = TextInputEditText(inputLayout.context)
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(getString(R.string.dialog_create_title))
            .setView(inputLayout)
            .setNeutralButton(getString(R.string.dialog_cancel), null)
            .setNegativeButton("File") { _, _ ->
                val name = input.text?.toString()?.trim() ?: return@setNegativeButton
                if (name.isNotEmpty()) createFile(name)
            }
            .setPositiveButton("Folder") { _, _ ->
                val name = input.text?.toString()?.trim() ?: return@setPositiveButton
                if (name.isNotEmpty()) {
                    val ok = viewModel.createFolder(name)
                    showMd3Dialog(
                        if (ok) "Success" else "Error",
                        if (ok) getString(R.string.msg_folder_created) else getString(R.string.error_rename_failed)
                    )
                }
            }
            .show()
    }

    private fun createFile(name: String) {
        val parent = viewModel.currentPath.value ?: return
        val newFile = File(parent, name)
        try {
            val ok = newFile.createNewFile()
            showMd3Dialog(
                if (ok) "Success" else "Error",
                if (ok) "File created" else "Failed"
            )
            if (ok) viewModel.loadPath(parent)
        } catch (e: Exception) {
            showMd3Dialog("Error", "Failed")
        }
    }

    private fun showRenameDialog(item: FileItem) {
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = getString(R.string.dialog_rename_title)
            setPadding(48, 24, 48, 8)
        }
        val input = TextInputEditText(inputLayout.context)
        input.setText(item.name)
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.dialog_rename_title)
            .setView(inputLayout)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                val newName = input.text?.toString()?.trim() ?: return@setPositiveButton
                if (newName.isNotEmpty()) {
                    val ok = viewModel.renameFile(item, newName)
                    showMd3Dialog(
                        if (ok) "Success" else "Error",
                        if (ok) getString(R.string.msg_renamed) else getString(R.string.error_rename_failed)
                    )
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showDeleteDialog(item: FileItem) {
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, item.name))
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                val ok = viewModel.deleteFile(item)
                showMd3Dialog(
                    if (ok) "Success" else "Error",
                    if (ok) getString(R.string.msg_deleted) else getString(R.string.error_delete_failed)
                )
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun shareFile(item: FileItem) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                item.file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType()
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, item.name))
        } catch (e: Exception) {
            showMd3Dialog("Error", getString(R.string.error_cannot_open))
        }
    }

    private fun toggleFavorite(item: FileItem) {
        if (prefs.isFavorite(item.path)) {
            prefs.removeFavorite(item.path)
            showMd3Dialog("Success", getString(R.string.action_remove_favorite))
        } else {
            prefs.addFavorite(item.path)
            showMd3Dialog("Success", getString(R.string.action_add_favorite))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        if (adapter.isMultiSelectEnabled()) {
            inflater.inflate(R.menu.menu_multi_select, menu)
        } else {
            inflater.inflate(R.menu.menu_files, menu)

            val searchItem = menu.findItem(R.id.action_search)
            searchView = searchItem?.actionView as? SearchView
            searchView?.apply {
                queryHint = getString(R.string.action_search)
                maxWidth = Integer.MAX_VALUE

                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        query?.let { viewModel.search(it) }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (newText.isNullOrBlank()) {
                            viewModel.clearSearch()
                        } else {
                            viewModel.search(newText)
                        }
                        return true
                    }
                })

                setOnCloseListener {
                    viewModel.clearSearch()
                    false
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select_all -> {
                adapter.selectAll()
                true
            }
            R.id.action_compress -> {
                compressFiles(adapter.getSelectedItems())
                true
            }
            R.id.action_copy -> {
                copySelectedFiles()
                true
            }
            R.id.action_move -> {
                moveSelectedFiles()
                true
            }
            R.id.action_delete -> {
                deleteSelectedFiles()
                true
            }
            R.id.action_sort -> { showSortDialog(); true }
            R.id.action_view_toggle -> {
                viewModel.isGridView = !viewModel.isGridView
                prefs.isGridView = viewModel.isGridView
                item.setIcon(
                    if (viewModel.isGridView) R.drawable.ic_list else R.drawable.ic_grid
                )
                setupRecyclerView()
                true
            }
            R.id.action_theme -> {
                val newMode = when (prefs.themeMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
                    AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else -> AppCompatDelegate.MODE_NIGHT_NO
                }
                prefs.themeMode = newMode
                AppCompatDelegate.setDefaultNightMode(newMode)
                true
            }
            else -> false
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(
            getString(R.string.sort_name_asc),
            getString(R.string.sort_name_desc),
            getString(R.string.sort_date_newest),
            getString(R.string.sort_date_oldest),
            getString(R.string.sort_size_largest),
            getString(R.string.sort_size_smallest)
        )
        val orders = SortOrder.values()
        val current = orders.indexOf(viewModel.sortOrder).let { if (it < 0) 0 else it }
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.action_sort)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.applySortOrder(orders[which])
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}