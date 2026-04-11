package com.buge.files.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.buge.files.R
import com.buge.files.databinding.FragmentFavoritesBinding
import com.buge.files.model.FileItem
import com.buge.files.ui.files.FileAdapter
import com.buge.files.util.PrefsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PrefsManager
    private lateinit var adapter: FileAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())

        adapter = FileAdapter(
            onItemClick = { handleClick(it) },
            onItemLongClick = { showContextMenu(it) },
            onMoreClick = { item, _ -> showContextMenu(item) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        val items = prefs.favoritesPaths
            .map { File(it) }
            .filter { it.exists() }
            .map { FileItem(it) }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        adapter.submitList(items)
        binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun handleClick(item: FileItem) {
        if (item.isDirectory) {
            // 可跳转到 FilesFragment 并打开该目录（此处简单提示）
            Toast.makeText(requireContext(), item.path, Toast.LENGTH_SHORT).show()
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

    private fun showContextMenu(item: FileItem) {
        val options = arrayOf(
            getString(R.string.action_open),
            getString(R.string.action_remove_favorite)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> handleClick(item)
                    1 -> {
                        prefs.removeFavorite(item.path)
                        loadFavorites()
                        Toast.makeText(
                            requireContext(),
                            R.string.action_remove_favorite,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}