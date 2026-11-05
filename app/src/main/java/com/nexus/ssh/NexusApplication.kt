package com.nexus.ssh

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

/**
 * Nexus SSH Application entry point.
 * Configures BouncyCastle cryptography provider for modern SSH algorithms (Ed25519, Curve25519).
 */
class NexusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupSecurityProviders()
    }

    private fun setupSecurityProviders() {
        // Remove standard restricted BC if present, insert full BouncyCastle provider
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}
