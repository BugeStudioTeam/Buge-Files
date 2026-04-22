package com.buge.files.ui.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buge.files.R
import com.buge.files.model.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit,
    private val onMoreClick: (FileItem, View) -> Unit,
    private val onSelectionChanged: (Set<FileItem>) -> Unit = {}
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(a: FileItem, b: FileItem) = a.path == b.path
            override fun areContentsTheSame(a: FileItem, b: FileItem) = a == b
        }
        private val DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
    }

    private var multiSelectEnabled = false
    private val selectedItems = mutableSetOf<String>()

    fun enableMultiSelectMode(enabled: Boolean) {
        multiSelectEnabled = enabled
        if (!enabled) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
        if (!enabled) {
            onSelectionChanged(emptySet())
        }
    }

    fun getSelectedItems(): List<FileItem> = currentList.filter { selectedItems.contains(it.path) }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(currentList.map { it.path })
        notifyDataSetChanged()
        onSelectionChanged(getSelectedItems().toSet())
    }

    fun isMultiSelectEnabled() = multiSelectEnabled

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val icon: ImageView = view.findViewById(R.id.file_icon)
        val name: TextView = view.findViewById(R.id.file_name)
        val info: TextView = view.findViewById(R.id.file_info)
        val btnMore: ImageView = view.findViewById(R.id.btn_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.icon.setImageResource(item.iconRes())

        val isArchive = item.extension in listOf("zip", "rar", "7z", "tar", "gz")

        if (isArchive) {
            holder.icon.imageTintList = null
        } else {
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                typedValue,
                true
            )
            holder.icon.imageTintList =
                android.content.res.ColorStateList.valueOf(typedValue.data)
        }

        holder.name.text = item.name
        val date = DATE_FORMAT.format(Date(item.lastModified))
        holder.info.text = if (item.isDirectory) date
        else "${item.formattedSize()}  ·  $date"

        // 多选模式UI
        if (multiSelectEnabled) {
            holder.checkbox.visibility = View.VISIBLE
            holder.btnMore.visibility = View.GONE
            holder.checkbox.isChecked = selectedItems.contains(item.path)
        } else {
            holder.checkbox.visibility = View.GONE
            holder.btnMore.visibility = View.VISIBLE
        }

        // 单击事件
        holder.itemView.setOnClickListener {
            if (multiSelectEnabled) {
                // 多选模式下：切换选中状态
                if (selectedItems.contains(item.path)) {
                    selectedItems.remove(item.path)
                } else {
                    selectedItems.add(item.path)
                }
                holder.checkbox.isChecked = selectedItems.contains(item.path)
                onSelectionChanged(getSelectedItems().toSet())
            } else {
                // 普通模式：打开文件/文件夹
                onItemClick(item)
            }
        }

        // 长按事件
        holder.itemView.setOnLongClickListener {
            if (!multiSelectEnabled) {
                // 普通模式下长按：进入多选模式并选中当前项
                enableMultiSelectMode(true)
                selectedItems.add(item.path)
                notifyDataSetChanged()
                onSelectionChanged(getSelectedItems().toSet())
                true
            } else {
                // 多选模式下长按：调用批量操作回调
                onItemLongClick(item)
                true
            }
        }

        // 更多按钮点击
        holder.btnMore.setOnClickListener { onMoreClick(item, it) }
    }
}