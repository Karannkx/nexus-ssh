package com.nexus.ssh.core.ssh

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import java.io.InputStream
import java.io.OutputStream

class SshTerminalSession(
    private val connectionManager: SshConnectionManager
) {
    private var session: Session? = null
    private var shell: Session.Shell? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null

    private val _terminalOutput = MutableSharedFlow<String>(replay = 500)
    val terminalOutput: SharedFlow<String> = _terminalOutput.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun openShell(cols: Int = 80, rows: Int = 24): Result<Unit> = withContext(Dispatchers.IO) {
        val client = connectionManager.getClient()
            ?: return@withContext Result.failure(IllegalStateException("No active SSH connection"))

        try {
            close()
            val sess = client.startSession()
            sess.allocatePTY("xterm-256color", cols, rows, 0, 0, emptyMap<net.schmizz.sshj.connection.channel.direct.PTYMode, Int>())
            val sh = sess.startShell()

            session = sess
            shell = sh
            inputStream = sh.inputStream
            outputStream = sh.outputStream

            startReading()
            Result.success(Unit)
        } catch (e: Exception) {
            close()
            Result.failure(e)
        }
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(4096)
            val stream = inputStream ?: return@launch
            try {
                while (isActive) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    val chunk = String(buffer, 0, read, Charsets.UTF_8)
                    _terminalOutput.emit(chunk)
                }
            } catch (e: Exception) {
                if (isActive) {
                    _terminalOutput.emit("\r\n[Session closed: ${e.message}]\r\n")
                }
            }
        }
    }

    suspend fun sendInput(data: String) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(data.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            // handle error
        }
    }

    suspend fun sendBytes(bytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(bytes)
            outputStream?.flush()
        } catch (e: Exception) {
            // handle error
        }
    }

    suspend fun resize(cols: Int, rows: Int) = withContext(Dispatchers.IO) {
        try {
            shell?.changeWindowDimensions(cols, rows, 0, 0)
        } catch (_: Exception) {}
    }

    fun close() {
        readJob?.cancel()
        readJob = null
        try {
            outputStream?.close()
            inputStream?.close()
            shell?.close()
            session?.close()
        } catch (_: Exception) {}
        shell = null
        session = null
    }
}
