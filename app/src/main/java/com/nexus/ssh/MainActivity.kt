package com.nexus.ssh

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.ui.CubeIcon
import com.nexus.ssh.ui.GridIcon
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.NexusBackdrop
import com.nexus.ssh.ui.NexusTheme
import com.nexus.ssh.ui.PulseIcon
import com.nexus.ssh.ui.Routes
import com.nexus.ssh.ui.ServersIcon
import com.nexus.ssh.ui.accentColor
import com.nexus.ssh.ui.components.DialogShape
import com.nexus.ssh.ui.components.FloatingNavBar
import com.nexus.ssh.ui.components.NavItemSpec
import com.nexus.ssh.ui.screens.AppsScreen
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
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val vm: NexusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
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
            NexusTheme(accentColor(accentKey)) {
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

private val TOP_ROUTES = setOf(Routes.HOME, Routes.SERVERS, Routes.APPS, Routes.SETTINGS)
private val WALLPAPER_ROUTES = setOf(Routes.HOME, Routes.SERVERS, Routes.APPS)

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
    val files by vm.files.collectAsState()
    val openFile by vm.openFile.collectAsState()
    val transfer by vm.transfer.collectAsState()
    val docker by vm.docker.collectAsState()
    val dockerDetails by vm.dockerDetails.collectAsState()
    val services by vm.services.collectAsState()
    val logs by vm.logs.collectAsState()
    val notices by vm.notices.collectAsState()
    val unread by vm.unreadNotices.collectAsState()

    val displayName by prefs.displayName.collectAsState()
    val wallpaperPref by prefs.showWallpaper.collectAsState()
    val icons by prefs.serverIcons.collectAsState()
    val favorites by prefs.favorites.collectAsState()
    val fontSize by prefs.terminalFontSize.collectAsState()
    val showHidden by prefs.showHiddenFiles.collectAsState()
    val appLock by prefs.appLock.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    val uriHandler = LocalUriHandler.current

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
    val connectedId = (connection as? ConnectionState.Connected)?.server?.id
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val addServer: () -> Unit = { editTarget = null; showEditor = true }
    val connectSelected: () -> Unit = { vm.connect() }

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
                        contentColor = Color.White
                    )
                }
            },
            bottomBar = {
                if (!imeVisible) {
                    FloatingNavBar(
                        items = if (route in TOP_ROUTES) TOP_NAV else TOOL_NAV,
                        selectedRoute = route,
                        onSelect = { navigateTo(it.route) }
                    )
                }
            }
        ) { inner ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(inner).imePadding(),
                enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 } },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 24 } }
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        displayName = displayName,
                        servers = servers,
                        selected = selected,
                        connection = connection,
                        telemetry = telemetry,
                        unread = unread,
                        onNavigate = { navigateTo(it) },
                        onSearch = { showSearch = true },
                        onNotifications = { vm.markNoticesRead(); showNotices = true },
                        onConnect = connectSelected,
                        onDisconnect = vm::disconnect,
                        onAddServer = addServer
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
                        onOpenUrl = { uriHandler.openUri(it) }
                    )
                }
                composable(Routes.FILES) {
                    FilesScreen(
                        connected = isConnected,
                        serverName = selected?.name ?: "",
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
                        serverName = selected?.name ?: "",
                        tabs = tabs,
                        activeTab = activeTab,
                        lines = terminalLines,
                        fontSize = fontSize,
                        onBack = goHome,
                        onConnect = connectSelected,
                        onSelectTab = vm::selectTerminalTab,
                        onNewTab = vm::newTerminalTab,
                        onCloseTab = vm::closeTerminalTab,
                        onSendLine = { vm.sendTerminalInput(it + "\r") },
                        onSendRaw = vm::sendTerminalInput,
                        onClear = vm::clearTerminal,
                        onResize = vm::resizeTerminal
                    )
                }
                composable(Routes.DOCKER) {
                    LaunchedEffect(isConnected) { if (isConnected) vm.refreshDocker() }
                    DockerScreen(
                        connected = isConnected,
                        state = docker,
                        details = dockerDetails,
                        onBack = goHome,
                        onConnect = connectSelected,
                        onRefresh = vm::refreshDocker,
                        onAction = vm::dockerAction,
                        onOpenDetails = vm::openDockerDetails,
                        onCloseDetails = vm::closeDockerDetails
                    )
                }
                composable(Routes.SERVICES) {
                    LaunchedEffect(isConnected) { if (isConnected) vm.refreshServices() }
                    ServicesScreen(
                        connected = isConnected,
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
                        state = logs,
                        onBack = goHome,
                        onConnect = connectSelected,
                        onLoadSource = vm::loadLogs,
                        onLoadFile = vm::loadLogFile
                    )
                }
            }
        }

        // ---------------- dialogs & sheets
        if (showEditor) {
            val target = editTarget
            ServerEditorDialog(
                initial = target,
                initialIconKey = target?.let { icons[it.id] } ?: "globe",
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

        if (!loaded && servers.isEmpty()) {
            // first frame while Room loads: keep the backdrop only
        }

        if (locked && appLock) {
            LockScreen(onUnlock = { authenticate { ok -> if (ok) locked = false } })
        }
    }
}
