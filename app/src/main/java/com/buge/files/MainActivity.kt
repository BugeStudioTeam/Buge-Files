package com.buge.files

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: BugeViewModel by viewModels()
    private var awaitingAllFilesAccess = false
    private var selectMode = false
    private var saveMode = false

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            viewModel.addRoot(uri)
        } catch (_: SecurityException) {
            Toast.makeText(this, "Folder access could not be saved", Toast.LENGTH_LONG).show()
        }
    }

    private val legacyStoragePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) viewModel.refreshDirectStorageAccess(preferDirect = true)
        else Toast.makeText(this, "Storage permission is required to manage internal storage", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectMode = intent?.action == Intent.ACTION_GET_CONTENT || intent?.action == Intent.ACTION_OPEN_DOCUMENT
        saveMode = intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE
        enableEdgeToEdge()
        setContent {
            BugeApp(
                viewModel = viewModel,
                onRequestFolder = { folderPicker.launch(null) },
                onRequestDirectStorage = ::requestDirectStorageAccess,
                onOpenFile = ::openFile,
                onInstallApk = ::installApk,
                onShareFiles = ::shareFiles,
                pickOnly = selectMode,
                saveIncoming = saveMode,
                incomingUris = if (saveMode) collectIncomingUris(intent) else emptyList(),
                onSaveIncoming = { destination, finishAfter ->
                    val result = viewModel.saveIncoming(collectIncomingUris(intent), destination)
                    result?.let {
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                        if (it.success && finishAfter) finish()
                    }
                }
            )
        }
    }

    private fun collectIncomingUris(source: Intent?): List<Uri> {
        source ?: return emptyList()
        return when (source.action) {
            Intent.ACTION_SEND -> listOfNotNull(source.parcelableExtra<Uri>(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> source.parcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> emptyList()
        }
    }

    private inline fun <reified T : android.os.Parcelable> Intent.parcelableExtra(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableExtra(key, T::class.java)
        else @Suppress("DEPRECATION") getParcelableExtra(key) as? T

    private inline fun <reified T : android.os.Parcelable> Intent.parcelableArrayListExtra(key: String): ArrayList<T>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableArrayListExtra(key, T::class.java)
        else @Suppress("DEPRECATION") getParcelableArrayListExtra(key)



    override fun onResume() {
        super.onResume()
        if (awaitingAllFilesAccess) {
            awaitingAllFilesAccess = false
            viewModel.refreshDirectStorageAccess(preferDirect = true)
        }
    }

    private fun requestDirectStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            awaitingAllFilesAccess = true
            val appUri = Uri.parse("package:$packageName")
            runCatching {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, appUri))
            }.onFailure {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            val needed = buildList {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (needed.isEmpty()) viewModel.refreshDirectStorageAccess(preferDirect = true)
            else legacyStoragePermission.launch(needed.toTypedArray())
        }
    }

    private fun openFile(file: FileEntry) {
        if (selectMode) {
            deliverSelection(file)
            return
        }
        val uri = safeUri(file) ?: run {
            Toast.makeText(this, "The selected file is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mimeType ?: contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(Intent.createChooser(intent, "Open with")) }
            .onFailure { Toast.makeText(this, "No compatible app found", Toast.LENGTH_SHORT).show() }
    }

    private fun deliverSelection(file: FileEntry) {
        if (file.isDirectory) return
        val uri = safeUri(file) ?: run {
            Toast.makeText(this, "The selected file is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val caller = referrer?.authority ?: callingPackage
            if (caller != null) {
                runCatching { grantUriPermission(caller, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
        }
        val result = Intent().apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private fun installApk(file: FileEntry) {
        val uri = safeUri(file) ?: run {
            Toast.makeText(this, "The APK is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, "Allow Buge Files to install unknown apps, then tap Install again", Toast.LENGTH_LONG).show()
            runCatching { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
                .onFailure { Toast.makeText(this, "This device does not expose the install-source setting", Toast.LENGTH_LONG).show() }
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        runCatching { startActivity(intent) }
            .onFailure { Toast.makeText(this, "No system package installer is available", Toast.LENGTH_LONG).show() }
    }

    private fun shareFiles(files: List<FileEntry>) {
        if (files.isEmpty()) return
        val uris = files.mapNotNull(::safeUri)
        if (uris.isEmpty()) {
            Toast.makeText(this, "No selected files are available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = files.first().mimeType ?: contentResolver.getType(uris.first()) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { startActivity(Intent.createChooser(intent, "Share with")) }
            .onFailure { Toast.makeText(this, "No compatible sharing app found", Toast.LENGTH_SHORT).show() }
    }

    private fun safeUri(file: FileEntry): Uri? = if (file.uri.scheme == "file") {
        val local = file.uri.path?.let(::File)?.takeIf { it.exists() } ?: return null
        FileProvider.getUriForFile(this, "$packageName.fileprovider", local)
    } else file.uri
}
