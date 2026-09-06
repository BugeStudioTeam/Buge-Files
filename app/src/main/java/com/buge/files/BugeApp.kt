@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.buge.files

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BugeApp(
    viewModel: BugeViewModel,
    onRequestFolder: () -> Unit,
    onRequestDirectStorage: () -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onInstallApk: (FileEntry) -> Unit,
    onShareFiles: (List<FileEntry>) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val roots by viewModel.roots.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val currentRoot by viewModel.currentRoot.collectAsStateWithLifecycle()
    val directStorageAvailable by viewModel.directStorageAvailable.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val language = settings.language
    val snackbars = remember { SnackbarHostState() }
    var showRootMenu by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var showCreateFile by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var showCompress by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileEntry?>(null) }
    var deleteRequest by remember { mutableStateOf<List<FileEntry>?>(null) }
    val selected = viewModel.selectedEntries()

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbars.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    BackHandler(enabled = viewModel.selection.isNotEmpty() || viewModel.isSearching || viewModel.navigationPath.size > 1) {
        when {
            viewModel.selection.isNotEmpty() -> viewModel.clearSelection()
            viewModel.isSearching -> viewModel.setSearchActive(false)
            else -> viewModel.navigateUp()
        }
    }

    BugeTheme(settings) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    BugeNavigationRail(
                        destination = viewModel.destination,
                        language = language,
                        onSelect = viewModel::selectDestination
                    )
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = {
                        BugeTopBar(
                            language = language,
                            root = currentRoot,
                            roots = roots,
                            selectionCount = viewModel.selection.size,
                            isSearching = viewModel.isSearching,
                            query = viewModel.searchQuery,
                            viewMode = settings.viewMode,
                            sortOption = viewModel.sortOption,
                            ascending = viewModel.ascending,
                            onSearch = { viewModel.setSearchActive(true) },
                            onQueryChange = viewModel::updateSearch,
                            onCloseSearch = { viewModel.setSearchActive(false) },
                            onToggleRootMenu = { showRootMenu = !showRootMenu },
                            onSelectRoot = { viewModel.selectRoot(it); showRootMenu = false },
                            onShowActions = { showActions = true },
                            onChangeView = { viewModel.setViewMode(if (settings.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST) },
                            onClearSelection = viewModel::clearSelection,
                            onSelectAll = viewModel::selectAll,
                            onCopy = { viewModel.copySelected(ClipboardMode.COPY) },
                            onMove = { viewModel.copySelected(ClipboardMode.MOVE) },
                            onRename = { selected.singleOrNull()?.let { renameTarget = it } },
                            onCompress = { showCompress = true },
                            onShare = { onShareFiles(selected) },
                            onDelete = { deleteRequest = selected },
                            rootMenuExpanded = showRootMenu,
                            onAddLocation = onRequestFolder,
                            onRemoveRoot = viewModel::removeRoot
                        )
                    },
                    bottomBar = {
                        if (!wide) BugeNavigationBar(viewModel.destination, language, viewModel::selectDestination)
                    },
                    floatingActionButton = {
                        if (viewModel.destination == AppDestination.BROWSE && currentRoot != null && !viewModel.isSearching) {
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ExtendedFloatingActionButton(
                                    text = { Text(language.t("new_file")) },
                                    icon = { Icon(Icons.Outlined.Description, null) },
                                    onClick = { showCreateFile = true }
                                )
                                ExtendedFloatingActionButton(
                                    text = { Text(language.t("new_folder")) },
                                    icon = { Icon(Icons.Outlined.CreateNewFolder, null) },
                                    onClick = { showCreateFolder = true }
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbars) }
                ) { padding ->
                    AnimatedContent(
                        targetState = viewModel.destination,
                        transitionSpec = { (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut()) },
                        label = "destination"
                    ) { destination ->
                        when (destination) {
                            AppDestination.BROWSE -> BrowseScreen(
                                modifier = Modifier.padding(padding),
                                language = language,
                                root = currentRoot,
                                directStorageAvailable = directStorageAvailable,
                                path = viewModel.navigationPath,
                                entries = if (viewModel.searchQuery.isBlank()) entries else searchResults,
                                searchActive = viewModel.isSearching && viewModel.searchQuery.isNotBlank(),
                                isLoading = viewModel.isLoading,
                                viewMode = settings.viewMode,
                                compact = settings.compactMode,
                                clipboard = viewModel.clipboard,
                                isBookmarked = viewModel.navigationPath.lastOrNull()?.let(viewModel::isBookmarked) ?: false,
                                onRequestFolder = onRequestFolder,
                                onRequestDirectStorage = onRequestDirectStorage,
                                selectionActive = viewModel.selection.isNotEmpty(),
                                onOpenDirectory = viewModel::openDirectory,
                                onOpenFile = { if (!viewModel.openBuiltInTool(it)) { viewModel.recordOpened(it); onOpenFile(it) } },
                                onToggleSelection = viewModel::toggleSelection,
                                isSelected = viewModel::isSelected,
                                onInfo = viewModel::showInfo,
                                onToggleBookmark = { viewModel.navigationPath.lastOrNull()?.let(viewModel::toggleBookmark) },
                                onPaste = viewModel::paste
                            )
                            AppDestination.RECENTS -> RecentsScreen(
                                modifier = Modifier.padding(padding), language = language, items = viewModel.recents,
                                onOpen = { if (!viewModel.openBuiltInTool(it)) { viewModel.recordOpened(it); onOpenFile(it) } }, onInfo = viewModel::showInfo
                            )
                            AppDestination.FAVORITES -> FavoritesScreen(
                                modifier = Modifier.padding(padding), language = language, items = bookmarks,
                                onOpen = viewModel::selectRoot, onRemove = viewModel::toggleBookmark
                            )
                            AppDestination.STORAGE -> StorageScreen(
                                modifier = Modifier.padding(padding), language = language, root = currentRoot, data = storage,
                                loading = viewModel.isStorageLoading, onRequestFolder = onRequestFolder
                            )
                            AppDestination.SETTINGS -> SettingsScreen(
                                modifier = Modifier.padding(padding), language = language, settings = settings,
                                onSettingsChange = viewModel::updateSettings
                            )
                        }
                    }
                }
            }
        }

        if (showActions) {
            SortAndViewSheet(
                language = language, sortOption = viewModel.sortOption, ascending = viewModel.ascending,
                viewMode = settings.viewMode, showHidden = settings.showHidden,
                onSort = viewModel::setSort, onView = viewModel::setViewMode, onToggleHidden = viewModel::toggleHidden,
                onDismiss = { showActions = false }
            )
        }
        if (showCreateFile) {
            NameDialog(language.t("new_file"), language.t("file_name"), language, initialValue = "untitled.txt", onDismiss = { showCreateFile = false }) {
                viewModel.createFile(it); showCreateFile = false
            }
        }
        if (showCompress) {
            NameDialog(language.t("compress"), language.t("archive_name"), language, initialValue = "Buge Archive.zip", confirmLabel = language.t("compress"), onDismiss = { showCompress = false }) {
                viewModel.compressSelected(it); showCompress = false
            }
        }
        if (showCreateFolder) {
            NameDialog(language.t("new_folder"), language.t("folder_name"), language, onDismiss = { showCreateFolder = false }) {
                viewModel.createFolder(it); showCreateFolder = false
            }
        }
        renameTarget?.let { target ->
            NameDialog(language.t("rename"), target.name, language, initialValue = target.name, confirmLabel = language.t("rename"), onDismiss = { renameTarget = null }) {
                viewModel.rename(target, it); renameTarget = null
            }
        }
        deleteRequest?.let { targets ->
            AlertDialog(
                onDismissRequest = { deleteRequest = null },
                icon = { Icon(Icons.Outlined.DeleteOutline, null) },
                title = { Text(language.t("delete")) },
                text = { Text(if (targets.size == 1) "${targets.first().name}" else "${targets.size} ${language.t("items")}") },
                confirmButton = { TextButton(onClick = { viewModel.delete(targets); deleteRequest = null }) { Text(language.t("delete")) } },
                dismissButton = { TextButton(onClick = { deleteRequest = null }) { Text(language.t("cancel")) } }
            )
        }
        viewModel.apkTarget?.let { file ->
            ApkInspectorSheet(
                file = file,
                metadata = viewModel.apkMetadata,
                loading = viewModel.apkLoading,
                language = language,
                settings = settings,
                shizukuReady = viewModel.shizukuReady.value,
                isInstalling = viewModel.isInstallingViaShizuku,
                onInstall = { onInstallApk(file) },
                onInstallWithShizuku = { viewModel.installApkWithShizuku(file) },
                onDismiss = viewModel::dismissApk
            )
        }
        viewModel.editorTarget?.let { file ->
            CodeEditorScreen(file = file, content = viewModel.editorText, loading = viewModel.editorLoading, language = language, onContentChange = viewModel::updateEditorText, onSave = viewModel::saveEditor, onDismiss = viewModel::dismissEditor)
        }
        viewModel.archiveTarget?.let { file ->
            ArchiveBrowserSheet(file = file, items = viewModel.archiveItems, loading = viewModel.archiveLoading, language = language, onExtract = viewModel::extractArchiveToCurrentLocation, onDismiss = viewModel::dismissArchive)
        }
        viewModel.imagePreview?.let { file ->
            ImageViewerScreen(file = file, language = language, onShare = { onShareFiles(listOf(file)) }, onDismiss = viewModel::dismissImage)
        }
        viewModel.checksumTarget?.let { file ->
            ChecksumSheet(file = file, value = viewModel.checksumValue, loading = viewModel.checksumLoading, language = language, onDismiss = viewModel::dismissChecksum)
        }
        viewModel.pendingInfo?.let { file ->
            FileDetailsSheet(
                file = file, language = language,
                onDismiss = viewModel::dismissInfo,
                onOpen = { viewModel.recordOpened(file); onOpenFile(file); viewModel.dismissInfo() },
                onOpenTool = { viewModel.openBuiltInTool(file); viewModel.dismissInfo() },
                onChecksum = { viewModel.calculateChecksum(file); viewModel.dismissInfo() },
                onShare = { onShareFiles(listOf(file)) },
                onRename = { renameTarget = file; viewModel.dismissInfo() },
                onDelete = { deleteRequest = listOf(file); viewModel.dismissInfo() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BugeTopBar(
    language: AppLanguage, root: RootLocation?, roots: List<RootLocation>, selectionCount: Int,
    isSearching: Boolean, query: String, viewMode: ViewMode, sortOption: SortOption, ascending: Boolean,
    onSearch: () -> Unit, onQueryChange: (String) -> Unit, onCloseSearch: () -> Unit,
    onToggleRootMenu: () -> Unit, onSelectRoot: (RootLocation) -> Unit, onShowActions: () -> Unit,
    onChangeView: () -> Unit, onClearSelection: () -> Unit, onSelectAll: () -> Unit,
    onCopy: () -> Unit, onMove: () -> Unit, onRename: () -> Unit, onCompress: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit,
    rootMenuExpanded: Boolean, onAddLocation: () -> Unit, onRemoveRoot: (RootLocation) -> Unit
) {
    val title = when { selectionCount > 0 -> "$selectionCount ${language.t("items")}"; isSearching -> ""; else -> "Buge Files" }
    var selectionMenu by remember { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        title = {
            if (isSearching) {
                OutlinedTextField(
                    value = query, onValueChange = onQueryChange, singleLine = true,
                    placeholder = { Text(language.t("search")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                )
            } else Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            when {
                selectionCount > 0 -> IconButton(onClick = onClearSelection) { Icon(Icons.Outlined.Close, language.t("done")) }
                else -> Box {
                    FilledIconButton(onClick = onToggleRootMenu) { Icon(Icons.Outlined.FolderOpen, language.t("locations")) }
                    DropdownMenu(expanded = rootMenuExpanded, onDismissRequest = onToggleRootMenu, modifier = Modifier.widthIn(min = 260.dp, max = 360.dp)) {
                        Text(language.t("locations"), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                        roots.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = { Icon(if (location.uri == root?.uri) Icons.Outlined.FolderOpen else Icons.Outlined.Folder, null) },
                                trailingIcon = { if (location.uri.scheme != "file") IconButton(onClick = { onRemoveRoot(location) }) { Icon(Icons.Outlined.Close, language.t("remove")) } },
                                onClick = { onSelectRoot(location) }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(language.t("add_location")) }, leadingIcon = { Icon(Icons.Outlined.Add, null) }, onClick = onAddLocation)
                    }
                }
            }
        },
        actions = {
            when {
                selectionCount > 0 -> {
                    IconButton(onClick = onSelectAll) { Icon(Icons.Outlined.SelectAll, language.t("select_all")) }
                    IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, language.t("copy")) }
                    IconButton(onClick = onMove) { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, language.t("move")) }
                    Box {
                        IconButton(onClick = { selectionMenu = true }) { Icon(Icons.Outlined.MoreVert, language.t("details")) }
                        DropdownMenu(expanded = selectionMenu, onDismissRequest = { selectionMenu = false }) {
                            if (selectionCount == 1) DropdownMenuItem(text = { Text(language.t("rename_selected")) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { selectionMenu = false; onRename() })
                            DropdownMenuItem(text = { Text(language.t("compress")) }, leadingIcon = { Icon(Icons.Outlined.Archive, null) }, onClick = { selectionMenu = false; onCompress() })
                            DropdownMenuItem(text = { Text(language.t("share")) }, leadingIcon = { Icon(Icons.Outlined.Share, null) }, onClick = { selectionMenu = false; onShare() })
                            DropdownMenuItem(text = { Text(language.t("delete")) }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = { selectionMenu = false; onDelete() })
                        }
                    }
                }
                isSearching -> IconButton(onClick = onCloseSearch) { Icon(Icons.Outlined.Close, language.t("done")) }
                else -> {
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, language.t("search")) }
                    IconButton(onClick = onChangeView) { Icon(if (viewMode == ViewMode.LIST) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.List, language.t("view")) }
                    IconButton(onClick = onShowActions) { Icon(Icons.AutoMirrored.Outlined.Sort, "${language.t("sort")}: ${sortOption.name} ${if (ascending) "↑" else "↓"}") }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
private fun BugeNavigationBar(destination: AppDestination, language: AppLanguage, onSelect: (AppDestination) -> Unit) {
    NavigationBar {
        navigationItems(language).forEach { item ->
            NavigationBarItem(selected = destination == item.first, onClick = { onSelect(item.first) }, icon = { Icon(item.second, null) }, label = { Text(item.third) })
        }
    }
}

@Composable
private fun BugeNavigationRail(destination: AppDestination, language: AppLanguage, onSelect: (AppDestination) -> Unit) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        navigationItems(language).forEach { item ->
            NavigationRailItem(selected = destination == item.first, onClick = { onSelect(item.first) }, icon = { Icon(item.second, null) }, label = { Text(item.third) })
        }
    }
}

private fun navigationItems(language: AppLanguage): List<Triple<AppDestination, ImageVector, String>> = listOf(
    Triple(AppDestination.BROWSE, Icons.Outlined.FolderOpen, language.t("browse")),
    Triple(AppDestination.RECENTS, Icons.Outlined.FileOpen, language.t("recents")),
    Triple(AppDestination.FAVORITES, Icons.Outlined.Bookmark, language.t("favorites")),
    Triple(AppDestination.STORAGE, Icons.Outlined.Storage, language.t("storage")),
    Triple(AppDestination.SETTINGS, Icons.Outlined.Settings, language.t("settings"))
)

@Composable
private fun BrowseScreen(
    modifier: Modifier, language: AppLanguage, root: RootLocation?, directStorageAvailable: Boolean, path: List<RootLocation>, entries: List<FileEntry>,
    searchActive: Boolean, isLoading: Boolean, viewMode: ViewMode, compact: Boolean, clipboard: ClipboardState?, isBookmarked: Boolean,
    onRequestFolder: () -> Unit, onRequestDirectStorage: () -> Unit, selectionActive: Boolean, onOpenDirectory: (FileEntry) -> Unit, onOpenFile: (FileEntry) -> Unit,
    onToggleSelection: (FileEntry) -> Unit, isSelected: (FileEntry) -> Boolean, onInfo: (FileEntry) -> Unit,
    onToggleBookmark: () -> Unit, onPaste: () -> Unit
) {
    if (root == null) {
        if (directStorageAvailable) EmptyLocationScreen(modifier, language, onRequestFolder)
        else DirectStoragePermissionScreen(modifier, language, onRequestDirectStorage, onRequestFolder)
        return
    }
    Column(modifier.fillMaxSize()) {
        LazyColumn(modifier = if (viewMode == ViewMode.LIST) Modifier.weight(1f) else Modifier.heightIn(max = 210.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(text = path.joinToString("  /  ") { it.label }, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        IconButton(onClick = onToggleBookmark) { Icon(if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (isBookmarked) language.t("unfavorite") else language.t("favorite")) }
                    }
                }
            }
            if (clipboard != null) {
                item {
                    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (clipboard.mode == ClipboardMode.COPY) Icons.Outlined.ContentCopy else Icons.AutoMirrored.Outlined.DriveFileMove, null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (clipboard.mode == ClipboardMode.COPY) "${clipboard.entries.size} ${language.t("items")} ${language.t("copy").lowercase()}" else "${clipboard.entries.size} ${language.t("items")} ${language.t("move").lowercase()}", modifier = Modifier.weight(1f))
                            FilledTonalButton(onClick = onPaste) { Text(language.t("paste")) }
                        }
                    }
                }
            }
            if (isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (entries.isEmpty() && !isLoading) item { EmptyFolder(language, searchActive) }
            if (viewMode == ViewMode.LIST) {
                items(entries, key = { it.uri.toString() }) { entry ->
                    FileListItem(entry, language, compact, isSelected(entry), selectionActive, onOpenDirectory, onOpenFile, onToggleSelection, onInfo)
                }
            }
        }
        if (viewMode == ViewMode.GRID && entries.isNotEmpty()) {
            LazyVerticalGrid(columns = GridCells.Adaptive(145.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(entries, key = { it.uri.toString() }) { entry ->
                    FileGridItem(entry, language, isSelected(entry), selectionActive, onOpenDirectory, onOpenFile, onToggleSelection, onInfo)
                }
            }
        }
    }
}

@Composable
private fun EmptyLocationScreen(modifier: Modifier, language: AppLanguage, onRequestFolder: () -> Unit) {
    Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 430.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(92.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) } }
            Spacer(Modifier.height(20.dp)); Text(language.t("no_folder"), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp)); Text(language.t("choose_folder_sub"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp)); FilledTonalButton(onClick = onRequestFolder) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text(language.t("add_location")) }
            Spacer(Modifier.height(18.dp)); Text(language.t("access_note"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DirectStoragePermissionScreen(modifier: Modifier, language: AppLanguage, onRequestDirectStorage: () -> Unit, onRequestSafFolder: () -> Unit) {
    Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 460.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(92.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Storage, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.height(20.dp)); Text(language.t("direct_storage"), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp)); Text(language.t("direct_storage_sub"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp)); FilledTonalButton(onClick = onRequestDirectStorage) { Icon(Icons.Outlined.Storage, null); Spacer(Modifier.width(8.dp)); Text(language.t("enable_storage")) }
            Spacer(Modifier.height(12.dp)); TextButton(onClick = onRequestSafFolder) { Text(language.t("saf_alternative")) }
        }
    }
}

