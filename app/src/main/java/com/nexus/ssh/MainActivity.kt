package com.nexus.ssh

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.prefs.WebBookmark
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.core.ssh.likelyHttps
import com.nexus.ssh.ui.CubeIcon
import com.nexus.ssh.ui.GridIcon
import com.nexus.ssh.ui.NexusBackdrop
import com.nexus.ssh.ui.NexusTheme
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.PulseIcon
import com.nexus.ssh.ui.Routes
import com.nexus.ssh.ui.ServersIcon
import com.nexus.ssh.ui.accentColor
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.FloatingNavBar
import com.nexus.ssh.ui.components.FloatingNavRail
import com.nexus.ssh.ui.components.NavItemSpec
import com.nexus.ssh.ui.components.NexusLoader
import com.nexus.ssh.ui.components.StartupExperience
import com.nexus.ssh.ui.screens.AboutScreen
import com.nexus.ssh.ui.screens.AppsScreen
import com.nexus.ssh.ui.screens.ProcessesScreen
import com.nexus.ssh.ui.screens.SnippetsScreen
import com.nexus.ssh.ui.screens.BrowserScreen
import com.nexus.ssh.ui.screens.DockerScreen
import com.nexus.ssh.ui.screens.FilesScreen
import com.nexus.ssh.ui.screens.HomeScreen
import com.nexus.ssh.ui.screens.HostKeyDialog
import com.nexus.ssh.ui.screens.IconPickerDialog
import com.nexus.ssh.ui.screens.LockScreen
import com.nexus.ssh.ui.screens.LogsScreen
import com.nexus.ssh.ui.screens.MonitorScreen
import com.nexus.ssh.ui.screens.NoticesSheet
import com.nexus.ssh.ui.screens.SearchDialog
import com.nexus.ssh.ui.screens.ServerEditorDialog
import com.nexus.ssh.ui.screens.ServersScreen
import com.nexus.ssh.ui.screens.ServicesScreen
import com.nexus.ssh.ui.screens.SettingsScreen
import com.nexus.ssh.ui.screens.TerminalScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private val vm: NexusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Let Compose render the branded initializing screen immediately; do not hold the
        // platform splash (which otherwise displays the launcher icon) while data loads.

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        lifecycleScope.launch {
            vm.prefs.secureScreen.collect { secure ->
                if (secure) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        setContent {
            val accentKey by vm.prefs.accent.collectAsState()
            val darkTheme by vm.prefs.darkTheme.collectAsState()
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            NexusTheme(accentColor(accentKey), dark = darkTheme) {
                NexusApp(
                    vm = vm,
                    canAuthenticate = ::canAuthenticate,
                    authenticate = ::authenticate
                )
            }
        }
    }

    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    private fun canAuthenticate(): Boolean =
        BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    private fun authenticate(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false)
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Nexus SSH")
            .setSubtitle("Confirm it's you to open your servers")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}

private val TOP_ROUTES = setOf(Routes.HOME, Routes.SERVERS, Routes.APPS, Routes.SETTINGS, Routes.ABOUT)
private val WALLPAPER_ROUTES = setOf(Routes.HOME, Routes.SERVERS, Routes.APPS, Routes.SETTINGS)

private val TOP_NAV = listOf(
    NavItemSpec(Routes.HOME, Icons.Rounded.Home, "Home"),
    NavItemSpec(Routes.SERVERS, ServersIcon, "Servers"),
    NavItemSpec(Routes.APPS, GridIcon, "Apps"),
    NavItemSpec(Routes.SETTINGS, Icons.Rounded.Settings, "Settings")
)

private val TOOL_NAV = listOf(
    NavItemSpec(Routes.HOME, Icons.Rounded.Home, "Home"),
    NavItemSpec(Routes.SERVERS, ServersIcon, "Servers"),
    NavItemSpec(Routes.FILES, Icons.Rounded.Folder, "Files"),
    NavItemSpec(Routes.MONITOR, PulseIcon, "Monitor"),
    NavItemSpec(Routes.DOCKER, CubeIcon, "Docker")
)

