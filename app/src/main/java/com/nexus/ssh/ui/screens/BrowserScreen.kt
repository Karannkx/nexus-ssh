package com.nexus.ssh.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexus.ssh.BrowserTarget
import com.nexus.ssh.WebPortsState
import com.nexus.ssh.core.prefs.WebBookmark
import com.nexus.ssh.core.ssh.ListeningPort
import com.nexus.ssh.core.ssh.likelyHttps
import com.nexus.ssh.ui.LocalAccent
import com.nexus.ssh.ui.Nx
import com.nexus.ssh.ui.components.ActionSheet
import com.nexus.ssh.ui.components.EmptyState
import com.nexus.ssh.ui.components.ErrorBox
import com.nexus.ssh.ui.components.GroupSurface
import com.nexus.ssh.ui.components.IconBadge
import com.nexus.ssh.ui.components.ListRow
import com.nexus.ssh.ui.components.LoadingBar
import com.nexus.ssh.ui.components.RowDivider
import com.nexus.ssh.ui.components.ScreenTopBar
import com.nexus.ssh.ui.components.SectionHeader
import com.nexus.ssh.ui.components.SheetAction
import com.nexus.ssh.ui.nexusFieldColors

private val KNOWN_WEB = mapOf(
    80 to "HTTP", 81 to "Nginx Proxy Manager", 443 to "HTTPS", 631 to "CUPS printing", 3000 to "Grafana / Gitea / Node app",
    3001 to "Uptime Kuma", 4200 to "Angular dev server", 5000 to "Flask / Registry", 5173 to "Vite dev server",
    5601 to "Kibana", 5678 to "n8n", 7860 to "Gradio", 8000 to "HTTP dev server", 8080 to "HTTP (alt)",
    8081 to "HTTP (alt)", 8096 to "Jellyfin", 8123 to "Home Assistant", 8443 to "HTTPS (alt)",
    8501 to "Streamlit", 8888 to "Jupyter", 9000 to "Portainer / web app", 9090 to "Cockpit / Prometheus",
    9443 to "Portainer (HTTPS)", 15672 to "RabbitMQ", 32400 to "Plex"
)

private val KNOWN_OTHER = mapOf(
    22 to "SSH", 25 to "SMTP", 53 to "DNS", 110 to "POP3", 143 to "IMAP", 993 to "IMAPS", 995 to "POP3S",
    3306 to "MySQL / MariaDB", 5432 to "PostgreSQL", 6379 to "Redis", 11211 to "Memcached", 27017 to "MongoDB"
)

private val WEB_PROCESSES = listOf(
    "nginx", "apache", "httpd", "caddy", "node", "python", "java", "php", "ruby", "docker-proxy",
    "traefik", "gunicorn", "uvicorn", "lighttpd", "go", "dotnet", "deno", "bun"
)

private fun portTitle(p: ListeningPort): String {
    val name = KNOWN_WEB[p.port] ?: KNOWN_OTHER[p.port]
    return if (name != null) "${p.port} · $name" else "Port ${p.port}"
}

private fun isWebLike(p: ListeningPort): Boolean =
    p.port in KNOWN_WEB || (p.process != null && WEB_PROCESSES.any { p.process.contains(it, ignoreCase = true) })

private fun isExposed(p: ListeningPort): Boolean = p.bind == "0.0.0.0" || p.bind == "*" || p.bind == "::"

