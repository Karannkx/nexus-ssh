package com.nexus.ssh.core.sftp

import com.nexus.ssh.core.ssh.SshConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

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
    companion object {
        const val MAX_EDIT_BYTES = 1024 * 1024L
    }

    private var sftpClient: SFTPClient? = null

    private fun sftp(): SFTPClient {
        sftpClient?.let { return it }
        val client = connectionManager.getClient()
            ?: throw IllegalStateException("SSH not connected")
        return client.newSFTPClient().also { sftpClient = it }
    }

    private fun permissionString(mask: Int, isDir: Boolean): String {
        val sb = StringBuilder(if (isDir) "d" else "-")
        val bits = "rwxrwxrwx"
        for (i in 0 until 9) {
            sb.append(if (mask and (1 shl (8 - i)) != 0) bits[i] else '-')
        }
        return sb.toString()
    }

    suspend fun homeDir(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(sftp().canonicalize("."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFiles(remoteDir: String): Result<List<RemoteFileItem>> = withContext(Dispatchers.IO) {
        try {
            val items = sftp().ls(remoteDir)
                .filter { it.name != "." && it.name != ".." }
                .map { res ->
                    RemoteFileItem(
                        name = res.name,
                        path = res.path,
                        isDirectory = res.isDirectory,
                        size = res.attributes.size,
                        permissions = permissionString(res.attributes.mode.mask, res.isDirectory),
                        mtime = res.attributes.mtime * 1000L
                    )
                }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readTextFile(remotePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val text = sftp().open(remotePath).use { file ->
                val length = file.length()
                if (length > MAX_EDIT_BYTES) {
                    throw IOException("File is too large to edit here (${length / 1024} KB, limit ${MAX_EDIT_BYTES / 1024} KB)")
                }
                val bytes = file.RemoteFileInputStream().use { it.readBytes() }
                String(bytes, Charsets.UTF_8)
            }
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeTextFile(remotePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modes = setOf(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
            sftp().open(remotePath, modes).use { file ->
                file.RemoteFileOutputStream().use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Streams [input] to [remotePath]; returns number of bytes sent. */
    suspend fun upload(remotePath: String, input: InputStream): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val modes = setOf(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
            var total = 0L
            sftp().open(remotePath, modes).use { file ->
                file.RemoteFileOutputStream(0L, 16).use { out ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        total += n
                    }
                    out.flush()
                }
            }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Streams [remotePath] into [output]; returns number of bytes received. */
    suspend fun download(remotePath: String, output: OutputStream): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var total = 0L
            sftp().open(remotePath).use { file ->
                file.RemoteFileInputStream().use { input ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        total += n
                    }
                }
            }
            output.flush()
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(remotePath: String, isDir: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            deleteRecursive(sftp(), remotePath, isDir)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deleteRecursive(client: SFTPClient, path: String, isDir: Boolean) {
        if (isDir) {
            for (child in client.ls(path)) {
                if (child.name == "." || child.name == "..") continue
                deleteRecursive(client, child.path, child.isDirectory)
            }
            client.rmdir(path)
        } else {
            client.rm(path)
        }
    }

    suspend fun createDirectory(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sftp().mkdir(remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        try {
            sftpClient?.close()
        } catch (ignored: Exception) {
        }
        sftpClient = null
    }
}