@Composable
fun NexusApp(
    vm: NexusViewModel,
    canAuthenticate: () -> Boolean,
    authenticate: (onResult: (Boolean) -> Unit) -> Unit
) {
    val prefs = vm.prefs
    val servers by vm.servers.collectAsState()
    val loaded by vm.serversLoaded.collectAsState()
    val selected by vm.selectedServer.collectAsState()
    val connection by vm.connectionState.collectAsState()
    val telemetry by vm.telemetry.collectAsState()
    val metrics by vm.metrics.collectAsState()
    val tabs by vm.terminalTabs.collectAsState()
    val activeTab by vm.activeTerminalTab.collectAsState()
    val terminalLines by vm.terminalLines.collectAsState()
    val terminalCursor by vm.terminalCursor.collectAsState()
    val files by vm.files.collectAsState()
    val openFile by vm.openFile.collectAsState()
    val transfer by vm.transfer.collectAsState()
    val docker by vm.docker.collectAsState()
    val dockerDetails by vm.dockerDetails.collectAsState()
    val services by vm.services.collectAsState()
    val logs by vm.logs.collectAsState()
    val webPorts by vm.webPorts.collectAsState()
    val processes by vm.processes.collectAsState()
    val browserTarget by vm.browserTarget.collectAsState()
    val notices by vm.notices.collectAsState()
    val unread by vm.unreadNotices.collectAsState()

    val displayName by prefs.displayName.collectAsState()
    val wallpaperPref by prefs.showWallpaper.collectAsState()
    val icons by prefs.serverIcons.collectAsState()
    val favorites by prefs.favorites.collectAsState()
    val fontSize by prefs.terminalFontSize.collectAsState()
    val showHidden by prefs.showHiddenFiles.collectAsState()
    val appLock by prefs.appLock.collectAsState()
    val tileOrder by prefs.homeTileOrder.collectAsState()
    val bookmarks by prefs.webBookmarks.collectAsState()
    val snippets by prefs.snippets.collectAsState()

    var startupStage by rememberSaveable { mutableStateOf(0) }
    var startupVisible by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(loaded) {
        if (loaded && startupVisible) {
            startupStage = 1
            delay(1500)
            startupVisible = false
        }
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    val uriHandler = LocalUriHandler.current

    // ---------------- private key import (picker lives here, not inside a dialog, and can never crash the app)
    var keyReceiver by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val keyPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val receiver = keyReceiver
        keyReceiver = null
        if (uri != null && receiver != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            if (bytes.size > 100_000) null else String(bytes, Charsets.UTF_8)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (text != null && text.contains("PRIVATE KEY")) receiver(text)
                else snackbar.showSnackbar("That file does not look like an OpenSSH or PEM private key")
            }
        }
    }
    val pickKeyFile: ((String) -> Unit) -> Unit = { callback ->
        keyReceiver = callback
        try {
            keyPicker.launch("*/*")
        } catch (e: Exception) {
            keyReceiver = null
            scope.launch { snackbar.showSnackbar("No file manager found. Use Paste to insert the key instead.") }
        }
    }

    // ---------------- app lock
    var locked by rememberSaveable { mutableStateOf(prefs.appLock.value) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appLock) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && appLock) locked = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(locked, appLock) {
        if (locked && appLock) authenticate { ok -> if (ok) locked = false }
        if (!appLock) locked = false
    }

    // ---------------- navigation
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.HOME
    fun navigateTo(target: String) {
        if (target == Routes.HOME) {
            navController.popBackStack(Routes.HOME, false)
        } else {
            navController.navigate(target) {
                popUpTo(Routes.HOME) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    val goHome: () -> Unit = { navigateTo(Routes.HOME) }

    // ---------------- dialogs
    var showEditor by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ServerEntity?>(null) }
    var iconTarget by remember { mutableStateOf<ServerEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ServerEntity?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showNotices by remember { mutableStateOf(false) }

    val isConnected = connection is ConnectionState.Connected
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val navItems = if (route in TOP_ROUTES) TOP_NAV else TOOL_NAV

    val addServer: () -> Unit = { editTarget = null; showEditor = true }
    val connectSelected: () -> Unit = { vm.connect() }
    val serverName = selected?.name ?: ""

    Box(Modifier.fillMaxSize()) {
        NexusBackdrop(wallpaper = wallpaperPref && route in WALLPAPER_ROUTES)

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Nx.SurfaceHigh,
                        contentColor = Nx.Text
                    )
                }
            },
            bottomBar = {
                if (!wide && !imeVisible) {
                    FloatingNavBar(items = navItems, selectedRoute = route, onSelect = { navigateTo(it.route) })
                }
            }
        ) { inner ->
            Row(Modifier.fillMaxSize().padding(inner)) {
                if (wide) FloatingNavRail(items = navItems, selectedRoute = route, onSelect = { navigateTo(it.route) })
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier
                            .widthIn(max = 880.dp)
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(if (wide) WindowInsetsSides.End else WindowInsetsSides.Horizontal)
                            )
                            // the terminal pins its own key bar above the keyboard
                            .then(if (route == Routes.TERMINAL) Modifier else Modifier.imePadding()),
                        enterTransition = {
                            fadeIn(tween(260)) + slideInVertically(
                                animationSpec = spring(dampingRatio = 0.86f, stiffness = 300f)
                            ) { it / 14 }
                        },
                        exitTransition = { fadeOut(tween(190)) + slideOutVertically(tween(190)) { -it / 28 } },
                        popEnterTransition = { fadeIn(tween(260)) + slideInVertically(spring(dampingRatio = 0.86f, stiffness = 300f)) { -it / 14 } },
                        popExitTransition = { fadeOut(tween(190)) + slideOutVertically(tween(190)) { it / 28 } }
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                displayName = displayName,
                                servers = servers,
                                selected = selected,
                                connection = connection,
                                telemetry = telemetry,
                                unread = unread,
                                tileOrder = tileOrder,
                                onNavigate = { navigateTo(it) },
                                onSearch = { showSearch = true },
                                onNotifications = { vm.markNoticesRead(); showNotices = true },
                                onConnect = connectSelected,
                                onDisconnect = vm::disconnect,
                                onAddServer = addServer,
                                onReorder = { prefs.setHomeTileOrder(it) }
                            )
                        }
                        composable(Routes.SERVERS) {
                            ServersScreen(
                                servers = servers,
                                selectedId = selected?.id,
                                connection = connection,
                                icons = icons,
                                customFavorites = favorites,
                                onClose = goHome,
                                onSelect = { vm.selectServer(it.id) },
                                onConnect = { vm.connectServer(it) },
                                onDisconnect = vm::disconnect,
                                onAdd = addServer,
                                onEdit = { editTarget = it; showEditor = true },
                                onChangeIcon = { iconTarget = it },
                                onDelete = { deleteTarget = it },
                                onOpenFavorite = { path ->
                                    if (isConnected) vm.filesLoad(path)
                                    navigateTo(Routes.FILES)
                                },
                                onRemoveFavorite = { prefs.removeFavorite(it) },
                                onManage = { navigateTo(Routes.SETTINGS) }
                            )
                        }
                        composable(Routes.APPS) { AppsScreen(onNavigate = { navigateTo(it) }) }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                prefs = prefs,
                                servers = servers,
                                icons = icons,
                                onAddServer = addServer,
                                onEditServer = { editTarget = it; showEditor = true },
                                onDeleteServer = { deleteTarget = it },
                                onClearHostKeys = vm::clearTrustedHostKeys,
                                onToggleAppLock = { enable ->
                                    if (!enable) {
                                        prefs.setAppLock(false)
                                    } else if (canAuthenticate()) {
                                        prefs.setAppLock(true)
                                    } else {
                                        scope.launch { snackbar.showSnackbar("Set up a fingerprint, face unlock or screen lock in Android settings first") }
                                    }
                                },
                                onOpenAbout = { navigateTo(Routes.ABOUT) }
                            )
                        }
                        composable(Routes.ABOUT) {
                            AboutScreen(onBack = { navigateTo(Routes.SETTINGS) }, onOpenUrl = { uriHandler.openUri(it) })
                        }
                        composable(Routes.FILES) {
                            FilesScreen(
                                connected = isConnected,
                                serverName = serverName,
                                serverIconKey = selected?.let { icons[it.id] },
                                state = files,
                                openFile = openFile,
                                transfer = transfer,
                                showHidden = showHidden,
                                onBack = goHome,
                                onConnect = connectSelected,
                                onOpenItem = vm::filesOpen,
                                onNavigate = { vm.filesLoad(it) },
                                onUp = vm::filesUp,
                                onRefresh = vm::filesRefresh,
                                onMkdir = vm::filesMkdir,
                                onCreateFile = vm::filesCreateFile,
                                onDelete = vm::filesDelete,
                                onUpload = vm::filesUpload,
                                onDownload = { item, uri -> vm.filesDownload(item, uri) },
                                onSaveFile = vm::saveOpenFile,
                                onCloseFile = vm::closeOpenFile,
                                onToggleHidden = { prefs.setShowHiddenFiles(!showHidden) },
                                onAddFavorite = {
                                    prefs.addFavorite(it)
                                    scope.launch { snackbar.showSnackbar("Added to favorites") }
                                }
                            )
                        }
                        composable(Routes.MONITOR) {
                            MonitorScreen(
                                servers = servers,
                                activeServer = selected,
                                connection = connection,
                                telemetry = telemetry,
                                samples = metrics,
                                onBack = goHome,
                                onPickServer = { vm.connectServer(it) },
                                onConnect = connectSelected,
                                onRefresh = vm::refreshTelemetry
                            )
                        }
                        composable(Routes.TERMINAL) {
                            TerminalScreen(
                                connected = isConnected,
                                serverName = serverName,
                                tabs = tabs,
                                activeTab = activeTab,
                                lines = terminalLines,
                                cursor = terminalCursor,
                                fontSize = fontSize,
                                onBack = goHome,
                                onConnect = connectSelected,
                                onSelectTab = vm::selectTerminalTab,
                                onNewTab = vm::newTerminalTab,
                                onCloseTab = vm::closeTerminalTab,
                                onSendRaw = vm::sendTerminalInput,
                                onClear = vm::clearTerminal,
                                onResize = vm::resizeTerminal
                            )
                        }
                        composable(Routes.DOCKER) {
                            LaunchedEffect(isConnected) { if (isConnected) vm.refreshDocker() }
                            DockerScreen(
                                connected = isConnected,
                                serverName = serverName,
                                state = docker,
                                details = dockerDetails,
                                onBack = goHome,
                                onConnect = connectSelected,
                                onRefresh = vm::refreshDocker,
                                onAction = vm::dockerAction,
                                onOpenDetails = vm::openDockerDetails,
                                onCloseDetails = vm::closeDockerDetails,
                                onOpenPort = { port ->
                                    vm.closeDockerDetails()
                                    vm.openWebPort(port, likelyHttps(port))
                                    navigateTo(Routes.BROWSER)
                                }
                            )
                        }
                        composable(Routes.SERVICES) {
                            LaunchedEffect(isConnected) { if (isConnected) vm.refreshServices() }
                            ServicesScreen(
                                connected = isConnected,
                                serverName = serverName,
                                state = services,
                                onBack = goHome,
                                onConnect = connectSelected,
                                onRefresh = vm::refreshServices,
                                onAction = vm::serviceAction
                            )
                        }
                        composable(Routes.LOGS) {
                            LogsScreen(
                                connected = isConnected,
                                serverName = serverName,
                                state = logs,
                                onBack = goHome,
                                onConnect = connectSelected,
                                onLoadSource = vm::loadLogs,
                                onLoadFile = vm::loadLogFile
                            )
                        }
                        composable(Routes.PROCESSES) {
                            LaunchedEffect(isConnected) {
                                while (isConnected) {
                                    vm.refreshProcesses()
                                    delay(4_000)
                                }
                            }
                            ProcessesScreen(
                                connected = isConnected,
                                serverName = serverName,
                                state = processes,
                                onBack = goHome,
                                onConnect = connectSelected,
                                onRefresh = vm::refreshProcesses,
                                onKill = { p, sig -> vm.killProcess(p, sig) }
                            )
                        }
                        composable(Routes.SNIPPETS) {
                            SnippetsScreen(
                                snippets = snippets,
                                connected = isConnected,
                                onBack = goHome,
                                onRun = { cmd ->
                                    vm.sendTerminalInput(cmd + "\r")
                                    navigateTo(Routes.TERMINAL)
                                },
                                onSave = { id, title, command -> prefs.saveSnippet(id, title, command) },
                                onDelete = { prefs.removeSnippet(it) }
                            )
                        }
                        composable(Routes.BROWSER) {
                            LaunchedEffect(isConnected) { if (isConnected) vm.refreshWebPorts() }
                            BrowserScreen(
                                connected = isConnected,
                                serverId = selected?.id,
                                serverName = serverName,
                                ports = webPorts,
                                bookmarks = bookmarks,
                                target = browserTarget,
                                onBack = goHome,
                                onConnect = connectSelected,
                                onRefreshPorts = vm::refreshWebPorts,
                                onOpenPort = { port, https -> vm.openWebPort(port, https) },
                                onOpenInput = vm::openBrowserInput,
                                onCloseTarget = vm::closeBrowserTarget,
                                onAddBookmark = { prefs.addBookmark(it) },
                                onRemoveBookmark = { prefs.removeBookmark(it) }
                            )
                        }
                    }
                }
            }
        }

        // ---------------- dialogs & sheets
        if (showEditor) {
            val target = editTarget
            ServerEditorDialog(
                initial = target,
                initialIconKey = target?.let { icons[it.id] } ?: "globe",
                onPickKeyFile = pickKeyFile,
                onDismiss = { showEditor = false },
                onSave = { name, host, port, user, authType, secret, passphrase, iconKey ->
                    if (target == null) vm.addServer(name, host, port, user, authType, secret, passphrase, iconKey)
                    else vm.updateServer(target, name, host, port, user, authType, secret, passphrase, iconKey)
                    showEditor = false
                }
            )
        }

        iconTarget?.let { srv ->
            IconPickerDialog(
                currentKey = icons[srv.id] ?: "globe",
                onPick = { prefs.setServerIcon(srv.id, it); iconTarget = null },
                onDismiss = { iconTarget = null }
            )
        }

        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                shape = DialogShape,
                containerColor = Nx.SurfaceHigh,
                title = { Text("Delete ${target.name}?") },
                text = { Text("The saved credentials and trusted host key for this server will be removed from this device.", color = Nx.Muted) },
                confirmButton = { TextButton(onClick = { vm.deleteServer(target); deleteTarget = null }) { Text("Delete", color = Nx.Red) } },
                dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = Nx.Muted) } }
            )
        }

        val hostKey = connection
        if (hostKey is ConnectionState.HostKeyConfirmationRequired) {
            HostKeyDialog(
                server = hostKey.server,
                keyAlgorithm = hostKey.keyAlgorithm,
                fingerprint = hostKey.fingerprint,
                onTrust = vm::trustHostKey,
                onCancel = vm::dismissHostKeyPrompt
            )
        }

        if (showSearch) {
            SearchDialog(
                servers = servers,
                icons = icons,
                onTool = { showSearch = false; navigateTo(it.route) },
                onServer = { showSearch = false; vm.selectServer(it.id); navigateTo(Routes.HOME) },
                onDismiss = { showSearch = false }
            )
        }

        if (showNotices) {
            NoticesSheet(notices = notices, onClear = vm::clearNotices, onDismiss = { showNotices = false })
        }

        if (!loaded || startupVisible) {
            StartupExperience(if (!loaded) 0 else startupStage)
        }

        if (locked && appLock) {
            LockScreen(onUnlock = { authenticate { ok -> if (ok) locked = false } })
        }
    }
}
