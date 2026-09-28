package com.DevCiplak.advdisplay

import com.DevCiplak.advdisplay.data.MediaDownloader
import com.DevCiplak.advdisplay.data.PlaylistCache
import com.DevCiplak.advdisplay.model.*
import com.DevCiplak.advdisplay.repository.ContentRepository
import com.DevCiplak.advdisplay.viewmodel.PlaybackViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val temporary = TemporaryFolder()
    private val downloader = MediaDownloader { _, file -> file.writeText("media") }

    @Test fun apiFailureKeepsCachedPlaybackAvailable() = runTest {
        val folder = temporary.newFolder()
        val old = Playlist(listOf(MediaPage("4", listOf(MediaItem("https://example.com/old.mp4")))))
        PlaylistCache(folder, downloader).replace(old)
        val api = TestApiService().apply { data = { Response.success(DataContentResponse(false, "Page unavailable", null)) } }
        val vm = PlaybackViewModel(ContentRepository(api), downloader, main.dispatcher)
        vm.refresh("uid", "CODE", folder, PlaybackMode.VIDEO, true)
        advanceUntilIdle()
        assertEquals(old, vm.state.value.playlist)
        assertFalse(vm.state.value.syncing)
        assertEquals("Page unavailable", vm.state.value.message)
        assertEquals(old, PlaylistCache(folder, downloader).load(PlaybackMode.VIDEO))
    }

    @Test fun offlineRefreshDoesNotCallApiOrDiscardPlaylist() = runTest {
        val folder = temporary.newFolder()
        val old = Playlist(listOf(MediaPage("4", listOf(MediaItem("https://example.com/a.mp4")))))
        PlaylistCache(folder, downloader).replace(old)
        val api = TestApiService()
        val vm = PlaybackViewModel(ContentRepository(api), downloader, main.dispatcher)
        repeat(2) {
            vm.refresh("uid", "CODE", folder, PlaybackMode.VIDEO, false)
            advanceUntilIdle()
            assertEquals(old, vm.state.value.playlist)
        }
        assertEquals(0, api.pageCalls)
    }

    @Test fun concurrentRefreshesShareOneSyncAndRecoverAfterStop() = runTest {
        val folder = temporary.newFolder()
        val api = TestApiService().apply { data = {
            delay(1000)
            Response.success(DataContentResponse(true, null, listOf(PageDataInfo(null, "a.mp4", null))))
        } }
        val vm = PlaybackViewModel(ContentRepository(api), downloader, main.dispatcher)
        vm.refresh("uid", "CODE", folder, PlaybackMode.VIDEO, true)
        runCurrent()
        vm.refresh("uid", "CODE", folder, PlaybackMode.VIDEO, true)
        assertEquals(1, api.pageCalls)
        vm.stopSync()
        advanceUntilIdle()
        vm.refresh("uid", "CODE", folder, PlaybackMode.VIDEO, true)
        advanceUntilIdle()
        assertNotNull(vm.state.value.playlist)
        assertEquals(2, api.pageCalls)
        assertNull(vm.state.value.message)
    }

    @Test fun reloadsCommittedManifestInsteadOfUsingStaleInMemoryFiles() = runTest {
        val folder = temporary.newFolder()
        val cache = PlaylistCache(folder, downloader)
        val old = Playlist(listOf(MediaPage("4", listOf(MediaItem("https://example.com/old.mp4")))))
        val updated = Playlist(listOf(MediaPage("4", listOf(MediaItem("https://example.com/new.mp4")))))
        cache.replace(old)
        val vm = PlaybackViewModel(ContentRepository(TestApiService()), downloader, main.dispatcher)
        vm.refresh("uid", "CODE", folder, PlaybackMode.VIDEO, false)
        advanceUntilIdle()
        cache.replace(updated)
        vm.refresh("uid", "CODE", folder, PlaybackMode.VIDEO, false)
        advanceUntilIdle()
        assertEquals(updated, vm.state.value.playlist)
    }
}
