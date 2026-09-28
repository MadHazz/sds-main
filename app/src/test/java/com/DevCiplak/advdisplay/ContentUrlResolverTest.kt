package com.DevCiplak.advdisplay

import com.DevCiplak.advdisplay.util.ContentUrlResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentUrlResolverTest {

    @Test
    fun resolveDownloadUrl_keepsAbsoluteHttpsUrl() {
        val url = "https://cdn.example.com/video.mp4"

        val resolved = ContentUrlResolver.resolveDownloadUrl("https://api.example.com", url)

        assertEquals("https://cdn.example.com/video.mp4", resolved)
    }

    @Test
    fun resolveDownloadUrl_keepsAbsoluteHttpUrl() {
        val url = "http://cdn.example.com/video.mp4"

        val resolved = ContentUrlResolver.resolveDownloadUrl("https://api.example.com", url)

        assertEquals("http://cdn.example.com/video.mp4", resolved)
    }

    @Test
    fun resolveDownloadUrl_buildsAssetPathForRelativeFilename() {
        val resolved = ContentUrlResolver.resolveDownloadUrl(
            baseUrl = "https://api.example.com",
            rawPath = "clip.mp4"
        )

        assertEquals("https://api.example.com/assets/contents/clip.mp4", resolved)
    }

    @Test
    fun resolveDownloadUrl_trimsWhitespaceAndLeadingSlash() {
        val resolved = ContentUrlResolver.resolveDownloadUrl(
            baseUrl = "https://api.example.com/",
            rawPath = "  /folder/clip.mp4  "
        )

        assertEquals("https://api.example.com/assets/contents/folder/clip.mp4", resolved)
    }
}
