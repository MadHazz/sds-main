package com.DevCiplak.advdisplay

import com.DevCiplak.advdisplay.security.WebNavigationPolicy
import org.junit.Assert.*
import org.junit.Test

class WebNavigationPolicyTest {
    private val policy = WebNavigationPolicy("https://sds.example.com/")

    @Test fun allowsSameOriginNavigation() {
        assertTrue(policy.allows("https://sds.example.com/display?c=ABC"))
        assertTrue(policy.allows("https://sds.example.com:443/other"))
    }

    @Test fun blocksOtherOriginsDowngradesAndLocalSchemes() {
        listOf("https://sds.example.com.evil.test/", "https://evil.test/", "http://sds.example.com/",
            "https://sds.example.com:444/", "file:///etc/passwd", "javascript:alert(1)",
            "intent://anything", "https://user:pass@sds.example.com/").forEach { assertFalse(it, policy.allows(it)) }
    }
}
