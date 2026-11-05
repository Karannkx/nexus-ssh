package com.nexus.ssh.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuthType {
    PASSWORD,
    PRIVATE_KEY
}

enum class KeyType {
    ED25519,
    RSA,
    AUTO
}

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType,
    // Encrypted via KeystoreManager
    val encryptedSecret: String,
    val encryptedPassphrase: String = "",
    val keyType: KeyType = KeyType.AUTO,
    // SSH Host Key fingerprint (e.g. SHA256:abc...)
    val knownHostFingerprint: String? = null,
    val lastConnectedAt: Long? = null,
    val colorHex: String = "#0EA5E9"
)
