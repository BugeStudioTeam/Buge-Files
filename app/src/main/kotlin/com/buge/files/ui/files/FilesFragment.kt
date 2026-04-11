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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            onItemLongClick = { showContextMenu(it, null) },
            onMoreClick = { item, anchor -> showContextMenu(item, anchor) }
        )

        setupRecyclerView()
        observeViewModel()

        // 返回键：先退出搜索，再返回上级目录
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
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
        showCreateDialog()
    }

    private fun observeViewModel() {
        // 普通文件列表
        viewModel.files.observe(viewLifecycleOwner) { files ->
            if (!isInSearchMode) {
                adapter.submitList(files)
                binding.emptyView.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
                binding.searchResultsHeader.visibility = View.GONE
            }
        }

        // 搜索结果
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results == null) {
                // 搜索已清除，显示普通列表
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
            if (!isInSearchMode) {
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
        MaterialAlertDialogBuilder(requireContext())
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
            // 搜索结果中点击文件夹：退出搜索并导航到该目录
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
            Toast.makeText(requireContext(), R.string.error_cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showContextMenu(item: FileItem, anchor: View?) {
        val options = mutableListOf(
            getString(R.string.action_open),
            getString(R.string.action_rename),
            getString(R.string.action_delete),
            getString(R.string.action_share),
            if (prefs.isFavorite(item.path)) getString(R.string.action_remove_favorite)
            else getString(R.string.action_add_favorite)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> handleClick(item)
                    1 -> showRenameDialog(item)
                    2 -> showDeleteDialog(item)
                    3 -> shareFile(item)
                    4 -> toggleFavorite(item)
                }
            }.show()
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

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_create_title))
            .setView(inputLayout)
            .setNeutralButton(getString(R.string.dialog_cancel), null)
            .setNegativeButton(getString(R.string.dialog_create_file)) { _, _ ->
                val name = input.text?.toString()?.trim() ?: return@setNegativeButton
                if (name.isNotEmpty()) createFile(name)
            }
            .setPositiveButton(getString(R.string.dialog_create_folder)) { _, _ ->
                val name = input.text?.toString()?.trim() ?: return@setPositiveButton
                if (name.isNotEmpty()) {
                    val ok = viewModel.createFolder(name)
                    Toast.makeText(
                        requireContext(),
                        if (ok) R.string.msg_folder_created else R.string.error_rename_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun createFile(name: String) {
        val parent = viewModel.currentPath.value ?: return
        val newFile = File(parent, name)
        try {
            val ok = newFile.createNewFile()
            Toast.makeText(
                requireContext(),
                if (ok) R.string.msg_file_created else R.string.error_rename_failed,
                Toast.LENGTH_SHORT
            ).show()
            if (ok) viewModel.loadPath(parent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.error_rename_failed, Toast.LENGTH_SHORT).show()
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

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_rename_title)
            .setView(inputLayout)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                val newName = input.text?.toString()?.trim() ?: return@setPositiveButton
                if (newName.isNotEmpty()) {
                    val ok = viewModel.renameFile(item, newName)
                    Toast.makeText(
                        requireContext(),
                        if (ok) R.string.msg_renamed else R.string.error_rename_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showDeleteDialog(item: FileItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, item.name))
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                val ok = viewModel.deleteFile(item)
                Toast.makeText(
                    requireContext(),
                    if (ok) R.string.msg_deleted else R.string.error_delete_failed,
                    Toast.LENGTH_SHORT
                ).show()
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
            Toast.makeText(requireContext(), R.string.error_cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFavorite(item: FileItem) {
        if (prefs.isFavorite(item.path)) {
            prefs.removeFavorite(item.path)
            Toast.makeText(requireContext(), R.string.action_remove_favorite, Toast.LENGTH_SHORT).show()
        } else {
            prefs.addFavorite(item.path)
            Toast.makeText(requireContext(), R.string.action_add_favorite, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_files, menu)

        // 绑定 SearchView
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
                        activity?.title = viewModel.currentPath.value
                            ?.substringAfterLast("/")?.ifEmpty { "Files" } ?: "Files"
                    } else {
                        activity?.title = getString(R.string.action_search)
                        viewModel.search(newText)
                    }
                    return true
                }
            })

            // 关闭搜索时清除结果
            setOnCloseListener {
                viewModel.clearSearch()
                activity?.title = viewModel.currentPath.value
                    ?.substringAfterLast("/")?.ifEmpty { "Files" } ?: "Files"
                false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
        MaterialAlertDialogBuilder(requireContext())
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