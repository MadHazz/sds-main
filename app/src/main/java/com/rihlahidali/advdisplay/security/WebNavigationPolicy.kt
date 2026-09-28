package com.rihlahidali.advdisplay.security

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class WebNavigationPolicy(baseUrl: String) {
    private val origin = requireNotNull(baseUrl.toHttpUrlOrNull())

    fun allows(rawUrl: String): Boolean {
        val url = rawUrl.toHttpUrlOrNull() ?: return false
        return url.scheme == origin.scheme && url.host == origin.host && url.port == origin.port &&
            url.username.isEmpty() && url.password.isEmpty()
    }
}
