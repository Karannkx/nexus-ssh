package com.nexus.ssh.core.ssh

import android.util.Base64
import com.nexus.ssh.core.database.AuthType
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Buffer
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.PasswordUtils
import java.security.MessageDigest
import java.security.PublicKey
import java.util.concurrent.TimeUnit

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Connecting : ConnectionState
    data class HostKeyConfirmationRequired(
        val server: ServerEntity,
        val keyAlgorithm: String,
        val fingerprint: String
    ) : ConnectionState
    data class Connected(val server: ServerEntity) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

data class CommandResult(val stdout: String, val stderr: String, val exitCode: Int?) {
    val ok: Boolean get() = exitCode == 0
}

class HostKeyUnverifiedException : Exception("Host key requires user verification")

/** OpenSSH-style SHA256 fingerprint of a host key, e.g. SHA256:AbC... */
fun sha256Fingerprint(key: PublicKey): String {
    val blob = Buffer.PlainBuffer().putPublicKey(key).compactData
    val digest = MessageDigest.getInstance("SHA-256").digest(blob)
    return "SHA256:" + Base64.encodeToString(digest, Base64.NO_PADDING or Base64.NO_WRAP)
}

class SshConnectionManager(
    private val keystoreManager: KeystoreManager
) {
    @Volatile
    private var sshClient: SSHClient? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    suspend fun connect(server: ServerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        disconnectInternal()
        _connectionState.value = ConnectionState.Connecting

        val client = SSHClient()
        var seenFingerprint: String? = null
        var seenAlgorithm = ""
        var mismatch = false

        client.addHostKeyVerifier(object : HostKeyVerifier {
            override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                val fingerprint = sha256Fingerprint(key)
                seenFingerprint = fingerprint
                seenAlgorithm = try {
                    net.schmizz.sshj.common.KeyType.fromKey(key).toString()
                } catch (e: Exception) {
                    key.algorithm
                }
                val known = server.knownHostFingerprint ?: return false // first contact: ask the user
                if (known == fingerprint) return true
                mismatch = true
                return false
            }

            override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> =
                mutableListOf()
        })

        try {
            client.connectTimeout = 10_000
            client.timeout = 20_000
            client.connect(server.hostname, server.port)
            client.connection.keepAlive.keepAliveInterval = 30
            authenticate(client, server)

            sshClient = client
            _connectionState.value = ConnectionState.Connected(server)
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                client.disconnect()
            } catch (ignored: Exception) {
            }
            val fingerprint = seenFingerprint
            if (fingerprint != null && server.knownHostFingerprint == null) {
                _connectionState.value = ConnectionState.HostKeyConfirmationRequired(
                    server = server,
                    keyAlgorithm = seenAlgorithm,
                    fingerprint = fingerprint
                )
                Result.failure(HostKeyUnverifiedException())
            } else {
                val message = if (mismatch) {
                    "HOST KEY CHANGED for ${server.hostname}. Someone may be intercepting the connection, " +
                        "or the server was reinstalled. Delete and re-add the server profile to trust the new key."
                } else {
                    e.localizedMessage ?: e.javaClass.simpleName
                }
                _connectionState.value = ConnectionState.Error(message)
                Result.failure(e)
            }
        }
    }

    private fun authenticate(client: SSHClient, server: ServerEntity) {
        val secret = keystoreManager.decrypt(server.encryptedSecret)
        when (server.authType) {
            AuthType.PASSWORD -> client.authPassword(server.username, secret)
            AuthType.PRIVATE_KEY -> {
                val passphrase = keystoreManager.decrypt(server.encryptedPassphrase)
                val finder: PasswordFinder? =
                    if (passphrase.isNotEmpty()) PasswordUtils.createOneOff(passphrase.toCharArray()) else null
                // loadKeys(privateKeyText, publicKeyText, finder) parses the key from memory (not a file path)
                val keyProvider: KeyProvider = client.loadKeys(secret, null as String?, finder)
                client.authPublickey(server.username, keyProvider)
            }
        }
    }

    /** Runs a command on a fresh exec channel and returns stdout, stderr and exit code. */
    suspend fun execute(command: String, timeoutSeconds: Long = 20): Result<CommandResult> =
        withContext(Dispatchers.IO) {
            val client = sshClient
                ?: return@withContext Result.failure(IllegalStateException("Not connected"))
            try {
                client.startSession().use { session ->
                    val cmd = session.exec(command)
                    val partial = coroutineScope {
                        val err = async { cmd.errorStream.reader().readText() }
                        val out = cmd.inputStream.reader().readText()
                        CommandResult(out, err.await(), null)
                    }
                    cmd.join(timeoutSeconds, TimeUnit.SECONDS)
                    Result.success(partial.copy(exitCode = cmd.exitStatus))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun getClient(): SSHClient? = sshClient

    fun isAlive(): Boolean {
        val c = sshClient ?: return false
        return c.isConnected && c.isAuthenticated
    }

    fun markConnectionLost() {
        disconnectInternal(resetState = false)
        _connectionState.value = ConnectionState.Error("Connection lost")
    }

    fun resetState() {
        if (_connectionState.value !is ConnectionState.Connected) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }

    private fun disconnectInternal(resetState: Boolean = true) {
        try {
            sshClient?.disconnect()
        } catch (ignored: Exception) {
        }
        sshClient = null
        if (resetState) _connectionState.value = ConnectionState.Disconnected
    }
}
