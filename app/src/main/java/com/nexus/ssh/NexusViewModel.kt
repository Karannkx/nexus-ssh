package com.nexus.ssh

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.ssh.core.database.AuthType
import com.nexus.ssh.core.database.NexusDatabase
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.prefs.AppPrefs
import com.nexus.ssh.core.security.KeystoreManager
import com.nexus.ssh.core.sftp.RemoteFileItem
import com.nexus.ssh.core.sftp.SftpManager
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.core.ssh.DOCKER_PS_ARGS
import com.nexus.ssh.core.ssh.DockerContainer
import com.nexus.ssh.core.ssh.LIST_PORTS_CMD
import com.nexus.ssh.core.ssh.PS_CMD
import com.nexus.ssh.core.ssh.ProcessInfo
import com.nexus.ssh.core.ssh.parseProcesses
import com.nexus.ssh.core.ssh.LIST_SERVICES_CMD
import com.nexus.ssh.core.ssh.ListeningPort
import com.nexus.ssh.core.ssh.ServiceUnit
import com.nexus.ssh.core.ssh.SshConnectionManager
import com.nexus.ssh.core.ssh.SshTerminalSession
import com.nexus.ssh.core.ssh.TELEMETRY_SCRIPT
import com.nexus.ssh.core.ssh.Telemetry
import com.nexus.ssh.core.ssh.TerminalBuffer
import com.nexus.ssh.core.ssh.likelyHttps
import com.nexus.ssh.core.ssh.parseDockerPs
import com.nexus.ssh.core.ssh.parseListeningPorts
import com.nexus.ssh.core.ssh.parseServices
import com.nexus.ssh.core.ssh.parseTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

// ---------------------------------------------------------------- UI state models

