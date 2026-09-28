package com.DevCiplak.advdisplay

import com.DevCiplak.advdisplay.data.MediaDownloader
import com.DevCiplak.advdisplay.data.PlaylistCache
import com.DevCiplak.advdisplay.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class PlaylistCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun playlist(vararg urls: String) = Playlist(urls.map { MediaPage("4", listOf(MediaItem(it))) })
    private val goodDownload = MediaDownloader { _, target -> target.writeText("complete media") }

    @Test fun failedReplacementPreservesLastPlaylistAndFiles() = runTest {
        val folder = temporary.newFolder()
        val cache = PlaylistCache(folder, goodDownload)
        val original = playlist("https://example.com/old.mp4")
        cache.replace(original)
        val failure = PlaylistCache(folder, MediaDownloader { url, file ->
            assertTrue(cache.fileFor(original.pages.single().media.single()).exists())
            assertEquals(original, cache.load(PlaybackMode.VIDEO))
            file.writeText("partial")
            if (url.endsWith("bad.mp4")) throw IOException("Disconnected")
        })
        assertTrue(runCatching { failure.replace(playlist("https://example.com/new.mp4", "https://example.com/bad.mp4")) }.isFailure)
        assertEquals(original, cache.load(PlaybackMode.VIDEO))
        assertEquals(2, folder.listFiles()!!.size)
        assertTrue(folder.listFiles()!!.none { it.extension == "part" })
    }

    @Test fun successfulReplacementCommitsOrderThenRemovesStaleMedia() = runTest {
        val cache = PlaylistCache(temporary.newFolder(), goodDownload)
        val old = playlist("https://example.com/old.mp4")
        cache.replace(old)
        val next = playlist("https://example.com/z.mp4", "https://example.com/a.mp4")
        cache.replace(next)
        assertEquals(next, cache.load(PlaybackMode.VIDEO))
        assertFalse(cache.fileFor(old.pages.single().media.single()).exists())
    }

    @Test fun unchangedUrlsAreNotDownloadedAgain() = runTest {
        var calls = 0
        val cache = PlaylistCache(temporary.newFolder(), MediaDownloader { _, file -> calls++; file.writeText("media") })
        val data = playlist("https://example.com/a.mp4")
        cache.replace(data)
        cache.replace(data)
        assertEquals(1, calls)
    }

    @Test fun cancellationRollsBackNewDownloads() = runTest {
        val folder = temporary.newFolder()
        val original = playlist("https://example.com/old.mp4")
        PlaylistCache(folder, goodDownload).replace(original)
        val cache = PlaylistCache(folder, MediaDownloader { _, file ->
            file.writeText("unfinished")
            throw CancellationException("Stopped")
        })
        val result = runCatching { cache.replace(playlist("https://example.com/new.mp4")) }
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(original, cache.load(PlaybackMode.VIDEO))
    }

    @Test fun emptyFilesNeverBecomePlayable() = runTest {
        val cache = PlaylistCache(temporary.newFolder(), MediaDownloader { _, file -> file.writeText("") })
        assertTrue(runCatching { cache.replace(playlist("https://example.com/empty.mp4")) }.isFailure)
        assertNull(cache.load(PlaybackMode.VIDEO))
    }

    @Test fun sameBasenameOnDifferentUrlsDoesNotCollide() {
        val cache = PlaylistCache(temporary.newFolder(), goodDownload)
        assertNotEquals(cache.fileFor(MediaItem("https://one.example/clip.mp4?a=1")),
            cache.fileFor(MediaItem("https://two.example/clip.mp4?a=1")))
    }

    @Test fun legacyVideosRemainAvailableBeforeFirstSuccessfulSync() {
        val folder = temporary.newFolder()
        File(folder, "old.MP4").writeText("old media")
        File(folder, "incomplete.mp4").writeText("")
        val cache = PlaylistCache(folder, goodDownload)
        val restored = requireNotNull(cache.load(PlaybackMode.VIDEO))
        assertEquals("old.MP4", cache.fileFor(restored.pages.single().media.single()).name)
    }

    @Test fun corruptManifestDoesNotSelectUncommittedFiles() {
        val folder = temporary.newFolder()
        File(folder, "playlist.json").writeText("not JSON")
        File(folder, "orphan.mp4").writeText("not committed")
        assertNull(PlaylistCache(folder, goodDownload).load(PlaybackMode.VIDEO))
    }
}
