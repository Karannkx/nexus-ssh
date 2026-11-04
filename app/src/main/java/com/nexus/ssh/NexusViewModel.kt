package com.nexus.ssh

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.ssh.core.database.AuthType
import com.nexus.ssh.core.database.NexusDatabase
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.security.KeystoreManager
import com.nexus.ssh.core.sftp.RemoteFileItem
import com.nexus.ssh.core.sftp.SftpManager
import com.nexus.ssh.core.ssh.ConnectionState
import com.nexus.ssh.core.ssh.DOCKER_PS_ARGS
import com.nexus.ssh.core.ssh.DockerContainer
import com.nexus.ssh.core.ssh.LIST_SERVICES_CMD
import com.nexus.ssh.core.ssh.ServiceUnit
import com.nexus.ssh.core.ssh.SshConnectionManager
import com.nexus.ssh.core.ssh.SshTerminalSession
import com.nexus.ssh.core.ssh.TELEMETRY_SCRIPT
import com.nexus.ssh.core.ssh.Telemetry
import com.nexus.ssh.core.ssh.TerminalBuffer
import com.nexus.ssh.core.ssh.parseDockerPs
import com.nexus.ssh.core.ssh.parseServices
import com.nexus.ssh.core.ssh.parseTelemetry
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
import kotlinx.coroutines.Dispatchers

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

data class ServicesState(
    val units: List<ServiceUnit> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class NexusViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = NexusDatabase.get(app).serverDao()
    private val keystore = KeystoreManager()
    private val ssh = SshConnectionManager(keystore)

    private var sftp: SftpManager? = null
    private var terminal: SshTerminalSession? = null
    private var terminalJob: Job? = null
    private var monitorJob: Job? = null
    private val termBuffer = TerminalBuffer()
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

    private val _terminalLines = MutableStateFlow<List<String>>(emptyList())
    val terminalLines: StateFlow<List<String>> = _terminalLines

    private val _files = MutableStateFlow(FilesState())
    val files: StateFlow<FilesState> = _files

    private val _openFile = MutableStateFlow<OpenFile?>(null)
    val openFile: StateFlow<OpenFile?> = _openFile

    private val _docker = MutableStateFlow(DockerState())
    val docker: StateFlow<DockerState> = _docker

    private val _services = MutableStateFlow(ServicesState())
    val services: StateFlow<ServicesState> = _services

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val dashboardVisible = MutableStateFlow(false)

    private fun toast(text: String) {
        _messages.trySend(text)
    }

    private fun describe(e: Throwable): String = e.localizedMessage ?: e.javaClass.simpleName

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
        passphrase: String
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
                selectedId.value = id
            } catch (e: Exception) {
                toast("Could not save server: ${describe(e)}")
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

    // ---------- connection ----------
    fun connect() {
        val server = selectedServer.value ?: return
        connectTo(server)
    }

    private fun connectTo(server: ServerEntity) {
        viewModelScope.launch {
            selectedId.value = server.id
            cleanupSession()
            val result = ssh.connect(server)
            if (result.isSuccess) {
                dao.updateLastConnected(server.id, System.currentTimeMillis())
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
        openTerminal()
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
        terminalJob?.cancel()
        terminalJob = null
        if (terminal != null) termBuffer.write("\r\n[disconnected]\r\n")
        _terminalLines.value = termBuffer.snapshot().let { if (terminal != null) it else emptyList() }
        terminal?.destroy()
        terminal = null
        sftp?.close()
        sftp = null
        _telemetry.value = null
        _files.value = FilesState()
        _openFile.value = null
        _docker.value = DockerState()
        _services.value = ServicesState()
    }

    // ---------- telemetry ----------
    fun setDashboardVisible(visible: Boolean) {
        dashboardVisible.value = visible
        if (visible) refreshTelemetry()
    }

    fun refreshTelemetry() {
        if (connectionState.value !is ConnectionState.Connected) return
        viewModelScope.launch { fetchTelemetry() }
    }

    private suspend fun fetchTelemetry() {
        val result = ssh.execute(TELEMETRY_SCRIPT).getOrNull() ?: return
        parseTelemetry(result.stdout)?.let { _telemetry.value = it }
    }

    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (isActive) {
                if (!ssh.isAlive()) {
                    onConnectionLost()
                    return@launch
                }
                if (dashboardVisible.value || _telemetry.value == null) fetchTelemetry()
                delay(5_000)
            }
        }
    }

    // ---------- terminal ----------
    private fun openTerminal() {
        termBuffer.clear()
        _terminalLines.value = emptyList()
        val session = SshTerminalSession(ssh)
        terminal = session
        terminalJob = viewModelScope.launch {
            session.terminalOutput.collect { chunk ->
                termBuffer.write(chunk)
                _terminalLines.value = termBuffer.snapshot()
            }
        }
        viewModelScope.launch {
            session.openShell(termCols, termRows).onFailure { toast("Shell failed: ${describe(it)}") }
        }
    }

    fun sendTerminalInput(data: String) {
        val session = terminal ?: return
        viewModelScope.launch { session.sendInput(data) }
    }

    fun clearTerminal() {
        termBuffer.clear()
        _terminalLines.value = emptyList()
        sendTerminalInput("\u000c")
    }

    fun resizeTerminal(cols: Int, rows: Int) {
        termCols = cols
        termRows = rows
        val session = terminal ?: return
        viewModelScope.launch { session.resize(cols, rows) }
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
                .onSuccess { _openFile.value = OpenFile(item.path, it) }
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
                .onSuccess { filesRefresh() }
                .onFailure { toast("Delete failed: ${describe(it)}") }
        }
    }

    fun filesMkdir(name: String) {
        val manager = sftp ?: return
        val clean = name.trim()
        if (clean.isEmpty() || clean.contains('/')) {
            toast("Invalid folder name")
            return
        }
        val base = _files.value.path
        val full = if (base == "/") "/$clean" else "$base/$clean"
        viewModelScope.launch {
            manager.createDirectory(full)
                .onSuccess { filesRefresh() }
                .onFailure { toast("Could not create folder: ${describe(it)}") }
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

    override fun onCleared() {
        terminal?.destroy()
        sftp?.close()
        super.onCleared()
    }
}
