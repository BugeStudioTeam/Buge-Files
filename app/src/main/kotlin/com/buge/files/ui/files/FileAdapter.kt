package com.buge.files.ui.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val onMoreClick: (FileItem, View) -> Unit
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(a: FileItem, b: FileItem) = a.path == b.path
            override fun areContentsTheSame(a: FileItem, b: FileItem) = a == b
        }
        private val DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
    }

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        // 只有 archive（压缩包）保留独立颜色，其余全部用 colorOnSurfaceVariant
        val isArchive = item.extension in listOf("zip", "rar", "7z", "tar", "gz")

        if (isArchive) {
            // 压缩包：保持 drawable 里定义的棕色，不覆盖 tint
            holder.icon.imageTintList = null
        } else {
            // 其余所有类型（包括文件夹、图片、视频、音频、PDF、APK、文本等）
            // 统一使用跟随主题的中性色：浅色=灰，深色=白
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

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener { onItemLongClick(item); true }
        holder.btnMore.setOnClickListener { onMoreClick(item, it) }
    }
}