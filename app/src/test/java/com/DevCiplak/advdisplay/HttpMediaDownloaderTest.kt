package com.DevCiplak.advdisplay

import com.DevCiplak.advdisplay.network.HttpMediaDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit

class HttpMediaDownloaderTest {
    @get:Rule val temporary = TemporaryFolder()
    private val server = MockWebServer()
    private val downloader = HttpMediaDownloader()

    @After fun closeServer() { server.shutdown() }

    @Test fun writesCompleteMediaResponse() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "video/mp4").setBody("complete video bytes"))
        val file = temporary.newFile()
        downloader.download(server.url("/clip.mp4").toString(), file)
        assertEquals("complete video bytes", file.readText())
    }

    @Test fun rejectsServerErrorAndHtmlInsteadOfMedia() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(runCatching { downloader.download(server.url("/clip.mp4").toString(), temporary.newFile()) }.isFailure)
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("login page"))
        assertTrue(runCatching { downloader.download(server.url("/clip.mp4").toString(), temporary.newFile()) }.isFailure)
    }

    @Test fun interruptedResponseIsNotReportedAsComplete() = runTest {
        server.enqueue(MockResponse().setBody("x".repeat(128000)).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        assertTrue(runCatching { downloader.download(server.url("/clip.mp4").toString(), temporary.newFile()) }.isFailure)
    }

    @Test fun cancellationClosesAnUnresponsiveRequestPromptly() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val url = server.url("/stalled.mp4").toString()
        val file = temporary.newFile()
        val job = launch(Dispatchers.IO) { downloader.download(url, file) }
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        withTimeout(2_000) { job.cancelAndJoin() }
        assertTrue(job.isCancelled)
        assertEquals(0L, file.length())
    }
}
