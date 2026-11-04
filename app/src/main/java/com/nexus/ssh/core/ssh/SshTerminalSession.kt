package com.nexus.ssh.core.ssh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schmizz.sshj.connection.channel.direct.PTYMode
import net.schmizz.sshj.connection.channel.direct.Session
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

/** Interactive shell over a real PTY channel. */
class SshTerminalSession(
    private val connectionManager: SshConnectionManager
) {
    private var session: Session? = null
    private var shell: Session.Shell? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readJob: Job? = null

    private val _terminalOutput = MutableSharedFlow<String>(replay = 128, extraBufferCapacity = 1024)
    val terminalOutput: SharedFlow<String> = _terminalOutput.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun openShell(cols: Int, rows: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val client = connectionManager.getClient()
            ?: return@withContext Result.failure(IllegalStateException("No active SSH connection"))
        try {
            close()
            val sess = client.startSession()
            sess.allocatePTY("xterm-256color", cols, rows, 0, 0, emptyMap<PTYMode, Int>())
            val sh = sess.startShell()
            session = sess
            shell = sh
            outputStream = sh.outputStream
            inputStream = sh.inputStream
            startReading(sh.inputStream)
            Result.success(Unit)
        } catch (e: Exception) {
            close()
            Result.failure(e)
        }
    }

    private fun startReading(stream: InputStream) {
        readJob?.cancel()
        readJob = scope.launch {
            // A Reader decodes UTF-8 correctly even when a multi-byte char is split across reads
            val reader = InputStreamReader(stream, Charsets.UTF_8)
            val buffer = CharArray(4096)
            try {
                while (isActive) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    _terminalOutput.emit(String(buffer, 0, read))
                }
                if (isActive) _terminalOutput.emit("\r\n[session ended]\r\n")
            } catch (e: Exception) {
                if (isActive) _terminalOutput.emit("\r\n[session closed: ${e.message}]\r\n")
            }
        }
    }

    suspend fun sendInput(data: String) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(data.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            _terminalOutput.emit("\r\n[write failed: ${e.message}]\r\n")
        }
    }

    suspend fun resize(cols: Int, rows: Int) = withContext(Dispatchers.IO) {
        try {
            shell?.changeWindowDimensions(cols, rows, 0, 0)
        } catch (ignored: Exception) {
        }
    }

    fun close() {
        readJob?.cancel()
        readJob = null
        try {
            outputStream?.close()
            inputStream?.close()
            shell?.close()
            session?.close()
        } catch (ignored: Exception) {
        }
        shell = null
        session = null
        outputStream = null
        inputStream = null
    }

    fun destroy() {
        close()
        scope.cancel()
    }
}
