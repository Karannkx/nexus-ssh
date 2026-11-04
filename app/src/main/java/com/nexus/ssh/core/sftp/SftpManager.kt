package com.nexus.ssh.core.sftp

import com.nexus.ssh.core.ssh.SshConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

data class RemoteFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,
    val mtime: Long
)

class SftpManager(
    private val connectionManager: SshConnectionManager
) {
    private var sftpClient: SFTPClient? = null

    private fun getOrCreateSftp(): SFTPClient {
        val client = connectionManager.getClient()
            ?: throw IllegalStateException("SSH not connected")
        if (sftpClient == null) {
            sftpClient = client.newSFTPClient()
        }
        return sftpClient!!
    }

    suspend fun listFiles(remoteDir: String): Result<List<RemoteFileItem>> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateSftp()
            val list = sftp.ls(remoteDir)
            val items = list.map { res: RemoteResourceInfo ->
                RemoteFileItem(
                    name = res.name,
                    path = res.path,
                    isDirectory = res.isDirectory,
                    size = res.attributes.size,
                    permissions = res.attributes.mode.toString(),
                    mtime = res.attributes.mtime * 1000L
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readTextFile(remotePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateSftp()
            val file = sftp.open(remotePath)
            val bytes = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var read: Int
            val input = file.RemoteFileInputStream()
            while (input.read(buf).also { read = it } != -1) {
                bytes.write(buf, 0, read)
            }
            input.close()
            file.close()
            Result.success(bytes.toString(Charsets.UTF_8.name()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeTextFile(remotePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateSftp()
            val file = sftp.open(remotePath, setOf(net.schmizz.sshj.sftp.OpenMode.CREAT, net.schmizz.sshj.sftp.OpenMode.WRITE, net.schmizz.sshj.sftp.OpenMode.TRUNC))
            val output = file.RemoteFileOutputStream()
            output.write(content.toByteArray(Charsets.UTF_8))
            output.flush()
            output.close()
            file.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(remotePath: String, isDir: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateSftp()
            if (isDir) {
                sftp.rmdir(remotePath)
            } else {
                sftp.rm(remotePath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = getOrCreateSftp()
            sftp.mkdir(remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        try {
            sftpClient?.close()
        } catch (_: Exception) {}
        sftpClient = null
    }
}