data class FilesState(
    val path: String = "",
    val entries: List<RemoteFileItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class OpenFile(val path: String, val content: String)

data class DockerState(
    val containers: List<DockerContainer> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class DockerDetails(
    val container: DockerContainer,
    val info: List<Pair<String, String>> = emptyList(),
    val logs: String = "",
    val loading: Boolean = true,
    val error: String? = null
)

data class ServicesState(
    val units: List<ServiceUnit> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

/** One real metrics sample taken from the connected server. */
data class MetricSample(
    val t: Long,
    val cpu: Float,
    val mem: Float,
    val disk: Float,
    val rxBps: Float,
    val txBps: Float
)

data class TerminalTabInfo(val id: Int, val title: String)

data class AppNotice(val id: Long, val time: Long, val text: String)

enum class LogSource(val label: String, val command: String) {
    SYSTEM("System", "journalctl -n 400 --no-pager 2>/dev/null || tail -n 400 /var/log/syslog"),
    KERNEL("Kernel", "journalctl -k -n 400 --no-pager 2>/dev/null || dmesg | tail -n 400"),
    SSH("SSH", "journalctl -u ssh -u sshd -n 400 --no-pager 2>/dev/null || tail -n 400 /var/log/auth.log"),
    WARNINGS("Warnings", "journalctl -b -p warning -n 400 --no-pager")
}

data class ProcessesState(
    val items: List<ProcessInfo> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class WebPortsState(
    val ports: List<ListeningPort> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

/** Page currently shown in the in-app browser (url is already the loopback tunnel url for server apps). */
data class BrowserTarget(val label: String, val url: String, val remotePort: Int?)

data class LogsState(
    val source: LogSource? = LogSource.SYSTEM,
    val customPath: String = "",
    val lines: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

// ---------------------------------------------------------------- ViewModel

class NexusViewModel(private val app: Application) : AndroidViewModel(app) {

    private val dao = NexusDatabase.get(app).serverDao()
    private val keystore = KeystoreManager()
    private val ssh = SshConnectionManager(keystore, app)
    val prefs = AppPrefs(app)

    private var sftp: SftpManager? = null
    private var monitorJob: Job? = null
    private var termCols = 80
    private var termRows = 24

    // ---------- servers ----------
    private val _serversLoaded = MutableStateFlow(false)
    val serversLoaded: StateFlow<Boolean> = _serversLoaded

    val servers: StateFlow<List<ServerEntity>> = dao.getAllServers()
        .onEach { _serversLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val selectedId = MutableStateFlow<Long?>(null)

    val selectedServer: StateFlow<ServerEntity?> = combine(servers, selectedId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val connectionState: StateFlow<ConnectionState> = ssh.connectionState

    // ---------- live data ----------
    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry

    private val history = ArrayList<MetricSample>()
    private val _metrics = MutableStateFlow<List<MetricSample>>(emptyList())
    val metrics: StateFlow<List<MetricSample>> = _metrics

    private val _files = MutableStateFlow(FilesState())
    val files: StateFlow<FilesState> = _files

    private val _openFile = MutableStateFlow<OpenFile?>(null)
    val openFile: StateFlow<OpenFile?> = _openFile

    private val _transfer = MutableStateFlow<String?>(null)
    /** Human readable description of a running upload/download, or null. */
    val transfer: StateFlow<String?> = _transfer

    private val _docker = MutableStateFlow(DockerState())
    val docker: StateFlow<DockerState> = _docker

    private val _dockerDetails = MutableStateFlow<DockerDetails?>(null)
    val dockerDetails: StateFlow<DockerDetails?> = _dockerDetails

    private val _services = MutableStateFlow(ServicesState())
    val services: StateFlow<ServicesState> = _services

    private val _logs = MutableStateFlow(LogsState())
    val logs: StateFlow<LogsState> = _logs

    private val _processes = MutableStateFlow(ProcessesState())
    val processes: StateFlow<ProcessesState> = _processes

    private val _webPorts = MutableStateFlow(WebPortsState())
    val webPorts: StateFlow<WebPortsState> = _webPorts

    private val _browserTarget = MutableStateFlow<BrowserTarget?>(null)
    val browserTarget: StateFlow<BrowserTarget?> = _browserTarget

    // ---------- notifications (in-app activity log) ----------
    private val _notices = MutableStateFlow<List<AppNotice>>(emptyList())
    val notices: StateFlow<List<AppNotice>> = _notices
    private val _unread = MutableStateFlow(0)
    val unreadNotices: StateFlow<Int> = _unread
    private var noticeCounter = 0L

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private fun toast(text: String) {
        _messages.trySend(text)
        noticeCounter++
        _notices.update { (listOf(AppNotice(noticeCounter, System.currentTimeMillis(), text)) + it).take(50) }
        _unread.update { it + 1 }
    }

    fun markNoticesRead() { _unread.value = 0 }
    fun clearNotices() {
        _notices.value = emptyList()
        _unread.value = 0
    }

    private fun describe(e: Throwable): String = e.localizedMessage ?: e.javaClass.simpleName

    // ---------- terminal tabs ----------
    private inner class TermTab(val id: Int, val title: String) {
        val buffer = TerminalBuffer()
        var session: SshTerminalSession? = null
        var job: Job? = null
    }

    private val tabs = ArrayList<TermTab>()
    private var nextTabId = 1

    private val _tabInfos = MutableStateFlow<List<TerminalTabInfo>>(emptyList())
    val terminalTabs: StateFlow<List<TerminalTabInfo>> = _tabInfos

    private val _activeTab = MutableStateFlow(0)
    val activeTerminalTab: StateFlow<Int> = _activeTab

    private val _terminalLines = MutableStateFlow<List<String>>(emptyList())
    val terminalLines: StateFlow<List<String>> = _terminalLines

    private val _terminalCursor = MutableStateFlow(0)
    val terminalCursor: StateFlow<Int> = _terminalCursor

    private fun publishActive() {
        val tab = tabs.firstOrNull { it.id == _activeTab.value }
        _terminalLines.value = tab?.buffer?.snapshot() ?: emptyList()
        _terminalCursor.value = tab?.buffer?.cursorColumn ?: 0
    }

    private fun publishTabs() {
        _tabInfos.value = tabs.map { TerminalTabInfo(it.id, it.title) }
    }

    private fun addTerminalTab() {
        val tab = TermTab(nextTabId, "Shell $nextTabId")
        nextTabId++
        tabs.add(tab)
        publishTabs()
        val session = SshTerminalSession(ssh)
        tab.session = session
        tab.job = viewModelScope.launch {
            // Drain SSH output in short batches. This avoids a Compose snapshot/recomposition
            // for every tiny network packet while keeping interactive output responsive.
            val chunks = Channel<String>(Channel.UNLIMITED)
            val reader = launch { session.terminalOutput.collect { chunks.send(it) } }
            val pending = StringBuilder()
            try {
                while (isActive) {
                    val first = chunks.receiveCatching().getOrNull() ?: break
                    pending.setLength(0)
                    pending.append(first)
                    while (true) {
                        val next = withTimeoutOrNull(40) { chunks.receive() } ?: break
                        pending.append(next)
                        if (pending.length >= 8_192) break
                    }
                    tab.buffer.write(pending.toString())
                    if (_activeTab.value == tab.id) publishActive()
                }
            } finally {
                reader.cancel()
                chunks.close()
            }
        }
        viewModelScope.launch {
            session.openShell(termCols, termRows).onFailure { toast("Shell failed: ${describe(it)}") }
        }
        selectTerminalTab(tab.id)
    }

    fun newTerminalTab() {
        if (connectionState.value is ConnectionState.Connected) addTerminalTab()
    }

    fun selectTerminalTab(id: Int) {
        _activeTab.value = id
        publishActive()
    }

    fun closeTerminalTab(id: Int) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        tab.job?.cancel()
        tab.session?.destroy()
        tabs.remove(tab)
        publishTabs()
        val connected = connectionState.value is ConnectionState.Connected
        when {
            tabs.isEmpty() && connected -> addTerminalTab()
            tabs.isEmpty() -> {
                _activeTab.value = 0
                publishActive()
            }
            _activeTab.value == id -> selectTerminalTab(tabs[tabs.size - 1].id)
        }
    }

    fun sendTerminalInput(data: String) {
        val session = tabs.firstOrNull { it.id == _activeTab.value }?.session ?: return
        // Non-blocking enqueue: key events never spawn a new ViewModel coroutine per character.
        session.sendInput(data)
    }

    fun clearTerminal() {
        val tab = tabs.firstOrNull { it.id == _activeTab.value } ?: return
        tab.buffer.clear()
        publishActive()
        sendTerminalInput("\u000c")
    }

    fun resizeTerminal(cols: Int, rows: Int) {
        termCols = cols
        termRows = rows
        for (tab in tabs) {
            val s = tab.session ?: continue
            viewModelScope.launch { s.resize(cols, rows) }
        }
    }

    private fun resetTerminals() {
        for (tab in tabs) {
            tab.job?.cancel()
            tab.session?.destroy()
        }
        tabs.clear()
        nextTabId = 1
        publishTabs()
        _activeTab.value = 0
        publishActive()
    }

    // ---------- server management ----------
    fun selectServer(id: Long) {
        val state = connectionState.value
        if (state is ConnectionState.Connected && state.server.id != id) disconnect()
        selectedId.value = id
    }

    fun addServer(
        name: String,
        hostname: String,
        port: Int,
        username: String,
        authType: AuthType,
        secret: String,
        passphrase: String,
        iconKey: String
    ) {
        viewModelScope.launch {
            try {
                val entity = withContext(Dispatchers.IO) {
                    ServerEntity(
                        name = name.ifBlank { hostname },
                        hostname = hostname,
                        port = port,
                        username = username,
                        authType = authType,
                        encryptedSecret = keystore.encrypt(
                            if (authType == AuthType.PRIVATE_KEY) secret.trim() + "\n" else secret
                        ),
                        encryptedPassphrase = keystore.encrypt(passphrase)
                    )
                }
                val id = dao.insertServer(entity)
                prefs.setServerIcon(id, iconKey)
                selectedId.value = id
            } catch (e: Exception) {
                toast("Could not save server: ${describe(e)}")
            }
        }
    }

    /** Blank [secret] keeps the stored credentials; a changed host/port resets the trusted host key. */
    fun updateServer(
        existing: ServerEntity,
        name: String,
        hostname: String,
        port: Int,
        username: String,
        authType: AuthType,
        secret: String,
        passphrase: String,
        iconKey: String
    ) {
        viewModelScope.launch {
            try {
                val state = connectionState.value
                if (state is ConnectionState.Connected && state.server.id == existing.id) {
                    cleanupSession()
                    ssh.disconnect()
                }
                val updated = withContext(Dispatchers.IO) {
                    val newSecret = when {
                        secret.isBlank() && authType == existing.authType -> existing.encryptedSecret
                        authType == AuthType.PRIVATE_KEY -> keystore.encrypt(secret.trim() + "\n")
                        else -> keystore.encrypt(secret)
                    }
                    val newPass = when {
                        secret.isBlank() && authType == existing.authType -> existing.encryptedPassphrase
                        else -> keystore.encrypt(passphrase)
                    }
                    val hostChanged = hostname != existing.hostname || port != existing.port
                    existing.copy(
                        name = name.ifBlank { hostname },
                        hostname = hostname,
                        port = port,
                        username = username,
                        authType = authType,
                        encryptedSecret = newSecret,
                        encryptedPassphrase = newPass,
                        knownHostFingerprint = if (hostChanged) null else existing.knownHostFingerprint
                    )
                }
                dao.updateServer(updated)
                prefs.setServerIcon(existing.id, iconKey)
                toast("Saved ${updated.name}")
            } catch (e: Exception) {
                toast("Could not update server: ${describe(e)}")
            }
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            val state = connectionState.value
            if (state is ConnectionState.Connected && state.server.id == server.id) {
                cleanupSession()
                ssh.disconnect()
            }
            dao.deleteServer(server)
        }
    }

    fun clearTrustedHostKeys() {
        viewModelScope.launch {
            dao.clearAllHostFingerprints()
            toast("All trusted host keys were removed. You will be asked to verify them again.")
        }
    }

    // ---------- connection ----------
    fun connect() {
        val server = selectedServer.value ?: return
        connectTo(server)
    }

    /** Select [server] and connect (switching away from the current connection if needed). */
    fun connectServer(server: ServerEntity) {
        val state = connectionState.value
        if (state is ConnectionState.Connected && state.server.id == server.id) return
        connectTo(server)
    }

    private fun connectTo(server: ServerEntity) {
        viewModelScope.launch {
            selectedId.value = server.id
            cleanupSession()
            val result = ssh.connect(server)
            if (result.isSuccess) {
                dao.updateLastConnected(server.id, System.currentTimeMillis())
                toast("Connected to ${server.name}")
                onConnected()
            } else {
                val state = ssh.connectionState.value
                if (state is ConnectionState.Error) toast(state.message)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            cleanupSession()
            ssh.disconnect()
        }
    }

    /** User accepted the unknown host key: persist it, then reconnect with strict verification. */
    fun trustHostKey() {
        val state = ssh.connectionState.value as? ConnectionState.HostKeyConfirmationRequired ?: return
        viewModelScope.launch {
            dao.updateHostFingerprint(state.server.id, state.fingerprint)
            connectTo(state.server.copy(knownHostFingerprint = state.fingerprint))
        }
    }

    fun dismissHostKeyPrompt() {
        ssh.resetState()
    }

    private fun onConnected() {
        resetTerminals()
        addTerminalTab()
        sftp = SftpManager(ssh)
        filesLoad(null)
        startMonitor()
    }

    private fun onConnectionLost() {
        cleanupSession()
        ssh.markConnectionLost()
        toast("Connection lost")
    }

    private fun cleanupSession() {
        monitorJob?.cancel()
        monitorJob = null
        for (tab in tabs) {
            tab.job?.cancel()
            if (tab.session != null) tab.buffer.write("\r\n[disconnected]\r\n")
            tab.session?.destroy()
            tab.session = null
        }
        publishActive()
        sftp?.close()
        sftp = null
        _telemetry.value = null
        history.clear()
        _metrics.value = emptyList()
        _files.value = FilesState()
        _openFile.value = null
        _transfer.value = null
        _docker.value = DockerState()
        _dockerDetails.value = null
        _services.value = ServicesState()
        _logs.value = LogsState()
        _webPorts.value = WebPortsState()
        _processes.value = ProcessesState()
        _browserTarget.value = null
    }

    // ---------- telemetry & monitor ----------
    fun refreshTelemetry() {
        if (connectionState.value !is ConnectionState.Connected) return
        viewModelScope.launch { fetchTelemetry() }
    }

    private suspend fun fetchTelemetry() {
        val result = ssh.execute(TELEMETRY_SCRIPT).getOrNull() ?: return
        val t = parseTelemetry(result.stdout) ?: return
        _telemetry.value = t
        val memPct = if (t.memTotalKb > 0) t.memUsedKb * 100f / t.memTotalKb else 0f
        val diskPct = if (t.diskTotalKb > 0) t.diskUsedKb * 100f / t.diskTotalKb else 0f
        history.add(MetricSample(System.currentTimeMillis(), t.cpuPercent, memPct, diskPct, t.rxBytesPerSec, t.txBytesPerSec))
        while (history.size > 900) history.removeAt(0)
        _metrics.value = history.toList()
    }

    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (isActive) {
                if (!ssh.isAlive()) {
                    onConnectionLost()
                    return@launch
                }
                fetchTelemetry()
                delay(prefs.refreshSeconds.value.coerceIn(2, 60) * 1000L)
            }
        }
    }

    // ---------- files (SFTP) ----------
    fun filesLoad(path: String?) {
        val manager = sftp ?: return
        viewModelScope.launch {
            _files.update { it.copy(loading = true, error = null) }
            val target = path ?: manager.homeDir().getOrElse { "/" }
            manager.listFiles(target)
                .onSuccess { _files.value = FilesState(path = target, entries = it) }
                .onFailure { err -> _files.update { it.copy(loading = false, error = describe(err)) } }
        }
    }

    fun filesRefresh() {
        val path = _files.value.path
        filesLoad(path.ifEmpty { null })
    }

    fun filesUp() {
        val path = _files.value.path
        if (path.isEmpty() || path == "/") return
        filesLoad(path.substringBeforeLast('/').ifEmpty { "/" })
    }

    fun filesOpen(item: RemoteFileItem) {
        val manager = sftp ?: return
        if (item.isDirectory) {
            filesLoad(item.path)
            return
        }
        viewModelScope.launch {
            manager.readTextFile(item.path)
                .onSuccess {
                    if (it.indexOf('\u0000') in 0..4096) toast("${item.name} looks like a binary file. Use Download instead.")
                    else _openFile.value = OpenFile(item.path, it)
                }
                .onFailure { toast(describe(it)) }
        }
    }

    fun closeOpenFile() {
        _openFile.value = null
    }

    fun saveOpenFile(content: String) {
        val manager = sftp ?: return
        val file = _openFile.value ?: return
        viewModelScope.launch {
            manager.writeTextFile(file.path, content)
                .onSuccess {
                    toast("Saved ${file.path.substringAfterLast('/')}")
                    _openFile.value = null
                    filesRefresh()
                }
                .onFailure { toast("Save failed: ${describe(it)}") }
        }
    }

    fun filesDelete(item: RemoteFileItem) {
        val manager = sftp ?: return
        viewModelScope.launch {
            manager.deleteFile(item.path, item.isDirectory)
                .onSuccess {
                    toast("Deleted ${item.name}")
                    filesRefresh()
                }
                .onFailure { toast("Delete failed: ${describe(it)}") }
        }
    }

    private fun childPath(name: String): String {
        val base = _files.value.path
        return if (base == "/") "/$name" else "$base/$name"
    }

    fun filesMkdir(name: String) {
        val manager = sftp ?: return
        val clean = name.trim()
        if (clean.isEmpty() || clean.contains('/')) {
            toast("Invalid folder name")
            return
        }
        viewModelScope.launch {
            manager.createDirectory(childPath(clean))
                .onSuccess { filesRefresh() }
                .onFailure { toast("Could not create folder: ${describe(it)}") }
        }
    }

    fun filesCreateFile(name: String) {
        val manager = sftp ?: return
        val clean = name.trim()
        if (clean.isEmpty() || clean.contains('/')) {
            toast("Invalid file name")
            return
        }
        viewModelScope.launch {
            manager.writeTextFile(childPath(clean), "")
                .onSuccess { filesRefresh() }
                .onFailure { toast("Could not create file: ${describe(it)}") }
        }
    }

    private fun displayNameOf(uri: Uri): String? =
        try {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }

    fun filesUpload(uri: Uri) {
        val manager = sftp ?: return
        val name = (displayNameOf(uri) ?: uri.lastPathSegment ?: "upload.bin").substringAfterLast('/')
        val target = childPath(name)
        viewModelScope.launch {
            _transfer.value = "Uploading $name…"
            val input = try {
                app.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (input == null) {
                _transfer.value = null
                toast("Could not read the selected file")
                return@launch
            }
            val result = input.use { manager.upload(target, it) }
            _transfer.value = null
            result.onSuccess {
                toast("Uploaded $name (${it / 1024} KB)")
                filesRefresh()
            }.onFailure { toast("Upload failed: ${describe(it)}") }
        }
    }

    fun filesDownload(item: RemoteFileItem, uri: Uri) {
        val manager = sftp ?: return
        viewModelScope.launch {
            _transfer.value = "Downloading ${item.name}…"
            val out = try {
                app.contentResolver.openOutputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (out == null) {
                _transfer.value = null
                toast("Could not write to the selected location")
                return@launch
            }
            val result = out.use { manager.download(item.path, it) }
            _transfer.value = null
            result.onSuccess { toast("Downloaded ${item.name} (${it / 1024} KB)") }
                .onFailure { toast("Download failed: ${describe(it)}") }
        }
    }

    // ---------- docker ----------
    private suspend fun runDocker(args: String): Result<String> {
        var res = ssh.execute("docker $args").getOrElse { return Result.failure(it) }
        if (!res.ok && res.stderr.contains("permission denied", ignoreCase = true)) {
            res = ssh.execute("sudo -n docker $args").getOrElse { return Result.failure(it) }
        }
        if (res.ok) return Result.success(res.stdout)
        val message = res.stderr.ifBlank { res.stdout }.trim()
            .ifBlank { "docker exited with code ${res.exitCode}" }
        return Result.failure(Exception(message))
    }

    fun refreshDocker() {
        if (connectionState.value !is ConnectionState.Connected) return
        viewModelScope.launch {
            _docker.update { it.copy(loading = true, error = null) }
            runDocker(DOCKER_PS_ARGS)
                .onSuccess { _docker.value = DockerState(containers = parseDockerPs(it)) }
                .onFailure { err -> _docker.value = DockerState(error = describe(err)) }
        }
    }

    fun dockerAction(container: DockerContainer, action: String) {
        if (action !in setOf("start", "stop", "restart")) return
        if (!Regex("[A-Za-z0-9]+").matches(container.id)) return
        viewModelScope.launch {
            runDocker("$action ${container.id}")
                .onSuccess { toast("${container.name}: $action OK") }
                .onFailure { toast("${container.name}: ${describe(it)}") }
            refreshDocker()
        }
    }

    fun openDockerDetails(container: DockerContainer) {
        if (!Regex("[A-Za-z0-9]+").matches(container.id)) return
        _dockerDetails.value = DockerDetails(container)
        viewModelScope.launch {
            val template = "{{.Name}}|{{.Config.Image}}|{{.State.Status}}|{{.Created}}|{{.State.StartedAt}}|" +
                "{{.HostConfig.RestartPolicy.Name}}|{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}"
            val inspect = runDocker("inspect --format '$template' ${container.id}")
            val logs = runDocker("logs --tail 150 ${container.id} 2>&1")
            val ports = runDocker("port ${container.id}")
            val info = ArrayList<Pair<String, String>>()
            inspect.onSuccess { out ->
                val p = out.trim().split('|')
                if (p.size >= 7) {
                    info.add("Name" to p[0].removePrefix("/"))
                    info.add("Image" to p[1])
                    info.add("State" to p[2])
                    info.add("Created" to p[3])
                    info.add("Started" to p[4])
                    info.add("Restart policy" to p[5].ifBlank { "no" })
                    info.add("IP address" to p[6].trim().ifBlank { "—" })
                }
            }
            ports.onSuccess { out ->
                info.add("Ports" to out.trim().ifBlank { "none published" })
            }
            _dockerDetails.value = DockerDetails(
                container = container,
                info = info,
                logs = logs.getOrElse { "" },
                loading = false,
                error = inspect.exceptionOrNull()?.let { describe(it) }
            )
        }
    }

    fun closeDockerDetails() {
        _dockerDetails.value = null
    }

    // ---------- systemd services ----------
    private val unitPattern = Regex("[A-Za-z0-9@:._\\\\-]+")

    fun refreshServices() {
        if (connectionState.value !is ConnectionState.Connected) return
        viewModelScope.launch {
            _services.update { it.copy(loading = true, error = null) }
            val res = ssh.execute(LIST_SERVICES_CMD).getOrElse { err ->
                _services.value = ServicesState(error = describe(err))
                return@launch
            }
            if (res.ok) {
                _services.value = ServicesState(units = parseServices(res.stdout))
            } else {
                val message = res.stderr.ifBlank { res.stdout }.trim()
                    .ifBlank { "systemctl exited with code ${res.exitCode}" }
                _services.value = ServicesState(error = message)
            }
        }
    }

    fun serviceAction(unit: ServiceUnit, action: String) {
        if (action !in setOf("start", "stop", "restart")) return
        if (!unitPattern.matches(unit.name)) return
        // Use sudo (non-interactive) when not logged in as root
        val cmd = "if [ \"\$(id -u)\" -eq 0 ]; then systemctl $action '${unit.name}'; " +
            "else sudo -n systemctl $action '${unit.name}'; fi"
        viewModelScope.launch {
            val res = ssh.execute(cmd).getOrElse { err ->
                toast(describe(err))
                return@launch
            }
            if (res.ok) {
                toast("${unit.name}: $action OK")
            } else {
                toast("${unit.name}: " + res.stderr.ifBlank { res.stdout }.trim().ifBlank { "failed (exit ${res.exitCode})" })
            }
            refreshServices()
        }
    }

    // ---------- logs ----------
    private suspend fun runWithSudoFallback(cmd: String): Result<String> {
        val first = ssh.execute(cmd).getOrElse { return Result.failure(it) }
        if (first.ok && first.stdout.isNotBlank()) return Result.success(first.stdout)
        val quoted = cmd.replace("'", "'\\''")
        val second = ssh.execute("sudo -n sh -c '$quoted'").getOrNull()
        if (second != null && second.ok && second.stdout.isNotBlank()) return Result.success(second.stdout)
        if (first.ok) return Result.success(first.stdout)
        val msg = first.stderr.ifBlank { first.stdout }.trim().ifBlank { "Command failed (exit ${first.exitCode})" }
        return Result.failure(Exception(msg))
    }

    fun loadLogs(source: LogSource) {
        if (connectionState.value !is ConnectionState.Connected) return
        _logs.update { it.copy(source = source, loading = true, error = null) }
        viewModelScope.launch {
            runWithSudoFallback(source.command)
                .onSuccess { out -> _logs.update { it.copy(lines = out.lines(), loading = false) } }
                .onFailure { err -> _logs.update { it.copy(lines = emptyList(), loading = false, error = describe(err)) } }
        }
    }

    fun loadLogFile(path: String) {
        if (connectionState.value !is ConnectionState.Connected) return
        val clean = path.trim()
        if (!Regex("[A-Za-z0-9_./@:+-]+").matches(clean)) {
            _logs.update { it.copy(error = "Enter a plain absolute path such as /var/log/syslog") }
            return
        }
        _logs.update { it.copy(source = null, customPath = clean, loading = true, error = null) }
        viewModelScope.launch {
            runWithSudoFallback("tail -n 400 -- '$clean'")
                .onSuccess { out -> _logs.update { it.copy(lines = out.lines(), loading = false) } }
                .onFailure { err -> _logs.update { it.copy(lines = emptyList(), loading = false, error = describe(err)) } }
        }
    }

    // ---------- processes ----------
    fun refreshProcesses() {
        if (connectionState.value !is ConnectionState.Connected) return
        viewModelScope.launch {
            if (_processes.value.items.isEmpty()) _processes.update { it.copy(loading = true, error = null) }
            val res = ssh.execute(PS_CMD).getOrElse { err ->
                _processes.value = ProcessesState(error = describe(err))
                return@launch
            }
            val items = parseProcesses(res.stdout)
            _processes.value = if (items.isEmpty()) {
                ProcessesState(error = res.stderr.ifBlank { "Could not read the process list." }.trim())
            } else {
                ProcessesState(items = items)
            }
        }
    }

    fun killProcess(process: ProcessInfo, signal: String) {
        if (signal !in setOf("TERM", "KILL") || process.pid <= 1) return
        viewModelScope.launch {
            val cmd = "kill -$signal ${process.pid} 2>&1 || sudo -n kill -$signal ${process.pid} 2>&1"
            val res = ssh.execute(cmd).getOrElse { err ->
                toast(describe(err))
                return@launch
            }
            if (res.ok) toast("Sent $signal to ${process.command} (${process.pid})")
            else toast("Could not stop ${process.command}: " + res.stdout.ifBlank { res.stderr }.trim())
            refreshProcesses()
        }
    }

    // ---------- browser (web apps on the server through an SSH tunnel) ----------
    fun refreshWebPorts() {
        if (connectionState.value !is ConnectionState.Connected) return
        _webPorts.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val res = ssh.execute(LIST_PORTS_CMD).getOrElse { err ->
                _webPorts.value = WebPortsState(error = describe(err))
                return@launch
            }
            val ports = parseListeningPorts(res.stdout)
            _webPorts.value = if (ports.isEmpty()) {
                WebPortsState(error = "No listening ports found (the server needs the ss or netstat command).")
            } else {
                WebPortsState(ports = ports)
            }
        }
    }

    fun openWebPort(port: Int, https: Boolean, path: String = "/") {
        if (connectionState.value !is ConnectionState.Connected) {
            toast("Connect to a server first")
            return
        }
        viewModelScope.launch {
            ssh.openLocalForward(port)
                .onSuccess { local ->
                    val scheme = if (https) "https" else "http"
                    _browserTarget.value = BrowserTarget("Port $port", "$scheme://127.0.0.1:$local$path", port)
                }
                .onFailure { toast("Tunnel to port $port failed: ${describe(it)}") }
        }
    }

    /** Accepts "8080", "localhost:3000/app", "https://127.0.0.1:9443" (server apps) or any normal web address. */
    fun openBrowserInput(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        val portOnly = input.toIntOrNull()
        if (portOnly != null) {
            if (portOnly in 1..65535) openWebPort(portOnly, likelyHttps(portOnly)) else toast("Port must be between 1 and 65535")
            return
        }
        val withScheme = if (input.startsWith("http://") || input.startsWith("https://")) input else "http://$input"
        val uri = try {
            java.net.URI(withScheme)
        } catch (e: Exception) {
            toast("That address is not valid")
            return
        }
        val host = uri.host
        if (host == null) {
            toast("That address is not valid")
            return
        }
        val server = (connectionState.value as? ConnectionState.Connected)?.server
        val isServerLocal = host == "localhost" || host == "127.0.0.1" ||
            (server != null && host.equals(server.hostname, ignoreCase = true))
        val https = withScheme.startsWith("https://")
        if (isServerLocal) {
            val port = if (uri.port != -1) uri.port else if (https) 443 else 80
            val path = (uri.rawPath ?: "").ifEmpty { "/" } + (uri.rawQuery?.let { "?$it" } ?: "")
            openWebPort(port, https, path)
        } else {
            val direct = if (input.startsWith("http://") || input.startsWith("https://")) input else "https://$input"
            _browserTarget.value = BrowserTarget(host, direct, null)
        }
    }

    fun closeBrowserTarget() {
        _browserTarget.value = null
    }

    override fun onCleared() {
        for (tab in tabs) tab.session?.destroy()
        sftp?.close()
        super.onCleared()
    }
}
