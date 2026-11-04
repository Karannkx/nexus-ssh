package com.nexus.ssh.core.ssh

import com.nexus.ssh.core.database.AuthType
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.PublicKey

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Connecting : ConnectionState
    data class HostKeyConfirmationRequired(
        val server: ServerEntity,
        val hostname: String,
        val port: Int,
        val keyAlgorithm: String,
        val fingerprint: String
    ) : ConnectionState
    data class Connected(val server: ServerEntity) : ConnectionState
    data class Error(val message: String, val cause: Throwable? = null) : ConnectionState
}

class SshConnectionManager(
    private val keystoreManager: KeystoreManager
) {
    private var sshClient: SSHClient? = null
    private var currentServer: ServerEntity? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    suspend fun connect(server: ServerEntity, bypassHostCheck: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting
            disconnectInternal()

            val client = SSHClient().apply {
                timeout = 15000
                connectTimeout = 10000
            }

            var pendingFingerprint: String? = null
            var pendingKeyAlgo: String = ""

            // Host key verification logic
            client.addHostKeyVerifier(object : HostKeyVerifier {
                override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                    val fingerprint = SecurityUtils.getFingerprint(key)
                    pendingFingerprint = fingerprint
                    pendingKeyAlgo = key.algorithm

                    if (bypassHostCheck) return true

                    // Compare against saved fingerprint if available
                    if (server.knownHostFingerprint != null) {
                        return server.knownHostFingerprint == fingerprint
                    }
                    // Requires user manual confirmation
                    return false
                }

                override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> {
                    return mutableListOf()
                }
            })

            try {
                client.connect(server.hostname, server.port)
            } catch (e: Exception) {
                if (pendingFingerprint != null && server.knownHostFingerprint == null && !bypassHostCheck) {
                    _connectionState.value = ConnectionState.HostKeyConfirmationRequired(
                        server = server,
                        hostname = server.hostname,
                        port = server.port,
                        keyAlgorithm = pendingKeyAlgo,
                        fingerprint = pendingFingerprint!!
                    )
                    return@withContext Result.failure(SecurityException("Host key requires user verification"))
                }
                throw e
            }

            // Authenticate
            val decryptedSecret = try {
                keystoreManager.decrypt(server.encryptedSecret)
            } catch (_: Exception) {
                server.encryptedSecret
            }

            when (server.authType) {
                AuthType.PASSWORD -> {
                    client.authPassword(server.username, decryptedSecret)
                }
                AuthType.PRIVATE_KEY -> {
                    val passphrase = if (server.encryptedPassphrase.isNotEmpty()) {
                        keystoreManager.decrypt(server.encryptedPassphrase)
                    } else ""

                    val keyProvider: KeyProvider = if (passphrase.isNotEmpty()) {
                        client.loadKeys(decryptedSecret, passphrase)
                    } else {
                        client.loadKeys(decryptedSecret, null as String?)
                    }
                    client.authPublickey(server.username, keyProvider)
                }
            }

            if (!client.isAuthenticated) {
                throw SecurityException("Authentication failed for user ${server.username}")
            }

            sshClient = client
            currentServer = server
            _connectionState.value = ConnectionState.Connected(server)
            Result.success(Unit)
        } catch (e: Exception) {
            disconnectInternal()
            if (_connectionState.value !is ConnectionState.HostKeyConfirmationRequired) {
                _connectionState.value = ConnectionState.Error(e.localizedMessage ?: "Connection error", e)
            }
            Result.failure(e)
        }
    }

    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        val client = sshClient ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        try {
            client.startSession().use { session ->
                val cmd = session.exec(command)
                val output = cmd.inputStream.reader().readText()
                cmd.join(10, java.util.concurrent.TimeUnit.SECONDS)
                Result.success(output.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getClient(): SSHClient? = sshClient
    fun getCurrentServer(): ServerEntity? = currentServer

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        try {
            sshClient?.disconnect()
        } catch (_: Exception) {}
        sshClient = null
        currentServer = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
