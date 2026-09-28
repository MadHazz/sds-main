package com.DevCiplak.advdisplay.util

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ContentUrlResolver {

    fun resolveDownloadUrl(baseUrl: String, rawPath: String): String {
        val path = rawPath.trim()
        require(path.isNotEmpty()) { "The server returned an empty media URL." }
        val absolute = path.toHttpUrlOrNull()
        if (absolute != null) {
            require(absolute.username.isEmpty() && absolute.password.isEmpty()) { "Media URL contains credentials." }
            return absolute.toString()
        }
        require(!path.contains(":") && !path.startsWith("//") && !path.contains('\\')) { "Invalid media URL." }
        val relative = path.trimStart('/')
        require(relative.split('/').none { it == ".." || it == "." || it.contains('%') }) { "Invalid media path." }
        val base = (baseUrl.trimEnd('/') + "/").toHttpUrl()
        val assetPath = if (relative.startsWith("assets/contents/")) relative else "assets/contents/$relative"
        return requireNotNull(base.resolve(assetPath)).toString()
    }
}
