package com.nexus.ssh.core.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteParsersTest {
    @Test
    fun parsesDockerRowsAndSkipsMalformedLines() {
        val rows = parseDockerPs(
            "abc123|web|nginx:latest|running|Up 2 hours\n" +
                "malformed row\n" +
                "def456|db|postgres:16|exited|Exited (0) 1 hour ago"
        )
        assertEquals(2, rows.size)
        assertEquals("web", rows.first().name)
        assertTrue(rows.first().running)
        assertFalse(rows.last().running)
    }

    @Test
    fun parsesAndSortsListeningPorts() {
        val ports = parseListeningPorts(
            "LISTEN 0 128 0.0.0.0:8080 0.0.0.0:* users:((\"java\",pid=42,fd=9))\n" +
                "LISTEN 0 128 [::]:22 [::]:* users:((\"sshd\",pid=1,fd=3))\n" +
                "not a socket row"
        )
        assertEquals(listOf(22, 8080), ports.map { it.port })
        assertEquals("sshd", ports.first().process)
    }

    @Test
    fun detectsHttpsPorts() {
        assertTrue(likelyHttps(443))
        assertTrue(likelyHttps(8443))
        assertFalse(likelyHttps(8080))
    }

    @Test
    fun parsesSystemdServices() {
        val services = parseServices(
            "ssh.service loaded active running OpenBSD Secure Shell server\n" +
                "cups.service loaded inactive dead CUPS Scheduler"
        )
        assertEquals(2, services.size)
        assertEquals("cups.service", services.first().name)
        assertFalse(services.first().running)
        assertTrue(services.last().running)
    }
}
