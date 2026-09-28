package com.rihlahidali.advdisplay

import com.rihlahidali.advdisplay.security.WebNavigationPolicy
import org.junit.Assert.*
import org.junit.Test

class WebNavigationPolicyTest {
    private val policy = WebNavigationPolicy("https://advdisplay.example.com/")

    @Test fun allowsSameOriginNavigation() {
        assertTrue(policy.allows("https://advdisplay.example.com/display?c=ABC"))
        assertTrue(policy.allows("https://advdisplay.example.com:443/other"))
    }

    @Test fun blocksOtherOriginsDowngradesAndLocalSchemes() {
        listOf("https://advdisplay.example.com.evil.test/", "https://evil.test/", "http://advdisplay.example.com/",
            "https://advdisplay.example.com:444/", "file:///etc/passwd", "javascript:alert(1)",
            "intent://anything", "https://user:pass@advdisplay.example.com/").forEach { assertFalse(it, policy.allows(it)) }
    }
}
