package com.nexus.ssh.core.ssh

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import javax.net.SocketFactory

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
    private val keystoreManager: KeystoreManager,
    private val context: Context
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
            // For LAN / VPN addresses, send traffic over the Wi-Fi / VPN network instead of the
            // phone's default (often mobile-data) network, which can't reach private IPs.
            localNetworkSocketFactory(server.hostname)?.let { client.socketFactory = it }
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
                    friendlyError(e, server)
                }
                _connectionState.value = ConnectionState.Error(message)
                Result.failure(e)
            }
        }
    }

    private fun isLocalAddress(host: String): Boolean {
        val h = host.trim().lowercase()
        if (h.endsWith(".local") || h.endsWith(".lan")) return true
        val parts = h.split('.')
        if (parts.size != 4) return false
        val a = parts.map { it.toIntOrNull() ?: return false }
        return a[0] == 10 ||
            (a[0] == 172 && a[1] in 16..31) ||
            (a[0] == 192 && a[1] == 168) ||
            (a[0] == 169 && a[1] == 254) ||
            (a[0] == 100 && a[1] in 64..127)
    }

    private fun localNetworkSocketFactory(host: String): SocketFactory? {
        if (!isLocalAddress(host)) return null
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nets = cm.allNetworks.mapNotNull { n -> cm.getNetworkCapabilities(n)?.let { n to it } }
            val vpnFirst = host.startsWith("100.")
            val order = if (vpnFirst) {
                listOf(NetworkCapabilities.TRANSPORT_VPN, NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_ETHERNET)
            } else {
                listOf(NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_ETHERNET, NetworkCapabilities.TRANSPORT_VPN)
            }
            for (transport in order) {
                val match = nets.firstOrNull { it.second.hasTransport(transport) }
                if (match != null) return match.first.socketFactory
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun friendlyError(e: Exception, server: ServerEntity): String {
        val raw = e.localizedMessage ?: e.javaClass.simpleName
        val unreachable = e is java.net.ConnectException ||
            e is java.net.SocketTimeoutException ||
            e is java.net.NoRouteToHostException ||
            raw.contains("failed to connect", ignoreCase = true) ||
            raw.contains("timed out", ignoreCase = true)
        return when {
            e is java.net.UnknownHostException ->
                "Hostname \"${server.hostname}\" could not be resolved. Check the spelling and your internet connection."
            unreachable ->
                "Could not reach ${server.hostname}:${server.port}. Make sure your phone can reach the server " +
                    "(same Wi-Fi for 192.168.x.x / 10.x.x.x addresses, or use a VPN like Tailscale), " +
                    "that SSH is running, and that port ${server.port} is not blocked by a firewall.\n\n($raw)"
            else -> raw
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