@Composable
private fun EmptyFolder(language: AppLanguage, searching: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 58.dp)) {
        Icon(if (searching) Icons.Outlined.Search else Icons.Outlined.FolderOpen, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp)); Text(if (searching) language.t("recent_empty") else language.t("empty"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp)); Text(if (searching) language.t("search") else language.t("empty_sub"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(entry: FileEntry, language: AppLanguage, compact: Boolean, selected: Boolean, selectionActive: Boolean, onOpenDirectory: (FileEntry) -> Unit, onOpenFile: (FileEntry) -> Unit, onToggleSelection: (FileEntry) -> Unit, onInfo: (FileEntry) -> Unit) {
    val icon = iconFor(entry)
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val selectionScale by animateFloatAsState(if (selected) 0.985f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "listSelectionScale")
    val itemShape = RoundedCornerShape(if (compact) 14.dp else 20.dp)
    Surface(shape = itemShape, color = container, modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)).scale(selectionScale).clip(itemShape).combinedClickable(onClick = { if (selectionActive || selected) onToggleSelection(entry) else if (entry.isDirectory) onOpenDirectory(entry) else onOpenFile(entry) }, onLongClick = { onToggleSelection(entry) })) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 11.dp)) {
            if (selected) Checkbox(checked = true, onCheckedChange = { onToggleSelection(entry) }) else FileGlyph(icon, entry.isDirectory)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(fileSecondaryText(entry, language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { onInfo(entry) }) { Icon(Icons.Outlined.Info, language.t("details")) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItem(entry: FileEntry, language: AppLanguage, selected: Boolean, selectionActive: Boolean, onOpenDirectory: (FileEntry) -> Unit, onOpenFile: (FileEntry) -> Unit, onToggleSelection: (FileEntry) -> Unit, onInfo: (FileEntry) -> Unit) {
    val selectionScale by animateFloatAsState(if (selected) 0.96f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "gridSelectionScale")
    val itemShape = RoundedCornerShape(16.dp)
    ElevatedCard(shape = itemShape, colors = CardDefaults.elevatedCardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)).scale(selectionScale).clip(itemShape).combinedClickable(onClick = { if (selectionActive || selected) onToggleSelection(entry) else if (entry.isDirectory) onOpenDirectory(entry) else onOpenFile(entry) }, onLongClick = { onToggleSelection(entry) })) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FileGlyph(iconFor(entry), entry.isDirectory, size = 36.dp)
                IconButton(onClick = { onInfo(entry) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Outlined.MoreVert, language.t("details")) }
            }
            Spacer(Modifier.height(22.dp)); Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp)); Text(fileSecondaryText(entry, language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun FileGlyph(icon: ImageVector, folder: Boolean, size: Dp = 30.dp) {
    Surface(color = if (folder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(size + 16.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(size), tint = if (folder) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer) }
    }
}

@Composable
private fun RecentsScreen(modifier: Modifier, language: AppLanguage, items: List<FileEntry>, onOpen: (FileEntry) -> Unit, onInfo: (FileEntry) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.FileOpen, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(14.dp)); Text(language.t("recent_empty"), style = MaterialTheme.typography.headlineSmall); Text(language.t("recent_sub"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) } }
    } else {
        LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(language.t("recents"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 6.dp)) }
            items(items, key = { it.uri.toString() }) { FileListItem(it, language, false, false, false, {}, onOpen, {}, onInfo) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesScreen(modifier: Modifier, language: AppLanguage, items: List<RootLocation>, onOpen: (RootLocation) -> Unit, onRemove: (RootLocation) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp).widthIn(max = 360.dp)) {
                Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp)); Text(language.t("favorites_empty"), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp)); Text(language.t("favorites_sub"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(language.t("favorites"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 6.dp)) }
            items(items, key = { it.uri.toString() }) { item ->
                val itemShape = RoundedCornerShape(20.dp)
                Surface(shape = itemShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clip(itemShape).combinedClickable(onClick = { onOpen(item) }, onLongClick = { onRemove(item) })) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FileGlyph(Icons.Outlined.Folder, true)
                        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(item.uri.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        IconButton(onClick = { onRemove(item) }) { Icon(Icons.Outlined.Bookmark, language.t("unfavorite")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageScreen(modifier: Modifier, language: AppLanguage, root: RootLocation?, data: StorageBreakdown, loading: Boolean, onRequestFolder: () -> Unit) {
    if (root == null) { EmptyLocationScreen(modifier, language, onRequestFolder); return }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(language.t("storage"), style = MaterialTheme.typography.headlineLarge); Text(root.label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (loading) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        item {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(22.dp)) {
                    Text(language.t("total"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(formatBytes(data.total), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(10.dp)); Text("${data.files} ${language.t("files")} · ${data.folders} ${language.t("folders")}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        item { Text(language.t("files"), style = MaterialTheme.typography.titleLarge) }
        items(listOf(
            Triple(language.t("documents"), data.documents, Icons.Outlined.Description), Triple(language.t("images"), data.images, Icons.Outlined.Image), Triple(language.t("videos"), data.videos, Icons.Outlined.VideoFile), Triple(language.t("audio"), data.audio, Icons.Outlined.AudioFile), Triple(language.t("archives"), data.archives, Icons.Outlined.Archive), Triple(language.t("other"), data.other, Icons.Outlined.FileOpen)
        ), key = { it.first }) { row -> StorageRow(row.first, row.second, data.total, row.third) }
    }
}

@Composable
private fun StorageRow(label: String, size: Long, total: Long, icon: ImageVector) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) } }
                Spacer(Modifier.width(12.dp)); Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text(formatBytes(size), style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(progress = { if (total == 0L) 0f else size.toFloat() / total.toFloat() }, modifier = Modifier.width(540.dp).padding(top = 10.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        }
    }
}

private enum class AppearanceDialog { THEME, COLOR, LANGUAGE }

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    language: AppLanguage,
    settings: AppSettings,
    viewModel: BugeViewModel,
    onSettingsChange: (AppSettings) -> Unit
) {
    var appearanceDialog by remember { mutableStateOf<AppearanceDialog?>(null) }
    var showInstallerDialog by remember { mutableStateOf(false) }
    var installerNameInput by remember { mutableStateOf(settings.shizukuInstaller) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(language.t("settings"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 4.dp)) }
            item { SettingsSection(language.t("appearance")) }
            item {
                SettingsActionCard(
                    title = language.t("theme"),
                    summary = themePreferenceLabel(language, settings.theme),
                    icon = Icons.Outlined.Palette,
                    onClick = { appearanceDialog = AppearanceDialog.THEME }
                )
            }
            item {
                SettingsActionCard(
                    title = language.t("color"),
                    summary = colorSourceLabel(language, settings.colorSource),
                    icon = Icons.Outlined.Palette,
                    onClick = { appearanceDialog = AppearanceDialog.COLOR }
                )
            }
            item {
                SettingsActionCard(
                    title = language.t("language"),
                    summary = settings.language.nativeName,
                    icon = Icons.Outlined.Language,
                    onClick = { appearanceDialog = AppearanceDialog.LANGUAGE }
                )
            }
            item { SettingsSection(language.t("behavior")) }
            item { SettingSwitch(language.t("compact"), settings.compactMode) { onSettingsChange(settings.copy(compactMode = it)) } }
            item { SettingSwitch(language.t("hidden"), settings.showHidden) { onSettingsChange(settings.copy(showHidden = it)) } }
            item { SettingSwitch(language.t("haptics"), settings.hapticFeedback) { onSettingsChange(settings.copy(hapticFeedback = it)) } }
            item { SettingsSection("Shizuku") }
            item {
                SettingSwitch(
                    language.t("shizuku_enable"),
                    settings.shizukuEnabled
                ) {
                    onSettingsChange(settings.copy(shizukuEnabled = it))
                    if (it) {
                        viewModel.initializeShizuku()
                    }
                }
            }
            item {
                SettingsActionCard(
                    title = language.t("shizuku_installer"),
                    summary = if (settings.shizukuInstaller.isNotEmpty()) settings.shizukuInstaller else language.t("shizuku_installer_hint"),
                    icon = Icons.Outlined.Edit,
                    onClick = {
                        installerNameInput = settings.shizukuInstaller
                        showInstallerDialog = true
                    }
                )
            }
            item {
                SettingSwitch(
                    language.t("shizuku_prefer"),
                    settings.shizukuPreferInstall
                ) { onSettingsChange(settings.copy(shizukuPreferInstall = it)) }
            }
            item {
                val status = if (viewModel.shizukuReady.value) {
                    language.t("shizuku_connected")
                } else {
                    language.t("shizuku_disconnected")
                }
                val color = if (viewModel.shizukuReady.value) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    "Shizuku: $status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            item { SettingsSection(language.t("about")) }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Buge Files", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(language.t("file_manager"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Text(language.t("version"), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        when (appearanceDialog) {
            AppearanceDialog.THEME -> ThemeSettingsDialog(
                language = language,
                settings = settings,
                onSettingsChange = onSettingsChange,
                onDismiss = { appearanceDialog = null }
            )
            AppearanceDialog.COLOR -> ColorSettingsDialog(
                language = language,
                settings = settings,
                onSettingsChange = onSettingsChange,
                onDismiss = { appearanceDialog = null }
            )
            AppearanceDialog.LANGUAGE -> LanguageSettingsDialog(
                language = language,
                settings = settings,
                onSettingsChange = onSettingsChange,
                onDismiss = { appearanceDialog = null }
            )
            null -> Unit
        }

        if (showInstallerDialog) {
            AlertDialog(
                onDismissRequest = { showInstallerDialog = false },
                title = { Text(language.t("shizuku_installer")) },
                text = {
                    OutlinedTextField(
                        value = installerNameInput,
                        onValueChange = { installerNameInput = it },
                        label = { Text(language.t("shizuku_installer_hint")) },
                        placeholder = { Text("com.android.vending") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onSettingsChange(settings.copy(shizukuInstaller = installerNameInput.trim()))
                            showInstallerDialog = false
                        }
                    ) {
                        Text(language.t("save"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showInstallerDialog = false }) {
                        Text(language.t("cancel"))
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun SettingsActionCard(title: String, summary: String, icon: ImageVector, onClick: () -> Unit) {
    val cardShape = RoundedCornerShape(16.dp)
    ElevatedCard(
        shape = cardShape,
        modifier = Modifier.fillMaxWidth().clip(cardShape).clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ThemeSettingsDialog(language: AppLanguage, settings: AppSettings, onSettingsChange: (AppSettings) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("theme")) },
        text = {
            Column {
                ThemePreference.entries.forEach { preference ->
                    AppearanceOptionRow(
                        label = themePreferenceLabel(language, preference),
                        selected = settings.theme == preference,
                        onSelect = { onSettingsChange(settings.copy(theme = preference)) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(language.t("done")) } }
    )
}

@Composable
private fun ColorSettingsDialog(language: AppLanguage, settings: AppSettings, onSettingsChange: (AppSettings) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("color")) },
        text = {
            Column {
                ColorSource.entries.forEach { source ->
                    AppearanceOptionRow(
                        label = colorSourceLabel(language, source),
                        selected = settings.colorSource == source,
                        icon = Icons.Outlined.Palette,
                        onSelect = { onSettingsChange(settings.copy(colorSource = source)) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(language.t("done")) } }
    )
}

@Composable
private fun LanguageSettingsDialog(language: AppLanguage, settings: AppSettings, onSettingsChange: (AppSettings) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("language")) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                items(AppLanguage.entries.toList(), key = { it.code }) { appLanguage ->
                    AppearanceOptionRow(
                        label = appLanguage.nativeName,
                        supportingText = appLanguage.code,
                        selected = settings.language == appLanguage,
                        icon = Icons.Outlined.Language,
                        onSelect = { onSettingsChange(settings.copy(language = appLanguage)) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(language.t("done")) } }
    )
}

@Composable
private fun AppearanceOptionRow(label: String, selected: Boolean, onSelect: () -> Unit, icon: ImageVector? = null, supportingText: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supportingText != null) Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = onSelect)
    }
}

private fun themePreferenceLabel(language: AppLanguage, preference: ThemePreference): String = when (preference) {
    ThemePreference.SYSTEM -> language.t("theme_system")
    ThemePreference.LIGHT -> language.t("theme_light")
    ThemePreference.DARK -> language.t("theme_dark")
}

private fun colorSourceLabel(language: AppLanguage, source: ColorSource): String = when (source) {
    ColorSource.DYNAMIC -> language.t("dynamic")
    ColorSource.INDIGO -> language.t("indigo")
    ColorSource.OCEAN -> language.t("ocean")
    ColorSource.FOREST -> language.t("forest")
    ColorSource.SUNSET -> language.t("sunset")
    ColorSource.ORCHID -> language.t("orchid")
}

@Composable
private fun <T> ChoiceRow(values: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { (value, label) -> FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) }) } }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = onChange) } }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SortAndViewSheet(language: AppLanguage, sortOption: SortOption, ascending: Boolean, viewMode: ViewMode, showHidden: Boolean, onSort: (SortOption) -> Unit, onView: (ViewMode) -> Unit, onToggleHidden: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(language.t("sort"), style = MaterialTheme.typography.headlineSmall)
            SortOption.entries.forEach { option ->
                ListItem(headlineContent = { Text(when (option) { SortOption.NAME -> language.t("sort_name"); SortOption.DATE -> language.t("sort_date"); SortOption.SIZE -> language.t("sort_size"); SortOption.TYPE -> language.t("sort_type") }) }, trailingContent = { RadioButton(selected = option == sortOption, onClick = { onSort(option) }) }, modifier = Modifier.combinedClickable(onClick = { onSort(option) }, onLongClick = {}))
            }
            Text(if (ascending) "↑" else "↓", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(); Text(language.t("view"), style = MaterialTheme.typography.titleLarge)
            ChoiceRow(listOf(ViewMode.LIST to language.t("list"), ViewMode.GRID to language.t("grid")), viewMode, onView)
            SettingSwitch(language.t("hidden"), showHidden) { onToggleHidden() }
        }
    }
}

@Composable
private fun NameDialog(title: String, hint: String, language: AppLanguage, initialValue: String = "", confirmLabel: String = language.t("create"), onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(hint) }, singleLine = true) }, confirmButton = { TextButton(onClick = { onConfirm(value.trim()) }, enabled = value.trim().isNotEmpty()) { Text(confirmLabel) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(language.t("cancel")) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileDetailsSheet(file: FileEntry, language: AppLanguage, onDismiss: () -> Unit, onOpen: () -> Unit, onOpenTool: () -> Unit, onChecksum: () -> Unit, onShare: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { FileGlyph(iconFor(file), file.isDirectory, 34.dp); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(file.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(if (file.isDirectory) language.t("folder") else file.mimeType.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            Spacer(Modifier.height(20.dp)); DetailLine(language.t("size"), if (file.isDirectory) "${file.childCount} ${language.t("items")}" else formatBytes(file.size)); DetailLine(language.t("modified"), formatDate(file.lastModified)); DetailLine(language.t("type"), if (file.isDirectory) language.t("folder") else file.extension.uppercase())
            Spacer(Modifier.height(18.dp)); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!file.isDirectory) {
                    if (file.isApkPackage() || file.isImageFile() || file.isZipContainer() || file.isEditableText()) FilledTonalButton(onClick = onOpenTool) { Icon(if (file.isApkPackage() || file.isZipContainer()) Icons.Outlined.Archive else if (file.isImageFile()) Icons.Outlined.Image else Icons.Outlined.Description, null); Spacer(Modifier.width(6.dp)); Text(if (file.isApkPackage()) language.t("apk_package") else if (file.isImageFile()) language.t("image_preview") else if (file.isZipContainer()) language.t("archive") else language.t("edit")) }
                    OutlinedButton(onClick = onOpen) { Icon(Icons.Outlined.FileOpen, null); Spacer(Modifier.width(6.dp)); Text(language.t("open")) }
                    OutlinedButton(onClick = onShare) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(6.dp)); Text(language.t("share")) }
                    OutlinedButton(onClick = onChecksum) { Icon(Icons.Outlined.Info, null); Spacer(Modifier.width(6.dp)); Text("SHA-256") }
                }
                OutlinedButton(onClick = onRename) { Text(language.t("rename")) }; TextButton(onClick = onDelete) { Text(language.t("delete"), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApkInspectorSheet(
    file: FileEntry,
    metadata: ApkMetadata?,
    loading: Boolean,
    language: AppLanguage,
    settings: AppSettings,
    shizukuReady: Boolean,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onInstallWithShizuku: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileGlyph(Icons.Outlined.Archive, false, 30.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(metadata?.displayName ?: file.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(language.t("apk_details"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, language.t("close")) }
                }
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            metadata?.let { apk ->
                item {
                    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = if (apk.isNewerThanInstalled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(if (apk.isInstalled) language.t("installed") else language.t("not_installed"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(if (apk.isInstalled) "${apk.installedVersionName ?: "—"} · ${apk.installedVersionCode ?: "—"}" else language.t("system_installer"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (apk.isInstalled) Text(if (apk.isNewerThanInstalled) language.t("newer_update") else language.t("same_or_older"), style = MaterialTheme.typography.labelMedium, color = if (apk.isNewerThanInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailLine(language.t("package_name"), apk.packageName)
                        DetailLine(language.t("version"), "${apk.versionName} · ${apk.versionCode}")
                        DetailLine(language.t("min_sdk"), "API ${apk.minSdk}")
                        DetailLine(language.t("target_sdk"), "API ${apk.targetSdk}")
                        DetailLine(language.t("size"), formatBytes(file.size))
                    }
                }
                item {
                    Text(language.t("signing_certificate"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    if (apk.certificateSha256.isEmpty()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant) else apk.certificateSha256.forEach { fingerprint ->
                        SelectionContainer { Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) { Text(fingerprint, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp)) } }
                    }
                }
                item {
                    Text("${language.t("requested_permissions")} (${apk.requestedPermissions.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    if (apk.requestedPermissions.isEmpty()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant) else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        apk.requestedPermissions.forEach { permission -> AssistChip(onClick = {}, label = { Text(permission.substringAfterLast('.'), maxLines = 1) }) }
                    }
                }
                item {
                    if (settings.shizukuEnabled && shizukuReady) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onInstallWithShizuku,
                                modifier = Modifier.weight(1f),
                                enabled = !isInstalling
                            ) {
                                Icon(if (isInstalling) null else Icons.Outlined.FileOpen, null)
                                if (isInstalling) {
                                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(if (apk.isInstalled) language.t("install_update") else language.t("install"))
                            }
                            OutlinedButton(
                                onClick = onInstall,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.FileOpen, null)
                                Spacer(Modifier.width(8.dp))
                                Text(language.t("system_installer"))
                            }
                        }
                        if (!isInstalling) {
                            Text("Installer: ${if (settings.shizukuInstaller.isNotEmpty()) settings.shizukuInstaller else "empty"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        FilledTonalButton(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.FileOpen, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (apk.isInstalled) language.t("install_update") else language.t("install"))
                        }
                        Text(language.t("system_installer"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            if (!loading && metadata == null) item { Text("APK metadata is unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun CodeEditorScreen(file: FileEntry, content: String, loading: Boolean, language: AppLanguage, onContentChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, language.t("close")) }
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${codeLanguage(file)} · ${content.lineSequence().count()} ${language.t("lines")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = onSave, enabled = !loading) { Text(language.t("save")) }
                }
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
                } else {
                    Row(Modifier.fillMaxSize().padding(12.dp)) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.width(42.dp).fillMaxHeight()) {
                            LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), horizontalAlignment = Alignment.End) {
                                items(content.lineSequence().count().coerceAtLeast(1)) { index -> Text("${index + 1}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp, bottom = 2.dp)) }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = content,
                            onValueChange = onContentChange,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp),
                            singleLine = false,
                            placeholder = { Text(language.t("code_editor")) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveBrowserSheet(file: FileEntry, items: List<ArchiveItem>, loading: Boolean, language: AppLanguage, onExtract: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 700.dp).padding(horizontal = 24.dp).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileGlyph(Icons.Outlined.Archive, false, 28.dp)
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(file.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${items.count { !it.isDirectory }} ${language.t("files")}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                FilledTonalButton(onClick = onExtract, enabled = !loading) { Text(language.t("extract_here")) }
            }
            Spacer(Modifier.height(16.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!loading && items.isEmpty()) Text(language.t("empty"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(items, key = { it.name }) { item ->
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description, null, tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.name.trimEnd('/'), maxLines = 1, overflow = TextOverflow.Ellipsis); if (!item.isDirectory) Text("${formatBytes(item.size)} · ${formatBytes(item.compressedSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageViewerScreen(file: FileEntry, language: AppLanguage, onShare: () -> Unit, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0E0D10)) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = file.uri,
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale == 1f) Offset.Zero else offset + pan
                        }
                    }.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                )
                Surface(color = Color(0x99000000), shape = RoundedCornerShape(24.dp), modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp, start = 16.dp, end = 16.dp).fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, language.t("close"), tint = Color.White) }
                        Text(file.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        TextButton(onClick = { scale = 1f; offset = Offset.Zero }) { Text("1×", color = Color.White) }
                        IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, language.t("share"), tint = Color.White) }
                    }
                }
                Surface(color = Color(0x99000000), shape = RoundedCornerShape(18.dp), modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)) { Text("${formatBytes(file.size)} · ${file.extension.uppercase()} · ${(scale * 100).toInt()}%", color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecksumSheet(file: FileEntry, value: String?, loading: Boolean, language: AppLanguage, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text(language.t("checksum"), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp)); Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth()) else SelectionContainer { Text(value ?: "—", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(18.dp)); TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(language.t("close")) }
        }
    }
}

private fun codeLanguage(file: FileEntry): String = when (file.extension) {
    "kt", "kts" -> "Kotlin"; "java" -> "Java"; "xml" -> "XML"; "json" -> "JSON"; "js", "ts", "tsx", "jsx" -> "JavaScript"; "py" -> "Python"; "html" -> "HTML"; "css" -> "CSS"; "sql" -> "SQL"; else -> "Text"
}

@Composable
private fun DetailLine(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(108.dp)); Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)) } }

private fun iconFor(entry: FileEntry): ImageVector = when {
    entry.isDirectory -> Icons.Outlined.Folder
    entry.mimeType.orEmpty().startsWith("image/") -> Icons.Outlined.Image
    entry.mimeType.orEmpty().startsWith("video/") -> Icons.Outlined.VideoFile
    entry.mimeType.orEmpty().startsWith("audio/") -> Icons.Outlined.AudioFile
    entry.extension in setOf("zip", "rar", "7z", "tar", "gz") -> Icons.Outlined.Archive
    entry.mimeType.orEmpty().contains("pdf") || entry.mimeType.orEmpty().startsWith("text/") || entry.extension in setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "md", "json", "csv") -> Icons.Outlined.Description
    else -> Icons.Outlined.FileOpen
}

private fun fileSecondaryText(file: FileEntry, language: AppLanguage): String = if (file.isDirectory) "${file.childCount} ${language.t("items")} · ${formatDate(file.lastModified)}" else "${formatBytes(file.size)} · ${formatDate(file.lastModified)}"
private fun formatBytes(bytes: Long): String { if (bytes <= 0) return "0 B"; val units = arrayOf("B", "KB", "MB", "GB", "TB"); val exponent = (kotlin.math.ln(bytes.toDouble()) / kotlin.math.ln(1024.0)).toInt().coerceIn(0, units.lastIndex); return "%.1f %s".format(bytes / 1024.0.pow(exponent), units[exponent]) }
private fun Double.pow(exponent: Int): Double = java.lang.Math.pow(this, exponent.toDouble())
private fun formatDate(millis: Long): String = if (millis <= 0L) "—" else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
