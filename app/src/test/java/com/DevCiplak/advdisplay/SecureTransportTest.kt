package com.DevCiplak.advdisplay

import com.DevCiplak.advdisplay.network.SecureTransport
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class SecureTransportTest {
    @Test fun releaseAllowsOnlyHttpsWithoutCredentials() {
        val policy = SecureTransport(allowLocalHttp = false)
        assertTrue(policy.allows("https://media.example/clip.mp4".toHttpUrl()))
        listOf("http://media.example/clip.mp4", "http://localhost:18080/", "https://user:pass@media.example/").forEach {
            assertFalse(policy.allows(it.toHttpUrl()))
        }
    }

    @Test fun debugHttpExceptionIsRestrictedToLocalFixtures() {
        val policy = SecureTransport(allowLocalHttp = true)
        listOf("localhost", "127.0.0.1", "10.0.2.2").forEach {
            assertTrue(policy.allows("http://$it:18080/".toHttpUrl()))
        }
        listOf("localhost.example", "192.168.1.1", "media.example").forEach {
            assertFalse(policy.allows("http://$it/".toHttpUrl()))
        }
    }
}
