package com.nexus.ssh.core.ssh

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.schmizz.sshj.connection.channel.direct.Session
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshTerminalSession @Inject constructor(
    private val connectionManager: SshConnectionManager
) {
    private var session: Session? = null
    private var shell: Session.Shell? = null
    private var outputStream: OutputStream? = null

    private val _terminalOutput = MutableSharedFlow<String>(replay = 500)
    val terminalOutput = _terminalOutput.asSharedFlow()

    suspend fun openShell(cols: Int = 80, rows: Int = 24) = withContext(Dispatchers.IO) {
        val client = connectionManager.getClient() ?: error("Not connected")
        val sess = client.startSession()
        sess.allocatePTY("xterm-256color", cols, rows, 0, 0, emptyMap())
        val sh = sess.startShell()
        session = sess
        shell = sh
        outputStream = sh.outputStream

        CoroutineScope(Dispatchers.IO).launch {
            val buf = ByteArray(4096)
            val stream = sh.inputStream
            while (isActive) {
                val read = stream.read(buf)
                if (read == -1) break
                _terminalOutput.emit(String(buf, 0, read, Charsets.UTF_8))
            }
        }
    }

    suspend fun sendInput(data: String) = withContext(Dispatchers.IO) {
        outputStream?.write(data.toByteArray(Charsets.UTF_8))
        outputStream?.flush()
    }
}