@Composable
fun BrowserScreen(
    connected: Boolean,
    serverId: Long?,
    serverName: String,
    ports: WebPortsState,
    bookmarks: List<WebBookmark>,
    target: BrowserTarget?,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onRefreshPorts: () -> Unit,
    onOpenPort: (port: Int, https: Boolean) -> Unit,
    onOpenInput: (String) -> Unit,
    onCloseTarget: () -> Unit,
    onAddBookmark: (WebBookmark) -> Unit,
    onRemoveBookmark: (WebBookmark) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<ListeningPort?>(null) }

    if (target != null) {
        WebPage(target, serverId, serverName, bookmarks, onCloseTarget, onAddBookmark, onRemoveBookmark)
        return
    }

    val accent = LocalAccent.current
    val mine = bookmarks.filter { it.serverId == serverId }
    val shown = ports.ports.filter { showAll || isWebLike(it) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Browser", onBack, subtitle = serverName.ifBlank { null }) {
            IconButton(onClick = onRefreshPorts, enabled = connected) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Scan ports", tint = if (connected) Nx.Green else Nx.Faint)
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = nexusFieldColors(),
            placeholder = { Text("Port, localhost:3000 or any web address") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Nx.Muted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onOpenInput(input) }),
            trailingIcon = {
                TextButton(onClick = { onOpenInput(input) }, enabled = input.isNotBlank()) {
                    Text("Go", color = accent, fontWeight = FontWeight.Bold)
                }
            }
        )
        Text(
            "Apps running on the server open through a secure SSH tunnel, so even ports that are closed to the internet work.",
            color = Nx.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
        )
        LoadingBar(ports.loading)

        if (!connected) {
            EmptyState(
                icon = Icons.Rounded.Language,
                title = "Connect to use server apps",
                message = "Type a normal web address above to browse, or connect to a server to open its web apps.",
                actionLabel = "Connect",
                onAction = onConnect
            )
            return@Column
        }

        PullToRefreshBox(isRefreshing = ports.loading, onRefresh = onRefreshPorts, modifier = Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                if (mine.isNotEmpty()) {
                    item { SectionHeader("Saved") }
                    item {
                        GroupSurface {
                            mine.forEachIndexed { i, b ->
                                ListRow(
                                    title = b.label,
                                    subtitle = b.input,
                                    leading = { IconBadge(Icons.Rounded.Bookmark, Nx.Amber) },
                                    trailing = {
                                        IconButton(onClick = { onRemoveBookmark(b) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = Nx.Muted)
                                        }
                                    },
                                    onClick = { onOpenInput(b.input) }
                                )
                                if (i != mine.lastIndex) RowDivider()
                            }
                        }
                    }
                }
                item { SectionHeader("Detected on this server") }
                ports.error?.let { item { ErrorBox(it, onRetry = onRefreshPorts) } }
                if (ports.error == null && shown.isEmpty() && !ports.loading) {
                    item {
                        Text(
                            if (ports.ports.isEmpty()) "Pull down to scan for listening ports." else "No web apps found. Show all ports to see everything.",
                            color = Nx.Muted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                if (shown.isNotEmpty()) {
                    item {
                        GroupSurface {
                            shown.forEachIndexed { i, p ->
                                ListRow(
                                    title = portTitle(p),
                                    subtitle = buildString {
                                        append(p.process ?: "unknown process")
                                        append(" · ")
                                        append(if (isExposed(p)) "open to the network" else "local only")
                                    },
                                    leading = { IconBadge(Icons.Rounded.Language, if (isWebLike(p)) Nx.Cyan else Nx.Muted) },
                                    trailing = {
                                        IconButton(onClick = { menuFor = p }) {
                                            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Nx.Muted)
                                        }
                                    },
                                    onClick = { onOpenPort(p.port, likelyHttps(p.port)) }
                                )
                                if (i != shown.lastIndex) RowDivider()
                            }
                        }
                    }
                }
                if (ports.ports.isNotEmpty()) {
                    item {
                        TextButton(onClick = { showAll = !showAll }, modifier = Modifier.padding(top = 6.dp)) {
                            Text(if (showAll) "Show web apps only" else "Show all ${ports.ports.size} ports", color = accent)
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { p ->
        ActionSheet(
            title = portTitle(p),
            subtitle = "Opens through an SSH tunnel",
            actions = listOf(
                SheetAction("Open as HTTP", Icons.Rounded.Language) { onOpenPort(p.port, false) },
                SheetAction("Open as HTTPS", Icons.Rounded.Language) { onOpenPort(p.port, true) },
                SheetAction("Save to bookmarks", Icons.Rounded.BookmarkBorder) {
                    onAddBookmark(WebBookmark(serverId ?: -1L, portTitle(p), if (likelyHttps(p.port)) "https://localhost:${p.port}" else "localhost:${p.port}"))
                }
            ),
            onDismiss = { menuFor = null }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebPage(
    target: BrowserTarget,
    serverId: Long?,
    serverName: String,
    bookmarks: List<WebBookmark>,
    onClose: () -> Unit,
    onAddBookmark: (WebBookmark) -> Unit,
    onRemoveBookmark: (WebBookmark) -> Unit
) {
    val context = LocalContext.current
    var progress by remember(target.url) { mutableIntStateOf(0) }
    var currentUrl by remember(target.url) { mutableStateOf(target.url) }
    var pageTitle by remember(target.url) { mutableStateOf(target.label) }
    var canBack by remember(target.url) { mutableStateOf(false) }
    var canForward by remember(target.url) { mutableStateOf(false) }

    val webView = remember(target.url) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            setBackgroundColor(android.graphics.Color.WHITE)
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    if (url != null) currentUrl = url
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url != null) currentUrl = url
                    canBack = view?.canGoBack() == true
                    canForward = view?.canGoForward() == true
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                // The tunnel to 127.0.0.1 is already authenticated by SSH, so self-signed certificates
                // of server apps (Portainer, Cockpit, ...) are accepted there and nowhere else.
                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    val host = try {
                        Uri.parse(error?.url ?: "").host
                    } catch (e: Exception) {
                        null
                    }
                    if (host == "127.0.0.1") handler?.proceed() else handler?.cancel()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) pageTitle = title
                }
            }
            loadUrl(target.url)
        }
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    BackHandler { if (webView.canGoBack()) webView.goBack() else onClose() }

    val savedInput = if (target.remotePort != null) {
        if (target.url.startsWith("https")) "https://localhost:${target.remotePort}" else "localhost:${target.remotePort}"
    } else currentUrl
    val bookmark = bookmarks.firstOrNull { it.serverId == (serverId ?: -1L) && it.input == savedInput }
    val shownUrl = if (target.remotePort != null) {
        currentUrl.replace(Regex("^(https?://)127\\.0\\.0\\.1:\\d+"), "$1${serverName.ifBlank { "server" }}:${target.remotePort}")
    } else currentUrl

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp).height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close page", tint = Nx.Text) }
            Column(Modifier.weight(1f)) {
                Text(pageTitle, color = Nx.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(shownUrl, color = Nx.Muted, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = { webView.reload() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = Nx.Text) }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = LocalAccent.current, trackColor = Color.Transparent)
        } else {
            Spacer(Modifier.height(3.dp))
        }
        AndroidView(factory = { webView }, modifier = Modifier.weight(1f).fillMaxWidth())
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { webView.goBack() }, enabled = canBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = if (canBack) Nx.Green else Nx.Faint)
            }
            IconButton(onClick = { webView.goForward() }, enabled = canForward) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward", tint = if (canForward) Nx.Green else Nx.Faint)
            }
            IconButton(onClick = {
                if (bookmark != null) onRemoveBookmark(bookmark)
                else onAddBookmark(WebBookmark(serverId ?: -1L, pageTitle.take(40), savedInput))
            }) {
                Icon(
                    if (bookmark != null) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (bookmark != null) Nx.Amber else Nx.Green
                )
            }
        }
    }
}
