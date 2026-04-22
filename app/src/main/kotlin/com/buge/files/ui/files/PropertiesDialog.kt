package com.buge.files.ui.files

import android.app.Dialog
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.buge.files.R
import com.buge.files.model.FileItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PropertiesDialog(private val item: FileItem) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_properties, null, false)

        // 设置数据
        view.findViewById<TextView>(R.id.property_name_value).text = item.name
        view.findViewById<TextView>(R.id.property_path_value).text = item.path
        view.findViewById<TextView>(R.id.property_type_value).text = 
            if (item.isDirectory) "Folder" else "File"
        
        val sizeText = if (item.isDirectory) {
            val count = item.file.listFiles()?.size ?: 0
            "$count items"
        } else {
            Formatter.formatFileSize(requireContext(), item.size)
        }
        view.findViewById<TextView>(R.id.property_size_value).text = sizeText
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.getDefault())
        view.findViewById<TextView>(R.id.property_modified_value).text = 
            dateFormat.format(Date(item.lastModified))
        
        if (!item.isDirectory) {
            val extensionRow = view.findViewById<View>(R.id.extension_row)
            extensionRow.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.property_extension_value).text = 
                item.extension.ifEmpty { "None" }
        }

        return MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Properties")
            .setView(view)
            .setPositiveButton("OK", null)
            .create()
    }
}