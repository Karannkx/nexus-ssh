package com.nexus.ssh.core.ssh

import com.nexus.ssh.core.database.AuthType
import com.nexus.ssh.core.database.ServerEntity
import com.nexus.ssh.core.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshConnectionManager @Inject constructor(
    private val keystoreManager: KeystoreManager
) {
    private var sshClient: SSHClient? = null

    suspend fun connect(server: ServerEntity, bypassHostCheck: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = SSHClient().apply {
                timeout = 15000
                connectTimeout = 10000
            }

            var serverFingerprint: String? = null
            client.addHostKeyVerifier(object : HostKeyVerifier {
                override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                    val fp = SecurityUtils.getFingerprint(key)
                    serverFingerprint = fp
                    if (bypassHostCheck) return true
                    return server.knownHostFingerprint == fp
                }
                override fun findExistingAlgorithms(hostname: String, port: Int) = mutableListOf<String>()
            })

            client.connect(server.hostname, server.port)
            val secret = keystoreManager.decrypt(server.encryptedSecret)

            if (server.authType == AuthType.PASSWORD) {
                client.authPassword(server.username, secret)
            } else {
                val kp = client.loadKeys(secret, null, null)
                client.authPublickey(server.username, kp)
            }

            sshClient = client
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getClient() = sshClient
}