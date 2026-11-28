// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Buge Studio

@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.buge.files

import android.app.Activity
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
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
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onShareFiles: (List<FileEntry>) -> Unit,
    pickOnly: Boolean = false,
    saveIncoming: Boolean = false,
    incomingUris: List<Uri> = emptyList(),
    onSaveIncoming: suspend (Uri, Boolean) -> Unit = { _, _ -> }
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val roots by viewModel.roots.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val fileFavorites by viewModel.fileFavorites.collectAsStateWithLifecycle()
    val currentRoot by viewModel.currentRoot.collectAsStateWithLifecycle()
    val directStorageAvailable by viewModel.directStorageAvailable.collectAsStateWithLifecycle()
    val rootAvailable by viewModel.rootAvailable.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val language = settings.language.resolved()
    val snackbars = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    var showRootMenu by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var showTasks by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var createFabVisible by remember { mutableStateOf(true) }
    val activeTask = tasks.firstOrNull { it.id == viewModel.activeTaskId }
    val shizukuAvailable = viewModel.shizukuAvailable
    val shizukuPermission = viewModel.shizukuPermission
    var showCreateFile by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var showCompress by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileEntry?>(null) }
    var deleteRequest by remember { mutableStateOf<List<FileEntry>?>(null) }
    val selected = viewModel.selectedEntries()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var lastExitBackPress by remember { mutableLongStateOf(0L) }

    LaunchedEffect(viewModel.destination, viewModel.isSearching) {
        if (viewModel.destination != AppDestination.BROWSE || viewModel.isSearching) fabMenuExpanded = false
    }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbars.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    BackHandler(enabled = true) {
        if (fabMenuExpanded) {
            fabMenuExpanded = false
            return@BackHandler
        }
        when {
            viewModel.selection.isNotEmpty() -> viewModel.clearSelection()
            viewModel.isSearching -> viewModel.setSearchActive(false)
            viewModel.destination != AppDestination.BROWSE -> viewModel.selectDestination(AppDestination.BROWSE)
            viewModel.isInsideArchive() -> viewModel.leaveArchive()
            viewModel.navigationPath.size > 1 -> viewModel.navigateUp()
            viewModel.hasReturnDestination() -> viewModel.leaveBrowseToPrevious()
            else -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastExitBackPress <= 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastExitBackPress = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
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
                        showRecents = settings.showRecents,
                        onSelect = viewModel::selectDestination
                    )
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        BugeTopBar(
                            language = language,
                            root = currentRoot,
                            roots = roots,
                            selectionCount = viewModel.selection.size,
                            titleText = when (viewModel.destination) {
                                AppDestination.BROWSE -> if (viewModel.navigationPath.size > 1) viewModel.navigationPath.lastOrNull()?.label.orEmpty() else ""
                                AppDestination.RECENTS -> language.t("recents")
                                AppDestination.FAVORITES -> language.t("favorites")
                                AppDestination.STORAGE -> language.t("storage")
                                AppDestination.SETTINGS -> language.t("settings")
                            },
                            showBrowseControls = viewModel.destination == AppDestination.BROWSE,
                            viewMode = settings.viewMode,
                            taskCount = tasks.size,
                            onToggleRootMenu = { showRootMenu = !showRootMenu },
                            onSelectRoot = { viewModel.selectRoot(it, reportFailure = true); showRootMenu = false },
                            onShowTasks = { showTasks = true },
                            onShowActions = { showActions = true },
                            onRefresh = { viewModel.refresh(reportFailure = true) },
                            onCopyPath = {
                                val path = viewModel.currentLocationPath()
                                if (path.isBlank()) viewModel.showMessage(language.t("read_failed"))
                                else {
                                    clipboardManager.setText(AnnotatedString(path))
                                    viewModel.showMessage(language.t("path_copied"))
                                }
                            },
                            onChangeView = { viewModel.setViewMode(if (settings.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST) },
                            onClearSelection = viewModel::clearSelection,
                            onSelectAll = viewModel::selectAll,
                            onCopy = { viewModel.copySelected(ClipboardMode.COPY) },
                            onMove = { viewModel.copySelected(ClipboardMode.MOVE) },
                            onRename = { selected.singleOrNull()?.let { renameTarget = it } },
                            onCompress = { showCompress = true },
                            onShare = { onShareFiles(selected) },
                            onDelete = { deleteRequest = selected },
                            canFavorite = selected.any { !it.isDirectory },
                            onFavorite = { selected.filterNot { it.isDirectory }.forEach(viewModel::toggleFileFavorite) },
                            rootMenuExpanded = showRootMenu,
                            onAddLocation = onRequestFolder,
                            onAddSmb = { showRootMenu = false; viewModel.showSmbDialog() },
                            onRemoveRoot = viewModel::removeRoot
                        )
                    },
                    bottomBar = {
                        if (!wide) BugeNavigationBar(viewModel.destination, language, settings.showRecents, viewModel::selectDestination)
                    },
                    floatingActionButton = {
                        if (viewModel.destination == AppDestination.BROWSE && currentRoot != null && !viewModel.isSearching) {
                            AnimatedVisibility(
                                visible = createFabVisible,
                                enter = slideInVertically(animationSpec = tween(180), initialOffsetY = { it }),
                                exit = slideOutVertically(animationSpec = tween(140), targetOffsetY = { it })
                            ) {
                            when (settings.fabStyle) {
                                FabStyle.EXTENDED -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                FabStyle.ICON -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FloatingActionButton(onClick = { showCreateFile = true }) { Icon(Icons.Outlined.Description, language.t("new_file")) }
                                    FloatingActionButton(onClick = { showCreateFolder = true }) { Icon(Icons.Outlined.CreateNewFolder, language.t("new_folder")) }
                                }
                                FabStyle.MENU -> CreateFabMenu(
                                    expanded = fabMenuExpanded,
                                    language = language,
                                    onToggle = { fabMenuExpanded = !fabMenuExpanded },
                                    onNewFile = { fabMenuExpanded = false; showCreateFile = true },
                                    onNewFolder = { fabMenuExpanded = false; showCreateFolder = true }
                                )
                            }
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
                                viewModel = viewModel,
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
                                showThumbnails = settings.showThumbnails,
                                activeTask = activeTask,
                                taskCount = tasks.size,
                                isBookmarked = viewModel.navigationPath.lastOrNull()?.let(viewModel::isBookmarked) ?: false,
                                onRequestFolder = onRequestFolder,
                                onRequestDirectStorage = onRequestDirectStorage,
                                selectionActive = viewModel.selection.isNotEmpty(),
                                onOpenDirectory = viewModel::openDirectory,
                                onOpenFile = { if (pickOnly) onOpenFile(it) else viewModel.openEntry(it, onOpenFile) },
                                onToggleSelection = viewModel::toggleSelection,
                                isSelected = viewModel::isSelected,
                                onInfo = viewModel::showInfo,
                                onToggleBookmark = { viewModel.navigationPath.lastOrNull()?.let(viewModel::toggleBookmark) },
                                onPaste = viewModel::paste,
                                onNavigate = viewModel::navigateTo,
                                saveIncoming = saveIncoming,
                                incomingCount = incomingUris.size,
                                onSaveIncoming = onSaveIncoming,
                                onFabVisibilityChange = { createFabVisible = it }
                            )
                            AppDestination.RECENTS -> RecentsScreen(
                                modifier = Modifier.padding(padding), language = language, items = viewModel.recents, compact = settings.compactMode,
                                onOpen = { if (pickOnly) onOpenFile(it) else viewModel.openEntry(it, onOpenFile) }, onInfo = viewModel::showInfo
                            )
                            AppDestination.FAVORITES -> FavoritesScreen(
                                modifier = Modifier.padding(padding), language = language, items = bookmarks, fileFavorites = fileFavorites, compact = settings.compactMode,
                                onOpen = viewModel::openBookmark, onRemove = viewModel::toggleBookmark,
                                onOpenFile = { favorite -> viewModel.openEntry(FileEntry(favorite.uri, favorite.label, null, false, 0L, 0L), onOpenFile) },
                                onRemoveFile = viewModel::toggleFileFavorite
                            )
                            AppDestination.STORAGE -> StorageScreen(
                                modifier = Modifier.padding(padding), language = language, root = currentRoot, data = storage,
                                loading = viewModel.isStorageLoading, onRequestFolder = onRequestFolder,
                                onRefresh = viewModel::refreshStorage,
                                onOpenFile = { if (pickOnly) onOpenFile(it) else viewModel.openEntry(it, onOpenFile) },
                                onInfo = viewModel::showInfo
                            )
                            AppDestination.SETTINGS -> SettingsScreen(
                                modifier = Modifier.padding(padding), language = language, settings = settings,
                                rootAvailable = rootAvailable, shizukuAvailable = shizukuAvailable, shizukuPermission = shizukuPermission,
                                onSettingsChange = viewModel::updateSettings, onCheckForUpdates = viewModel::checkForUpdates,
                                onRequestShizuku = viewModel::requestShizukuPermission
                            )
                        }
                    }
                }
            }
        }

        if (showActions) {
            SortAndViewSheet(
                language = language, sortOption = viewModel.sortOption,
                viewMode = settings.viewMode, showHidden = settings.showHidden,
                onSort = viewModel::setSort, onView = viewModel::setViewMode, onToggleHidden = viewModel::toggleHidden,
                onDismiss = { showActions = false }
            )
        }
        if (showTasks) {
            TaskListSheet(
                language = language,
                tasks = tasks,
                activeId = viewModel.activeTaskId,
                onActivate = { viewModel.setActiveTask(it); showTasks = false },
                onRemove = viewModel::removeTask,
                onClear = viewModel::clearTasks,
                onDismiss = { showTasks = false }
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
                installing = viewModel.installInProgress,
                installed = viewModel.installCompleted,
                language = language,
                onInstall = { onInstallApk(file) },
                onOpen = {
                    val packageName = viewModel.apkMetadata?.packageName
                    if (packageName != null) {
                        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                        if (intent != null) runCatching { context.startActivity(intent) }
                    }
                },
                onDismiss = viewModel::dismissApk
            )
        }
        if (viewModel.smbDialogVisible) {
            SmbConnectionDialog(
                language = language,
                loading = viewModel.testingSmb,
                onDismiss = viewModel::dismissSmbDialog,
                onConnect = { host, share, user, password, domain, port ->
                    viewModel.dismissSmbDialog()
                    viewModel.addSmbRoot(host, share, user, password, domain, port)
                }
            )
        }
        viewModel.editorTarget?.let { file ->
            CodeEditorScreen(file = file, content = viewModel.editorText, loading = viewModel.editorLoading, language = language, onContentChange = viewModel::updateEditorText, onSave = viewModel::saveEditor, onDismiss = viewModel::dismissEditor)
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
                onOpen = { viewModel.dismissInfo(); viewModel.openEntry(file, onOpenFile) },
                onOpenTool = { viewModel.dismissInfo(); viewModel.openEntry(file, onOpenFile) },
                onChecksum = { viewModel.calculateChecksum(file); viewModel.dismissInfo() },
                onShare = { onShareFiles(listOf(file)) },
                onRename = { renameTarget = file; viewModel.dismissInfo() },
                onDelete = { deleteRequest = listOf(file); viewModel.dismissInfo() }
            )
        }
        if (viewModel.updateDialogVisible) {
            UpdateCheckDialog(
                language = language,
                checking = viewModel.updateChecking,
                result = viewModel.updateResult,
                onDownload = { url ->
                    runCatching { uriHandler.openUri(url) }
                    viewModel.dismissUpdateDialog()
                },
                onDismiss = viewModel::dismissUpdateDialog
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BugeTopBar(
    language: AppLanguage, root: RootLocation?, roots: List<RootLocation>, selectionCount: Int,
    titleText: String, showBrowseControls: Boolean, viewMode: ViewMode, taskCount: Int,
    onToggleRootMenu: () -> Unit, onSelectRoot: (RootLocation) -> Unit, onShowTasks: () -> Unit, onShowActions: () -> Unit,
    onRefresh: () -> Unit, onCopyPath: () -> Unit,
    onChangeView: () -> Unit, onClearSelection: () -> Unit, onSelectAll: () -> Unit,
    onCopy: () -> Unit, onMove: () -> Unit, onRename: () -> Unit, onCompress: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit,
    canFavorite: Boolean, onFavorite: () -> Unit,
    rootMenuExpanded: Boolean, onAddLocation: () -> Unit, onAddSmb: () -> Unit, onRemoveRoot: (RootLocation) -> Unit
) {
    val title = when { selectionCount > 0 -> "$selectionCount ${language.t("items")}"; else -> titleText }
    var selectionMenu by remember { mutableStateOf(false) }
    var overflowMenu by remember { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (showBrowseControls) when {
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
                        DropdownMenuItem(text = { Text(language.t("add_smb")) }, leadingIcon = { Icon(Icons.Outlined.Storage, null) }, onClick = onAddSmb)
                    }
                }
            }
        },
        actions = {
            if (showBrowseControls) when {
                selectionCount > 0 -> {
                    IconButton(onClick = onSelectAll) { Icon(Icons.Outlined.SelectAll, language.t("select_all")) }
                    IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, language.t("copy")) }
                    IconButton(onClick = onMove) { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, language.t("move")) }
                    Box {
                        IconButton(onClick = { selectionMenu = true }) { Icon(Icons.Outlined.MoreVert, language.t("details")) }
                        DropdownMenu(expanded = selectionMenu, onDismissRequest = { selectionMenu = false }) {
                            if (selectionCount == 1) DropdownMenuItem(text = { Text(language.t("rename_selected")) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { selectionMenu = false; onRename() })
                            if (canFavorite) DropdownMenuItem(text = { Text(language.t("favorite")) }, leadingIcon = { Icon(Icons.Outlined.Bookmark, null) }, onClick = { selectionMenu = false; onFavorite() })
                            DropdownMenuItem(text = { Text(language.t("compress")) }, leadingIcon = { Icon(Icons.Outlined.Archive, null) }, onClick = { selectionMenu = false; onCompress() })
                            DropdownMenuItem(text = { Text(language.t("share"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }, leadingIcon = { Icon(Icons.Outlined.Share, null) }, onClick = { selectionMenu = false; onShare() })
                            DropdownMenuItem(text = { Text(language.t("delete")) }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = { selectionMenu = false; onDelete() })
                        }
                    }
                }
                else -> {
                    IconButton(onClick = onChangeView) { Icon(if (viewMode == ViewMode.LIST) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.List, language.t("view")) }
                    Box {
                        IconButton(onClick = { overflowMenu = true }) { Icon(Icons.Outlined.MoreVert, language.t("more")) }
                        DropdownMenu(expanded = overflowMenu, onDismissRequest = { overflowMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(language.t("task_list")) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.List, null) },
                                trailingIcon = { if (taskCount > 0) Text("$taskCount", style = MaterialTheme.typography.labelLarge) },
                                onClick = { overflowMenu = false; onShowTasks() }
                            )
                            DropdownMenuItem(text = { Text(language.t("filter")) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, null) }, onClick = { overflowMenu = false; onShowActions() })
                            DropdownMenuItem(text = { Text(language.t("refresh")) }, leadingIcon = { Icon(Icons.Outlined.Refresh, null) }, onClick = { overflowMenu = false; onRefresh() })
                            DropdownMenuItem(text = { Text(language.t("copy_path")) }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { overflowMenu = false; onCopyPath() })
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateFabMenu(expanded: Boolean, language: AppLanguage, onToggle: () -> Unit, onNewFile: () -> Unit, onNewFolder: () -> Unit) {
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { onToggle() }
            ) {
                Icon(if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add, language.t("more"))
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = { onToggle(); onNewFile() },
            icon = { Icon(Icons.Outlined.Description, null) },
            text = { Text(language.t("new_file"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
        )
        FloatingActionButtonMenuItem(
            onClick = { onToggle(); onNewFolder() },
            icon = { Icon(Icons.Outlined.CreateNewFolder, null) },
            text = { Text(language.t("new_folder"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
        )
    }
}

@Composable
private fun BugeNavigationBar(destination: AppDestination, language: AppLanguage, showRecents: Boolean, onSelect: (AppDestination) -> Unit) {
    NavigationBar {
        navigationItems(language, showRecents).forEach { item ->
            NavigationBarItem(selected = destination == item.first, onClick = { onSelect(item.first) }, icon = { Icon(item.second, null) }, label = { Text(item.third) })
        }
    }
}

@Composable
private fun BugeNavigationRail(destination: AppDestination, language: AppLanguage, showRecents: Boolean, onSelect: (AppDestination) -> Unit) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        navigationItems(language, showRecents).forEach { item ->
            NavigationRailItem(selected = destination == item.first, onClick = { onSelect(item.first) }, icon = { Icon(item.second, null) }, label = { Text(item.third) })
        }
    }
}

private fun navigationItems(language: AppLanguage, showRecents: Boolean): List<Triple<AppDestination, ImageVector, String>> = buildList {
    add(Triple(AppDestination.BROWSE, Icons.Outlined.FolderOpen, language.t("browse")))
    if (showRecents) add(Triple(AppDestination.RECENTS, Icons.Outlined.FileOpen, language.t("recents")))
    add(Triple(AppDestination.FAVORITES, Icons.Outlined.Bookmark, language.t("favorites")))
    add(Triple(AppDestination.STORAGE, Icons.Outlined.Storage, language.t("storage")))
    add(Triple(AppDestination.SETTINGS, Icons.Outlined.Settings, language.t("settings")))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BugeSearchBar(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    clearOnCollapse: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            colors = SearchBarDefaults.containedColors(state = searchBarState).inputFieldColors,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) IconButton(onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") }) { Icon(Icons.Outlined.Close, null) }
            }
        )
    }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collectLatest { onQueryChange(it) }
    }
    LaunchedEffect(searchBarState.currentValue) {
        if (clearOnCollapse && searchBarState.currentValue == SearchBarValue.Collapsed && textFieldState.text.isNotEmpty()) {
            textFieldState.setTextAndPlaceCursorAtEnd("")
            onQueryChange("")
        }
    }
    SearchBar(state = searchBarState, inputField = inputField, modifier = modifier)
    ExpandedFullScreenContainedSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.containedColors(state = searchBarState),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BrowseScreen(
    viewModel: BugeViewModel, modifier: Modifier, language: AppLanguage, root: RootLocation?, directStorageAvailable: Boolean, path: List<RootLocation>, entries: List<FileEntry>,
    searchActive: Boolean, isLoading: Boolean, viewMode: ViewMode, compact: Boolean, showThumbnails: Boolean, activeTask: FileTask?, taskCount: Int, isBookmarked: Boolean,
    onRequestFolder: () -> Unit, onRequestDirectStorage: () -> Unit, selectionActive: Boolean, onOpenDirectory: (FileEntry) -> Unit, onOpenFile: (FileEntry) -> Unit,
    onToggleSelection: (FileEntry) -> Unit, isSelected: (FileEntry) -> Boolean, onInfo: (FileEntry) -> Unit,
    onToggleBookmark: () -> Unit, onPaste: () -> Unit, onNavigate: (Int) -> Unit,
    saveIncoming: Boolean, incomingCount: Int, onSaveIncoming: suspend (Uri, Boolean) -> Unit,
    onFabVisibilityChange: (Boolean) -> Unit
) {
    if (root == null) {
        if (directStorageAvailable) EmptyLocationScreen(modifier, language, onRequestFolder)
        else DirectStoragePermissionScreen(modifier, language, onRequestDirectStorage, onRequestFolder)
        return
    }
    val pathKey = path.joinToString("/") { it.uri.toString() }
    val browseSearchText = rememberTextFieldState()
    val browseSearchState = rememberContainedSearchBarState()
    LaunchedEffect(viewModel.searchQuery) {
        if (viewModel.searchQuery != browseSearchText.text.toString()) browseSearchText.setTextAndPlaceCursorAtEnd(viewModel.searchQuery)
    }
    val savedPosition = remember(pathKey) { viewModel.browseScrollPosition(path) }
    val listState = remember(pathKey) {
        LazyListState(savedPosition?.index ?: 0, savedPosition?.offset ?: 0)
    }
    LaunchedEffect(pathKey) { onFabVisibilityChange(true) }
    val gridState = rememberLazyGridState()
    LaunchedEffect(listState, gridState, viewMode) {
        var previous = -1
        snapshotFlow {
            if (viewMode == ViewMode.LIST) {
                listState.firstVisibleItemIndex * 100_000 + listState.firstVisibleItemScrollOffset
            } else {
                gridState.firstVisibleItemIndex * 100_000 + gridState.firstVisibleItemScrollOffset
            }
        }.collect { current ->
            if (previous >= 0) {
                when {
                    current > previous -> onFabVisibilityChange(false)
                    current < previous -> onFabVisibilityChange(true)
                }
            }
            previous = current
        }
    }
    var restoredPosition by remember(pathKey) { mutableStateOf(savedPosition == null) }
    LaunchedEffect(pathKey, entries.size, listState) {
        if (!restoredPosition && entries.isNotEmpty()) {
            listState.scrollToItem(savedPosition!!.index.coerceAtMost((entries.size - 1).coerceAtLeast(0)), savedPosition.offset)
            restoredPosition = true
        }
    }
    LaunchedEffect(pathKey, listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                if (restoredPosition) viewModel.saveBrowseScrollPosition(path, index, offset)
            }
    }
    val openDirectoryAndSavePosition: (FileEntry) -> Unit = { entry ->
        viewModel.saveBrowseScrollPosition(path, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        onOpenDirectory(entry)
    }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val onPullRefresh: () -> Unit = {
        if (!isRefreshing) {
            isRefreshing = true
            scope.launch {
                val started = SystemClock.elapsedRealtime()
                viewModel.refresh(reportFailure = true)
                val elapsed = SystemClock.elapsedRealtime() - started
                if (elapsed < 420L) delay(420L - elapsed)
                isRefreshing = false
            }
        }
    }
    val density = LocalDensity.current
    val slideDistance = with(density) { (LocalConfiguration.current.screenWidthDp * 0.35f).dp.toPx() }
    val slideOffset = remember { Animatable(0f) }
    var slideInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(pathKey) {
        if (!slideInitialized) {
            slideInitialized = true
            return@LaunchedEffect
        }
        val forward = viewModel.navigationDirection == NavigationDirection.FORWARD
        slideOffset.snapTo(if (forward) slideDistance else -slideDistance)
        slideOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
    }
    Column(modifier.fillMaxSize().graphicsLayer {
        translationX = slideOffset.value
        alpha = 1f - (kotlin.math.abs(slideOffset.value) / slideDistance.coerceAtLeast(1f)) * 0.25f
    }) {
            BugeSearchBar(
                textFieldState = browseSearchText,
                searchBarState = browseSearchState,
                placeholder = language.t("search"),
                onQueryChange = { text ->
                    if (text != viewModel.searchQuery) viewModel.updateSearch(text)
                    viewModel.setSearchActive(text.isNotBlank())
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                content = {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        if (entries.isEmpty()) {
                            item { Text("No results", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center) }
                        }
                        items(entries, key = { it.uri.toString() }) { entry ->
                            val resultShape = RoundedCornerShape(14.dp)
                            Surface(shape = resultShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(resultShape).clickable {
                                browseSearchText.setTextAndPlaceCursorAtEnd("")
                                viewModel.setSearchActive(false)
                                scope.launch { browseSearchState.animateToCollapsed() }
                                if (entry.isDirectory) onOpenDirectory(entry) else onOpenFile(entry)
                            }) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FileGlyph(iconFor(entry), entry.isDirectory, 24.dp, entry = entry)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(fileSecondaryText(entry, language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)
            ) {
                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    path.forEachIndexed { index, location ->
                        if (index > 0) {
                            Text(text = "/", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        if (index == path.lastIndex) {
                            Text(text = location.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text(text = location.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigate(index) })
                        }
                    }
                }
                IconButton(onClick = onToggleBookmark) { Icon(if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (isBookmarked) language.t("unfavorite") else language.t("favorite")) }
            }
            val saveDestination = path.lastOrNull()?.uri ?: root?.uri
            if (saveIncoming && saveDestination != null) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Archive, null)
                        Spacer(Modifier.width(12.dp))
                        Text("$incomingCount ${language.t("items")} ${language.t("save_as").lowercase()}", modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = { scope.launch { onSaveIncoming(saveDestination, true) } }) { Text(language.t("save_here")) }
                    }
                }
            }
            if (activeTask != null) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(taskIcon(activeTask.type), null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(taskTitle(activeTask, language), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${activeTask.entries.size} ${language.t("items")} · ${language.t("task_list")}: $taskCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        FilledTonalButton(onClick = onPaste) { Text(if (activeTask.type == TaskType.COMPRESS) language.t("compress") else language.t("paste"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
            if (isLoading && !isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp))
            if (entries.isEmpty() && !isLoading) EmptyFolder(language, searchActive)
            if (viewMode == ViewMode.LIST) {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onPullRefresh, modifier = Modifier.weight(1f)) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(entries, key = { it.uri.toString() }) { entry ->
                            FileListItem(entry, language, compact, isSelected(entry), selectionActive, openDirectoryAndSavePosition, onOpenFile, onToggleSelection, onInfo, showThumbnails = showThumbnails, thumbnailSource = viewModel::thumbnailSource, onNeedThumbnail = viewModel::loadThumbnail, thumbnailVersion = viewModel.thumbnailVersion)
                        }
                    }
                }
            } else {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onPullRefresh, modifier = Modifier.weight(1f)) {
                    LazyVerticalGrid(state = gridState, columns = GridCells.Adaptive(145.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        items(entries, key = { it.uri.toString() }) { entry ->
                            FileGridItem(entry, language, isSelected(entry), selectionActive, openDirectoryAndSavePosition, onOpenFile, onToggleSelection, onInfo, showThumbnails = showThumbnails, thumbnailSource = viewModel::thumbnailSource, onNeedThumbnail = viewModel::loadThumbnail, thumbnailVersion = viewModel.thumbnailVersion)
                        }
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
        Spacer(Modifier.height(4.dp)); Text(if (searching) language.t("search") else language.t("empty_sub"), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(entry: FileEntry, language: AppLanguage, compact: Boolean, selected: Boolean, selectionActive: Boolean, onOpenDirectory: (FileEntry) -> Unit, onOpenFile: (FileEntry) -> Unit, onToggleSelection: (FileEntry) -> Unit, onInfo: (FileEntry) -> Unit, showThumbnails: Boolean = false, thumbnailSource: (FileEntry) -> File? = { null }, onNeedThumbnail: (FileEntry) -> Unit = {}, thumbnailVersion: Int = 0) {
    val icon = iconFor(entry)
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val selectionScale by animateFloatAsState(if (selected) 0.985f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "listSelectionScale")
    val itemShape = RoundedCornerShape(if (compact) 14.dp else 20.dp)
    if (showThumbnails) LaunchedEffect(entry.uri, thumbnailVersion) { onNeedThumbnail(entry) }
    Surface(shape = itemShape, color = container, modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)).scale(selectionScale).clip(itemShape).combinedClickable(onClick = { if (selectionActive || selected) onToggleSelection(entry) else if (entry.isDirectory) onOpenDirectory(entry) else onOpenFile(entry) }, onLongClick = { onToggleSelection(entry) })) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 11.dp)) {
            if (selected) Checkbox(checked = true, onCheckedChange = { onToggleSelection(entry) }) else FileGlyph(icon, entry.isDirectory, entry = entry, showThumbnails = showThumbnails, thumbnailFile = thumbnailSource(entry), versionKey = thumbnailVersion)
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
private fun FileGridItem(entry: FileEntry, language: AppLanguage, selected: Boolean, selectionActive: Boolean, onOpenDirectory: (FileEntry) -> Unit, onOpenFile: (FileEntry) -> Unit, onToggleSelection: (FileEntry) -> Unit, onInfo: (FileEntry) -> Unit, showThumbnails: Boolean = false, thumbnailSource: (FileEntry) -> File? = { null }, onNeedThumbnail: (FileEntry) -> Unit = {}, thumbnailVersion: Int = 0) {
    val selectionScale by animateFloatAsState(if (selected) 0.96f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "gridSelectionScale")
    val itemShape = RoundedCornerShape(16.dp)
    if (showThumbnails) LaunchedEffect(entry.uri, thumbnailVersion) { onNeedThumbnail(entry) }
    ElevatedCard(shape = itemShape, colors = CardDefaults.elevatedCardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)).scale(selectionScale).clip(itemShape).combinedClickable(onClick = { if (selectionActive || selected) onToggleSelection(entry) else if (entry.isDirectory) onOpenDirectory(entry) else onOpenFile(entry) }, onLongClick = { onToggleSelection(entry) })) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FileGlyph(iconFor(entry), entry.isDirectory, size = 36.dp, entry = entry, showThumbnails = showThumbnails, thumbnailFile = thumbnailSource(entry), versionKey = thumbnailVersion)
                IconButton(onClick = { onInfo(entry) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Outlined.MoreVert, language.t("details")) }
            }
            Spacer(Modifier.height(22.dp)); Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp)); Text(fileSecondaryText(entry, language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun FileGlyph(icon: ImageVector, folder: Boolean, size: Dp = 30.dp, entry: FileEntry? = null, showThumbnails: Boolean = false, thumbnailFile: File? = null, versionKey: Int = 0) {
    val context = LocalContext.current
    var apkIcon by remember(entry?.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var thumbnailFailed by remember(entry?.uri, versionKey) { mutableStateOf(false) }
    LaunchedEffect(entry?.uri) {
        if (entry?.isApkPackage() == true) apkIcon = ApkRepository(context).loadIcon(entry)
    }
    val thumbnail = entry?.takeIf { showThumbnails && !it.isDirectory && (it.isImageFile() || it.isVideoFile()) }?.let { thumbnailFile }
    Surface(color = if (folder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(size + 16.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (thumbnail != null && !thumbnailFailed) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = entry?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size + 16.dp).clip(RoundedCornerShape(12.dp)),
                    onError = { thumbnailFailed = true }
                )
            } else if (apkIcon != null) {
                androidx.compose.foundation.Image(bitmap = apkIcon!!.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size), contentScale = ContentScale.Fit)
            } else {
                Icon(icon, null, modifier = Modifier.size(size), tint = if (folder) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
private fun RecentsScreen(modifier: Modifier, language: AppLanguage, items: List<FileEntry>, compact: Boolean, onOpen: (FileEntry) -> Unit, onInfo: (FileEntry) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.FileOpen, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(14.dp)); Text(language.t("recent_empty"), style = MaterialTheme.typography.headlineSmall); Text(language.t("recent_sub"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) } }
    } else {
        LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(language.t("recents"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 6.dp)) }
            items(items, key = { it.uri.toString() }) { FileListItem(it, language, compact, false, false, {}, onOpen, {}, onInfo) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesScreen(modifier: Modifier, language: AppLanguage, items: List<RootLocation>, fileFavorites: List<RootLocation>, compact: Boolean, onOpen: (RootLocation) -> Unit, onRemove: (RootLocation) -> Unit, onOpenFile: (RootLocation) -> Unit, onRemoveFile: (RootLocation) -> Unit) {
    var pendingFavorite by remember { mutableStateOf<Pair<RootLocation, Boolean>?>(null) }
    pendingFavorite?.let { (favorite, isFile) ->
        AlertDialog(
            onDismissRequest = { pendingFavorite = null },
            title = { Text(favorite.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text(favorite.uri.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = {
                    pendingFavorite = null
                    if (isFile) onOpenFile(favorite) else onOpen(favorite)
                }) { Text(language.t("open")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingFavorite = null
                    if (isFile) onRemoveFile(favorite) else onRemove(favorite)
                }) { Text(language.t("unfavorite"), color = MaterialTheme.colorScheme.error) }
            }
        )
    }
    if (items.isEmpty() && fileFavorites.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp).widthIn(max = 360.dp)) {
                Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp)); Text(language.t("favorites_empty"), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp)); Text(language.t("favorites_sub"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(language.t("favorites"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 6.dp)) }
            items(items, key = { it.uri.toString() }) { item ->
                val itemShape = RoundedCornerShape(if (compact) 14.dp else 20.dp)
                Surface(shape = itemShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clip(itemShape).combinedClickable(onClick = { onOpen(item) }, onLongClick = { pendingFavorite = item to false })) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        FileGlyph(Icons.Outlined.Folder, true)
                        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(item.uri.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        IconButton(onClick = { onRemove(item) }) { Icon(Icons.Outlined.Bookmark, language.t("unfavorite")) }
                    }
                }
            }
            items(fileFavorites, key = { "file_${it.uri}" }) { item ->
                val itemShape = RoundedCornerShape(if (compact) 14.dp else 20.dp)
                val entry = remember(item.uri) { FileEntry(item.uri, item.label, null, false, 0L, 0L) }
                Surface(shape = itemShape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clip(itemShape).combinedClickable(onClick = { onOpenFile(item) }, onLongClick = { pendingFavorite = item to true })) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        FileGlyph(iconFor(entry), false, entry = entry)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(item.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(item.uri.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        IconButton(onClick = { onRemoveFile(item) }) { Icon(Icons.Outlined.Bookmark, language.t("unfavorite")) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageScreen(
    modifier: Modifier,
    language: AppLanguage,
    root: RootLocation?,
    data: StorageBreakdown,
    loading: Boolean,
    onRequestFolder: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onInfo: (FileEntry) -> Unit
) {
    if (root == null) { EmptyLocationScreen(modifier, language, onRequestFolder); return }
    val categories = listOf(
        StorageCategory(language.t("documents"), data.documents, data.documentFiles, Color(0xFF7C4DFF), Icons.Outlined.Description),
        StorageCategory(language.t("images"), data.images, data.imageFiles, Color(0xFFFF7A00), Icons.Outlined.Image),
        StorageCategory(language.t("videos"), data.videos, data.videoFiles, Color(0xFF00C9A7), Icons.Outlined.VideoFile),
        StorageCategory(language.t("audio"), data.audio, data.audioFiles, Color(0xFFFF4D8D), Icons.Outlined.AudioFile),
        StorageCategory(language.t("archives"), data.archives, data.archiveFiles, Color(0xFF3D9BFF), Icons.Outlined.Archive),
        StorageCategory(language.t("other"), data.other, data.otherFiles, Color(0xFF8BC34A), Icons.Outlined.FileOpen)
    )
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(language.t("storage"), style = MaterialTheme.typography.headlineLarge)
                    Text(root.label, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, null) }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { StorageSummaryCard(categories, data) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StorageStatCard(formatCount(data.files), language.t("files"), Icons.Outlined.FileOpen, Color(0xFF7C4DFF), Modifier.weight(1f))
                StorageStatCard(formatCount(data.folders), language.t("folders"), Icons.Outlined.FolderOpen, Color(0xFFFF7A00), Modifier.weight(1f))
                StorageStatCard(
                    if (data.files > 0) formatBytes(data.total / data.files) else "0 B",
                    "Average",
                    Icons.Outlined.Storage,
                    Color(0xFF00C9A7),
                    Modifier.weight(1f)
                )
            }
        }
        item { Text("By type", style = MaterialTheme.typography.titleLarge) }
        item {
            if (data.files == 0 && data.total == 0L && !loading) {
                EmptyStorageState(language)
            } else {
                StorageCategoriesCard(categories, data.total, language)
            }
        }
        if (data.largestFiles.isNotEmpty()) {
            item { Text("Largest files", style = MaterialTheme.typography.titleLarge) }
            items(data.largestFiles, key = { it.uri.toString() }) { entry ->
                LargestFileRow(entry, language, onOpenFile, onInfo)
            }
        }
    }
}

private data class StorageCategory(val label: String, val value: Long, val count: Int, val color: Color, val icon: ImageVector)

@Composable
private fun StorageSummaryCard(categories: List<StorageCategory>, data: StorageBreakdown) {
    val total = data.total
    val used = if (data.capacity > 0L) (data.capacity - data.free).coerceAtLeast(total) else total
    val usedFraction = if (data.capacity > 0L) (used.toFloat() / data.capacity.toFloat()).coerceIn(0f, 1f) else 0f
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                StorageRing(categories, total, Modifier.size(190.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatBytes(if (data.capacity > 0L) used else total), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(if (data.capacity > 0L) "of ${formatBytes(data.capacity)}" else "Used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            if (data.capacity > 0L) {
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth(usedFraction).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Free ${formatBytes(data.free)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${(usedFraction * 100).toInt()}% used", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun StorageRing(categories: List<StorageCategory>, total: Long, modifier: Modifier) {
    val progress by animateFloatAsState(targetValue = if (total > 0L) 1f else 0f, animationSpec = tween(700), label = "storageRing")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier) {
        val stroke = 26.dp.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (total <= 0L) return@Canvas
        var start = -90f
        categories.filter { it.value > 0L }.forEach { category ->
            val sweep = category.value.toFloat() / total.toFloat() * 360f * progress
            val gap = (sweep * 0.04f).coerceIn(0f, 3f)
            drawArc(category.color, start + gap, (sweep - gap * 2f).coerceAtLeast(0.5f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            start += sweep
        }
    }
}

@Composable
private fun StorageStatCard(value: String, label: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.16f), modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StorageCategoriesCard(categories: List<StorageCategory>, total: Long, language: AppLanguage) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            categories.forEach { category ->
                val fraction = if (total > 0L) (category.value.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = category.color.copy(alpha = 0.16f), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(category.icon, null, tint = category.color, modifier = Modifier.size(22.dp)) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category.label, style = MaterialTheme.typography.titleMedium)
                            Text(formatBytes(category.value), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(CircleShape).background(category.color))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${category.count} ${language.t("files")} · ${(fraction * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LargestFileRow(entry: FileEntry, language: AppLanguage, onOpen: (FileEntry) -> Unit, onInfo: (FileEntry) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().clip(shape).clickable { onOpen(entry) }) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            FileGlyph(iconFor(entry), false, 22.dp, entry = entry)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${entry.extension.uppercase()} · ${formatDate(entry.lastModified)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatBytes(entry.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onInfo(entry) }) { Icon(Icons.Outlined.Info, null) }
        }
    }
}

@Composable
private fun EmptyStorageState(language: AppLanguage) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp)) }
        }
        Spacer(Modifier.height(14.dp))
        Text(language.t("empty"), style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatCount(count: Int): String = if (count > 9999) "${count / 1000}k" else count.toString()


private enum class AppearanceDialog { THEME, COLOR, LANGUAGE }

@Composable
private fun UpdateCheckDialog(
    language: AppLanguage,
    checking: Boolean,
    result: UpdateCheckResult?,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val downloadUrl = result?.downloadUrl
    val canDownload = result?.updateAvailable == true && !downloadUrl.isNullOrBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Refresh, null) },
        title = { Text(language.t("check_updates")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (checking) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(language.t("checking_updates"))
                } else if (result == null) {
                    Text(language.t("checking_updates"))
                } else {
                    val error = result.error
                    if (error != null) {
                        Text(language.t("update_check_failed"), style = MaterialTheme.typography.titleMedium)
                        Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (result.updateAvailable) {
                        Text(language.t("update_available"), style = MaterialTheme.typography.titleMedium)
                        Text("${language.t("current_version")}: ${result.currentVersion}")
                        Text("${language.t("latest_version")}: ${result.latestVersion.orEmpty()}")
                    } else {
                        Text(language.t("up_to_date"))
                    }
                }
            }
        },
        confirmButton = {
            if (canDownload) {
                TextButton(onClick = { onDownload(downloadUrl!!) }) { Text(language.t("download")) }
            } else {
                TextButton(onClick = onDismiss) { Text(language.t("done")) }
            }
        },
        dismissButton = {
            if (canDownload) {
                TextButton(onClick = onDismiss) { Text(language.t("cancel")) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SettingsScreen(modifier: Modifier, language: AppLanguage, settings: AppSettings, rootAvailable: Boolean, shizukuAvailable: Boolean, shizukuPermission: Boolean, onSettingsChange: (AppSettings) -> Unit, onCheckForUpdates: () -> Unit, onRequestShizuku: () -> Unit) {
    var appearanceDialog by remember { mutableStateOf<AppearanceDialog?>(null) }
    var rootCommandDialog by remember { mutableStateOf(false) }
    var installMethodDialog by remember { mutableStateOf(false) }
    var installerDeclarationDialog by remember { mutableStateOf(false) }
    var fabStyleDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val showAppearance = true
    val showAdvanced = true
    val showBehavior = true
    val showInstallation = true
    val showAbout = true

    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(language.t("settings"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 4.dp)) }
            if (showAppearance) {
                item { SettingsSection(language.t("appearance")) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsActionCard(
                        title = language.t("theme"),
                        summary = themePreferenceLabel(language, settings.theme),
                        icon = Icons.Outlined.Palette,
                        index = 0,
                        count = 5,
                        onClick = { appearanceDialog = AppearanceDialog.THEME }
                    )
                    SettingsActionCard(
                        title = language.t("color"),
                        summary = colorSourceLabel(language, settings.colorSource),
                        icon = Icons.Outlined.Palette,
                        index = 1,
                        count = 5,
                        onClick = { appearanceDialog = AppearanceDialog.COLOR }
                    )
                    SettingsActionCard(
                        title = language.t("language"),
                        summary = settings.language.nativeName,
                        icon = Icons.Outlined.Language,
                        index = 2,
                        count = 5,
                        onClick = { appearanceDialog = AppearanceDialog.LANGUAGE }
                    )
                    SettingSwitch(language.t("pure_black"), settings.pureBlack, 3, 5, icon = Icons.Outlined.Settings, supportingText = language.t("pure_black_summary")) { onSettingsChange(settings.copy(pureBlack = it)) }
                    SettingSwitch(language.t("system_font"), settings.useSystemFont, 4, 5, icon = Icons.Outlined.Description, supportingText = language.t("system_font_summary")) { onSettingsChange(settings.copy(useSystemFont = it)) }
                }
            }
            }
            if (showAdvanced) {
                item { SettingsSection(language.t("advanced")) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingSwitch(
                        label = language.t("root_access"),
                        checked = settings.rootAccess && rootAvailable,
                        index = 0,
                        count = 5,
                        enabled = rootAvailable,
                        icon = Icons.Outlined.Security,
                        supportingText = if (rootAvailable) language.t("root_access_summary") else language.t("root_unavailable")
                    ) { onSettingsChange(settings.copy(rootAccess = it)) }
                    SettingSwitch(
                        label = language.t("shizuku_access"),
                        checked = settings.shizukuAccess && shizukuPermission,
                        index = 1,
                        count = 5,
                        enabled = shizukuAvailable,
                        icon = Icons.Outlined.Terminal,
                        supportingText = when {
                            !shizukuAvailable -> language.t("shizuku_unavailable")
                            !shizukuPermission -> language.t("shizuku_permission_required")
                            else -> language.t("shizuku_access_summary")
                        }
                    ) { enabled ->
                        if (enabled && !shizukuPermission) {
                            onRequestShizuku()
                            onSettingsChange(settings.copy(shizukuAccess = true))
                        } else {
                            onSettingsChange(settings.copy(shizukuAccess = enabled))
                        }
                    }
                    SettingsActionCard(
                        title = language.t("root_command"),
                        summary = language.t("root_command_summary"),
                        icon = Icons.Outlined.Code,
                        index = 2,
                        count = 5,
                        trailingText = settings.rootCommand,
                        onClick = { rootCommandDialog = true }
                    )
                    SettingSwitch(language.t("hidden"), settings.showHidden, 3, 5, icon = Icons.Outlined.Visibility, supportingText = language.t("hidden_summary")) { onSettingsChange(settings.copy(showHidden = it)) }
                    SettingSwitch(language.t("auto_remount"), settings.autoRemount, 4, 5, enabled = rootAvailable, icon = Icons.Outlined.Terminal, supportingText = language.t("auto_remount_summary")) { onSettingsChange(settings.copy(autoRemount = it)) }
                }
            }
            }
            if (showBehavior) {
                item { SettingsSection(language.t("behavior")) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingSwitch(language.t("compact"), settings.compactMode, 0, 5, icon = Icons.AutoMirrored.Outlined.List, supportingText = language.t("compact_summary")) { onSettingsChange(settings.copy(compactMode = it)) }
                    SettingSwitch(language.t("thumbnails"), settings.showThumbnails, 1, 5, icon = Icons.Outlined.Image, supportingText = language.t("thumbnails_summary")) { onSettingsChange(settings.copy(showThumbnails = it)) }
                    SettingSwitch(language.t("haptics"), settings.hapticFeedback, 2, 5, icon = Icons.Outlined.Vibration, supportingText = language.t("haptics_summary")) { onSettingsChange(settings.copy(hapticFeedback = it)) }
                    SettingSwitch(language.t("show_recents"), settings.showRecents, 3, 5, icon = Icons.Outlined.FileOpen, supportingText = language.t("show_recents_summary")) { onSettingsChange(settings.copy(showRecents = it)) }
                    SettingsActionCard(
                        title = language.t("create_button_style"),
                        summary = fabStyleLabel(language, settings.fabStyle),
                        icon = Icons.Outlined.Add,
                        index = 4,
                        count = 5,
                        onClick = { fabStyleDialog = true }
                    )
                }
            }
            }
            if (showInstallation) {
                item { SettingsSection(language.t("installation")) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingSwitch(
                        label = language.t("use_app_installer"),
                        checked = settings.installEnabled,
                        index = 0,
                        count = 6,
                        icon = Icons.Outlined.Settings,
                        supportingText = language.t("use_app_installer_summary")
                    ) { onSettingsChange(settings.copy(installEnabled = it)) }
                    SettingsActionCard(
                        title = language.t("install_method"),
                        summary = installMethodLabel(language, settings.installMethod),
                        icon = Icons.Outlined.Terminal,
                        index = 1,
                        count = 6,
                        enabled = settings.installEnabled,
                        onClick = { installMethodDialog = true }
                    )
                    SettingsActionCard(
                        title = language.t("installer_declaration"),
                        summary = language.t("installer_declaration_summary"),
                        icon = Icons.Outlined.Info,
                        index = 2,
                        count = 6,
                        trailingText = settings.installerPackage.ifBlank { language.t("not_set") },
                        enabled = settings.installEnabled,
                        onClick = { installerDeclarationDialog = true }
                    )
                    SettingSwitch(language.t("allow_downgrade"), settings.allowDowngrade, 3, 6, icon = Icons.Outlined.ArrowDownward, supportingText = language.t("allow_downgrade_summary")) { onSettingsChange(settings.copy(allowDowngrade = it)) }
                    SettingSwitch(language.t("allow_test_packages"), settings.allowTestPackages, 4, 6, icon = Icons.Outlined.Code, supportingText = language.t("allow_test_packages_summary")) { onSettingsChange(settings.copy(allowTestPackages = it)) }
                    SettingSwitch(language.t("install_all_users"), settings.installAllUsers, 5, 6, icon = Icons.Outlined.People, supportingText = language.t("install_all_users_summary")) { onSettingsChange(settings.copy(installAllUsers = it)) }
                }
            }
            }
            if (showAbout) {
                item { SettingsSection(language.t("about")) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingsActionCard(
                        title = language.t("check_updates"),
                        summary = language.t("check_updates_summary"),
                        icon = Icons.Outlined.Refresh,
                        index = 0,
                        count = 2,
                        onClick = onCheckForUpdates
                    )
                    val aboutInteraction = remember { MutableInteractionSource() }
                    val aboutShape = pressedGroupShape(1, 2, aboutInteraction)
                    Surface(shape = aboutShape, color = CardDefaults.elevatedCardColors().containerColor, modifier = Modifier.fillMaxWidth().clip(aboutShape).clickable(interactionSource = aboutInteraction, indication = LocalIndication.current) { showAboutDialog = true }) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Buge Files", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(language.t("file_manager"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            Text("${language.t("version")} ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
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

        if (rootCommandDialog) {
            NameDialog(
                title = language.t("root_command"),
                hint = language.t("root_command"),
                language = language,
                initialValue = settings.rootCommand,
                confirmLabel = language.t("save"),
                onDismiss = { rootCommandDialog = false }
            ) { command ->
                onSettingsChange(settings.copy(rootCommand = command.ifBlank { "su" }))
                rootCommandDialog = false
            }
        }

        if (installMethodDialog) {
            InstallMethodDialog(
                language = language,
                selected = settings.installMethod,
                onSelect = { method -> onSettingsChange(settings.copy(installMethod = method)) },
                onDismiss = { installMethodDialog = false }
            )
        }

        if (installerDeclarationDialog) {
            NameDialog(
                title = language.t("installer_declaration"),
                hint = language.t("installer_package_hint"),
                language = language,
                initialValue = "",
                confirmLabel = language.t("save"),
                onDismiss = { installerDeclarationDialog = false }
            ) { value ->
                onSettingsChange(settings.copy(installerPackage = value.trim()))
                installerDeclarationDialog = false
            }
        }

        if (fabStyleDialog) {
            FabStyleDialog(
                language = language,
                selected = settings.fabStyle,
                onSelect = { style -> onSettingsChange(settings.copy(fabStyle = style)) },
                onDismiss = { fabStyleDialog = false }
            )
        }

        if (showAboutDialog) {
            AboutDialog(language = language, onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
private fun SettingsSection(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
}

private fun groupCorners(index: Int, count: Int): List<Dp> {
    val outer = 24.dp
    val inner = 4.dp
    return when {
        count <= 1 -> listOf(outer, outer, outer, outer)
        index == 0 -> listOf(outer, outer, inner, inner)
        index == count - 1 -> listOf(inner, inner, outer, outer)
        else -> listOf(inner, inner, inner, inner)
    }
}

private fun groupItemShape(index: Int, count: Int): RoundedCornerShape {
    val corners = groupCorners(index, count)
    return RoundedCornerShape(corners[0], corners[1], corners[3], corners[2])
}

@Composable
private fun pressedGroupShape(index: Int, count: Int, interactionSource: MutableInteractionSource): RoundedCornerShape {
    val pressed by interactionSource.collectIsPressedAsState()
    val base = groupCorners(index, count)
    val outer = 24.dp
    val spec = spring<Dp>(stiffness = Spring.StiffnessMediumLow)
    val topStart by animateDpAsState(if (pressed) outer else base[0], spec, label = "topStart")
    val topEnd by animateDpAsState(if (pressed) outer else base[1], spec, label = "topEnd")
    val bottomEnd by animateDpAsState(if (pressed) outer else base[2], spec, label = "bottomEnd")
    val bottomStart by animateDpAsState(if (pressed) outer else base[3], spec, label = "bottomStart")
    return RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
}

@Composable
private fun SettingsActionCard(title: String, summary: String, icon: ImageVector, index: Int, count: Int, trailingText: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = pressedGroupShape(index, count, interactionSource)
    val iconColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val summaryColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    Surface(
        shape = shape,
        color = CardDefaults.elevatedCardColors().containerColor,
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(interactionSource = interactionSource, indication = LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = summaryColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (trailingText != null) {
                Spacer(Modifier.width(12.dp))
                Text(trailingText, style = MaterialTheme.typography.labelLarge, color = summaryColor)
            }
        }
    }
}

@Composable
private fun AboutDialog(language: AppLanguage, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val links = listOf(
        "Website" to "https://bugestudio.website/files/",
        "GitHub" to "https://github.com/BugeStudioTeam/Buge-Files",
        "F-Droid" to "https://f-droid.org/packages/com.buge.files",
        "Telegram" to "https://t.me/bugestudio"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("about")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                links.forEach { (label, url) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            runCatching { uriHandler.openUri(url) }
                            onDismiss()
                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(language.t("done")) } }
    )
}

private fun fabStyleLabel(language: AppLanguage, style: FabStyle): String = when (style) {
    FabStyle.EXTENDED -> language.t("fab_style_extended")
    FabStyle.ICON -> language.t("fab_style_icon")
    FabStyle.MENU -> language.t("fab_style_menu")
}

@Composable
private fun FabStyleDialog(language: AppLanguage, selected: FabStyle, onSelect: (FabStyle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("create_button_style")) },
        text = {
            Column {
                FabStyle.entries.forEach { style ->
                    AppearanceOptionRow(
                        label = fabStyleLabel(language, style),
                        selected = selected == style,
                        onSelect = { onSelect(style) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(language.t("done")) } }
    )
}

private fun installMethodLabel(language: AppLanguage, method: InstallMethod): String = when (method) {
    InstallMethod.SHIZUKU -> language.t("install_method_shizuku")
    InstallMethod.ROOT -> language.t("install_method_root")
}

@Composable
private fun InstallMethodDialog(language: AppLanguage, selected: InstallMethod, onSelect: (InstallMethod) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("install_method")) },
        text = {
            Column {
                InstallMethod.entries.forEach { method ->
                    AppearanceOptionRow(
                        label = installMethodLabel(language, method),
                        selected = selected == method,
                        onSelect = { onSelect(method) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(language.t("done")) } }
    )
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
private fun SettingSwitch(label: String, checked: Boolean, index: Int, count: Int, enabled: Boolean = true, icon: ImageVector? = null, supportingText: String? = null, onChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = pressedGroupShape(index, count, interactionSource)
    val labelColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val supportColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    val iconColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    Surface(shape = shape, color = CardDefaults.elevatedCardColors().containerColor, modifier = Modifier.fillMaxWidth().clip(shape).clickable(interactionSource = interactionSource, indication = LocalIndication.current, enabled = enabled, role = Role.Switch) { onChange(!checked) }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = labelColor)
                if (supportingText != null) Text(supportingText, style = MaterialTheme.typography.bodySmall, color = supportColor)
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}

private fun taskIcon(type: TaskType): ImageVector = when (type) {
    TaskType.COPY -> Icons.Outlined.ContentCopy
    TaskType.MOVE -> Icons.AutoMirrored.Outlined.DriveFileMove
    TaskType.COMPRESS -> Icons.Outlined.Archive
}

private fun taskTitle(task: FileTask, language: AppLanguage): String = when (task.type) {
    TaskType.COMPRESS -> task.archiveName ?: language.t("compress")
    TaskType.MOVE -> "${language.t("move")} ${task.entries.firstOrNull()?.name.orEmpty()}".trim()
    TaskType.COPY -> "${language.t("copy")} ${task.entries.firstOrNull()?.name.orEmpty()}".trim()
}

private fun taskStateLabel(task: FileTask, language: AppLanguage): String = when (task.state) {
    TaskState.PENDING -> language.t("task_pending")
    TaskState.RUNNING -> language.t("task_running")
    TaskState.COMPLETED -> language.t("task_completed")
    TaskState.FAILED -> task.detail ?: language.t("task_failed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListSheet(language: AppLanguage, tasks: List<FileTask>, activeId: Long?, onActivate: (Long) -> Unit, onRemove: (Long) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(language.t("task_list"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (tasks.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Outlined.DeleteSweep, language.t("clear_tasks")) }
            }
            if (tasks.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Pending, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(language.t("no_tasks"), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(language.t("no_tasks_sub"), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                Text(language.t("task_copy_hint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        val active = task.id == activeId
                        val shape = RoundedCornerShape(18.dp)
                        Surface(
                            shape = shape,
                            color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth().clip(shape).clickable { onActivate(task.id) }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(taskIcon(task.type), null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(taskTitle(task, language), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${task.entries.size} ${language.t("items")} · ${taskStateLabel(task, language)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (active) Text(language.t("active_task"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { onRemove(task.id) }) { Icon(Icons.Outlined.Close, language.t("remove_task")) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SortAndViewSheet(language: AppLanguage, sortOption: SortOption, viewMode: ViewMode, showHidden: Boolean, onSort: (SortOption) -> Unit, onView: (ViewMode) -> Unit, onToggleHidden: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(language.t("sort"), style = MaterialTheme.typography.headlineSmall)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SortOption.entries.forEachIndexed { index, option ->
                    SortOptionRow(
                        language = language,
                        option = option,
                        index = index,
                        count = SortOption.entries.size,
                        selected = option == sortOption,
                        onSelect = { onSort(option) }
                    )
                }
            }
            HorizontalDivider(); Text(language.t("view"), style = MaterialTheme.typography.titleLarge)
            ChoiceRow(listOf(ViewMode.LIST to language.t("list"), ViewMode.GRID to language.t("grid")), viewMode, onView)
            SettingSwitch(language.t("hidden"), showHidden, 0, 1) { onToggleHidden() }
        }
    }
}

@Composable
private fun SortOptionRow(language: AppLanguage, option: SortOption, index: Int, count: Int, selected: Boolean, onSelect: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = pressedGroupShape(index, count, interactionSource)
    Surface(
        shape = shape,
        color = CardDefaults.elevatedCardColors().containerColor,
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(interactionSource = interactionSource, indication = LocalIndication.current, role = Role.Button, onClick = onSelect)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(sortLabel(language, option), style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun sortLabel(language: AppLanguage, option: SortOption): String = when (option) {
    SortOption.NAME -> language.t("sort_name")
    SortOption.DATE -> language.t("sort_date")
    SortOption.SIZE -> language.t("sort_size")
    SortOption.TYPE -> language.t("sort_type")
}


@Composable
private fun NameDialog(title: String, hint: String, language: AppLanguage, initialValue: String = "", confirmLabel: String = language.t("create"), onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(hint) }, singleLine = true) }, confirmButton = { TextButton(onClick = { onConfirm(value.trim()) }, enabled = value.trim().isNotEmpty()) { Text(confirmLabel) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(language.t("cancel")) } })
}

@Composable
private fun SmbConnectionDialog(
    language: AppLanguage,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConnect: (String, String, String, String, String, Int) -> Unit
) {
    var host by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("445") }
    val canConnect = host.isNotBlank() && share.isNotBlank() && !loading
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        icon = { Icon(Icons.Outlined.Storage, null) },
        title = { Text(language.t("smb_title")) },
        text = {
            Column {
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text(language.t("smb_host")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = share, onValueChange = { share = it }, label = { Text(language.t("smb_share")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text(language.t("smb_user")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(language.t("smb_password")) }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = domain, onValueChange = { domain = it }, label = { Text(language.t("smb_domain")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() } }, label = { Text(language.t("smb_port")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConnect(host.trim(), share.trim(), user, password, domain.trim(), port.toIntOrNull() ?: 445) },
                enabled = canConnect
            ) { Text(if (loading) language.t("smb_testing") else language.t("smb_connect")) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text(language.t("cancel")) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileDetailsSheet(file: FileEntry, language: AppLanguage, onDismiss: () -> Unit, onOpen: () -> Unit, onOpenTool: () -> Unit, onChecksum: () -> Unit, onShare: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { FileGlyph(iconFor(file), file.isDirectory, 34.dp, entry = file); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(file.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(if (file.isDirectory) language.t("folder") else file.mimeType.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            Spacer(Modifier.height(20.dp)); DetailLine(language.t("size"), if (file.isDirectory) directorySummary(file, language) else formatBytes(file.size)); DetailLine(language.t("modified"), formatDate(file.lastModified)); DetailLine(language.t("type"), if (file.isDirectory) language.t("folder") else file.extension.uppercase())
            Spacer(Modifier.height(18.dp)); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!file.isDirectory) {
                    if (file.isApkPackage() || file.isImageFile() || file.isZipContainer() || file.isEditableText()) FilledTonalButton(onClick = onOpenTool) { Icon(if (file.isApkPackage() || file.isZipContainer()) Icons.Outlined.Archive else if (file.isImageFile()) Icons.Outlined.Image else Icons.Outlined.Description, null); Spacer(Modifier.width(6.dp)); Text(if (file.isApkPackage()) language.t("apk_package") else if (file.isImageFile()) language.t("image_preview") else if (file.isZipContainer()) language.t("archive") else language.t("edit"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    OutlinedButton(onClick = onOpen) { Icon(Icons.Outlined.FileOpen, null); Spacer(Modifier.width(6.dp)); Text(language.t("open"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    OutlinedButton(onClick = onShare) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(6.dp)); Text(language.t("share"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    OutlinedButton(onClick = onChecksum) { Icon(Icons.Outlined.Info, null); Spacer(Modifier.width(6.dp)); Text("SHA-256", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                }
                OutlinedButton(onClick = onRename) { Text(language.t("rename"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }; TextButton(onClick = onDelete) { Text(language.t("delete"), color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun ApkInspectorSheet(file: FileEntry, metadata: ApkMetadata?, loading: Boolean, installing: Boolean, installed: Boolean, language: AppLanguage, onInstall: () -> Unit, onOpen: () -> Unit, onDismiss: () -> Unit) {
    var showPermissions by remember { mutableStateOf(false) }
    var showSignature by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FileGlyph(Icons.Outlined.Archive, false, 52.dp, entry = file)
                Spacer(Modifier.height(12.dp))
                Text(
                    metadata?.displayName ?: file.name,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (metadata != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        metadata.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${language.t("version")} ${metadata.versionName} (${metadata.versionCode})",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                    if (metadata.isInstalled) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${metadata.installedVersionName ?: "—"} (${metadata.installedVersionCode ?: "—"}) → ${metadata.versionName} (${metadata.versionCode})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (metadata.isNewerThanInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        ApkPill("Min: API ${metadata.minSdk}")
                        ApkPill("Target: API ${metadata.targetSdk}")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        ApkPill("${language.t("size")}: ${formatBytes(file.size)}")
                    }
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { showPermissions = true }, shape = CircleShape) { Text(language.t("view_all_permissions")) }
                        OutlinedButton(onClick = { showSignature = true }, shape = CircleShape) { Text(language.t("view_signature")) }
                    }
                } else if (!loading) {
                    Spacer(Modifier.height(12.dp))
                    Text("APK metadata is unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                if (loading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (installing) {
                    Spacer(Modifier.height(18.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FilledTonalButton(
                            onClick = { if (installed) onOpen() else onInstall() },
                            enabled = metadata != null && !loading,
                            shape = groupItemShape(0, 2),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) { Text(if (installed) language.t("open") else language.t("install"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                        FilledTonalButton(
                            onClick = onDismiss,
                            shape = groupItemShape(1, 2),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) { Text(language.t("cancel"), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }
    }
    if (showPermissions) {
        PermissionListDialog(title = language.t("requested_permissions"), permissions = metadata?.requestedPermissions.orEmpty(), language = language, onDismiss = { showPermissions = false })
    }
    if (showSignature) {
        SignatureDialog(title = language.t("signing_certificate"), signatures = metadata?.certificateSha256.orEmpty(), language = language, onDismiss = { showSignature = false })
    }
}

@Composable
private fun ApkPill(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun PermissionListDialog(title: String, permissions: List<String>, language: AppLanguage, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (permissions.isEmpty()) {
                Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(permissions) { permission ->
                        Text(permission, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(language.t("done")) } }
    )
}

@Composable
private fun SignatureDialog(title: String, signatures: List<String>, language: AppLanguage, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (signatures.isEmpty()) {
                Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    signatures.forEach { signature ->
                        SelectionContainer {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Text(signature, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(12.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(language.t("done")) } }
    )
}

@Composable
private fun CodeEditorScreen(file: FileEntry, content: String, loading: Boolean, language: AppLanguage, onContentChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(modifier = Modifier.fillMaxSize().imePadding(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, language.t("close")) }
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${codeLanguage(file)} · ${content.count { it == '\n' } + 1} ${language.t("lines")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = onSave, enabled = !loading) { Text(language.t("save")) }
                }
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
                } else {
                    val editorStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    val gutterStyle = editorStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val logicalLines = (content.count { it == '\n' } + 1).coerceAtLeast(1)
                    var gutterText by remember { mutableStateOf((1..logicalLines).joinToString("\n")) }
                    val editorScroll = rememberScrollState()
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp).verticalScroll(editorScroll)
                    ) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.width(64.dp)) {
                            Text(
                                text = gutterText,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
                                textAlign = TextAlign.End,
                                style = gutterStyle
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = content,
                            onValueChange = onContentChange,
                            modifier = Modifier.weight(1f),
                            textStyle = editorStyle,
                            singleLine = false,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            onTextLayout = { layout ->
                                val updated = buildGutterText(layout, content)
                                if (updated != gutterText) gutterText = updated
                            },
                            decorationBox = { innerTextField ->
                                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 16.dp)) {
                                    if (content.isEmpty()) Text(language.t("code_editor"), style = editorStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun buildGutterText(layout: TextLayoutResult, content: String): String {
    val builder = StringBuilder()
    var logical = 0
    for (index in 0 until layout.lineCount) {
        val start = layout.getLineStart(index)
        val startsLogicalLine = start == 0 || content.getOrNull(start - 1) == '\n'
        if (startsLogicalLine) logical++
        if (index > 0) builder.append('\n')
        if (startsLogicalLine) builder.append(logical)
    }
    return builder.toString()
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

private fun fileSecondaryText(file: FileEntry, language: AppLanguage): String = if (file.isDirectory) "${directorySummary(file, language)} · ${formatDate(file.lastModified)}" else "${formatBytes(file.size)} · ${formatDate(file.lastModified)}"
private fun directorySummary(file: FileEntry, language: AppLanguage): String = if (SmbUri.isSmb(file.uri)) "—" else "${file.childCount} ${language.t("items")}"
private fun formatBytes(bytes: Long): String { if (bytes <= 0) return "0 B"; val units = arrayOf("B", "KB", "MB", "GB", "TB"); val exponent = (kotlin.math.ln(bytes.toDouble()) / kotlin.math.ln(1024.0)).toInt().coerceIn(0, units.lastIndex); return "%.1f %s".format(bytes / 1024.0.pow(exponent), units[exponent]) }
private fun Double.pow(exponent: Int): Double = java.lang.Math.pow(this, exponent.toDouble())
private fun formatDate(millis: Long): String = if (millis <= 0L) "—" else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
