package com.DevCiplak.advdisplay

import com.DevCiplak.advdisplay.model.*
import com.DevCiplak.advdisplay.repository.ContentRepository
import com.DevCiplak.advdisplay.viewmodel.IdentificationState
import com.DevCiplak.advdisplay.viewmodel.MainIdentifierViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class IdentificationFlowTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun newerRequestCancelsOlderCodeAndKeepsInviteAttribution() = runTest {
        val api = TestApiService().apply { menuHandler = { code ->
            if (code == "OLD") delay(5000)
            Response.success(GetMenuTypeResponse(true, null, "0"))
        } }
        val vm = MainIdentifierViewModel(ContentRepository(api))
        vm.identify("device", "OLD")
        runCurrent()
        vm.identify("device", " new ", true)
        advanceUntilIdle()
        assertEquals(IdentificationState.Ready("NEW", "device", PlaybackMode.WEB, true), vm.state.value)
        vm.resetState()
        assertEquals(IdentificationState.Idle, vm.state.value)
    }

    @Test fun missingOrMixedTemplatesDoNotLaunchWrongPlayer() = runTest {
        val api = TestApiService()
        val vm = MainIdentifierViewModel(ContentRepository(api))
        for (templates in listOf(emptyList(), listOf("2", "4"), listOf("unknown"))) {
            api.content = api.content.copy(pageDetail = templates.map { PageDetail("page", it) })
            vm.identify("device", "CODE")
            advanceUntilIdle()
            assertTrue(vm.state.value is IdentificationState.Error)
        }
    }

    @Test fun templateThreeAloneRoutesToSlider() = runTest {
        val api = TestApiService().apply { content = content.copy(pageDetail = listOf(PageDetail("page", "3"))) }
        val vm = MainIdentifierViewModel(ContentRepository(api))
        vm.identify("device", "CODE")
        advanceUntilIdle()
        assertEquals(PlaybackMode.SLIDER, (vm.state.value as IdentificationState.Ready).mode)
    }

    @Test fun unsafeIdentifierRejectedBeforeApiRequest() = runTest {
        val api = TestApiService().apply { menuHandler = { error("Must not request the API") } }
        val vm = MainIdentifierViewModel(ContentRepository(api))
        vm.identify("device", "../escape")
        advanceUntilIdle()
        val state = vm.state.value as IdentificationState.Error
        assertTrue(state.message.startsWith("Enter a code"))
    }
}
