package com.nexus.ssh

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class NexusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Register full BouncyCastle provider for ED25519 & modern SSH ciphers
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}