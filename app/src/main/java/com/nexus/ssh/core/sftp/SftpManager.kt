package com.nexus.ssh.core.sftp

import com.nexus.ssh.core.ssh.SshConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.sftp.SFTPClient
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SftpManager @Inject constructor(
    private val connectionManager: SshConnectionManager
) {
    private var sftpClient: SFTPClient? = null

    private fun getSftp(): SFTPClient {
        val client = connectionManager.getClient() ?: error("SSH not connected")
        if (sftpClient == null) sftpClient = client.newSFTPClient()
        return sftpClient!!
    }

    suspend fun listFiles(path: String) = withContext(Dispatchers.IO) {
        getSftp().ls(path)
    }

    suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        val file = getSftp().open(path)
        val stream = file.RemoteFileInputStream()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var read: Int
        while (stream.read(buf).also { read = it } != -1) {
            out.write(buf, 0, read)
        }
        stream.close()
        file.close()
        out.toString(Charsets.UTF_8.name())
    }
}