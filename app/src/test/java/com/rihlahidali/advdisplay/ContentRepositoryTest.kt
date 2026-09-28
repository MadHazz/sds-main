package com.rihlahidali.advdisplay

import com.rihlahidali.advdisplay.model.*
import com.rihlahidali.advdisplay.repository.ContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class ContentRepositoryTest {
    private val api = TestApiService()
    private val repository = ContentRepository(api, "https://test.example/")

    @Test fun rejectsPartialPageResponses() = runTest {
        api.content = api.content.copy(pageDetail = listOf(PageDetail("first", "4"), PageDetail("second", "4")))
        api.data = { page -> Response.success(DataContentResponse(page != "second", "Unavailable", listOf(PageDataInfo("1", "clip.mp4", null)))) }
        val result = runCatching { repository.getPlaylist("uid", "CODE", PlaybackMode.VIDEO) }
        assertTrue(result.isFailure)
        assertEquals(2, api.pageCalls)
    }

    @Test fun rejectsEmptyAndMissingFilenames() = runTest {
        for (name in listOf(null, "", " ")) {
            api.data = { Response.success(DataContentResponse(true, null, listOf(PageDataInfo("1", name, null)))) }
            assertTrue(runCatching { repository.getPlaylist("uid", "CODE", PlaybackMode.VIDEO) }.isFailure)
        }
    }

    @Test fun acceptsThreePanelTemplateWithoutTemplateTwo() = runTest {
        api.content = api.content.copy(pageDetail = listOf(PageDetail("panel", "3")))
        api.data = { Response.success(DataContentResponse(true, null, listOf("1", "2", "3").map {
            PageDataInfo(it, "panel-$it.png", it)
        })) }
        val result = repository.getPlaylist("uid", "CODE", PlaybackMode.SLIDER)
        assertEquals(1, result.pages.size)
        assertEquals("3", result.pages.single().templateId)
        assertEquals(listOf("1", "2", "3"), result.pages.single().media.map { it.slot })
        assertEquals("https://test.example/assets/contents/panel-1.png", result.pages.single().media.first().url)
    }

    @Test fun rejectsIncompleteThreePanelLayout() = runTest {
        api.content = api.content.copy(pageDetail = listOf(PageDetail("panel", "3")))
        assertTrue(runCatching { repository.getPlaylist("uid", "CODE", PlaybackMode.SLIDER) }.isFailure)
    }

    @Test fun rejectsChangedOrMixedModesInsteadOfDroppingPages() = runTest {
        api.content = api.content.copy(pageDetail = listOf(PageDetail("video", "4"), PageDetail("image", "2")))
        assertTrue(runCatching { repository.getPlaylist("uid", "CODE", PlaybackMode.VIDEO) }.isFailure)
        assertEquals(0, api.pageCalls)
    }

    @Test fun preservesApiPlaybackOrder() = runTest {
        api.data = { Response.success(DataContentResponse(true, null, listOf("z.mp4", "a.mp4").map { PageDataInfo(null, it, null) })) }
        assertEquals(listOf("z.mp4", "a.mp4"), repository.getPlaylist("u", "C", PlaybackMode.VIDEO)
            .pages.map { it.media.single().url.substringAfterLast('/') })
    }

    @Test fun convertsRefreshIntervalsAndBoundsInvalidValues() {
        assertEquals(5, ContentRepository.parseRefreshRateSeconds("5000"))
        assertEquals(30, ContentRepository.parseRefreshRateSeconds("30000"))
        assertEquals(300, ContentRepository.parseRefreshRateSeconds("300000"))
        assertEquals(900, ContentRepository.parseRefreshRateSeconds("900000"))
        assertEquals(45, ContentRepository.parseRefreshRateSeconds("45"))
        listOf(null, "", "bad", "0", "-1").forEach { assertEquals(5, ContentRepository.parseRefreshRateSeconds(it)) }
        assertEquals(86400, ContentRepository.parseRefreshRateSeconds(Long.MAX_VALUE.toString()))
    }
}
