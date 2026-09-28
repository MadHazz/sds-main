package com.rihlahidali.advdisplay

import com.rihlahidali.advdisplay.model.ContentInfoResponse
import com.rihlahidali.advdisplay.model.GetMenuTypeResponse
import com.rihlahidali.advdisplay.model.PageDetail
import com.rihlahidali.advdisplay.viewmodel.MainIdentifierViewModel
import com.rihlahidali.advdisplay.viewmodel.UiState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class MainIdentifierViewModelTest {

    private val viewModel = MainIdentifierViewModel()

    @Test
    fun mapMenuTypeState_returnsSuccessForValidBody() {
        val body = GetMenuTypeResponse(status = true, message = "ok", isMenu = "1")
        val state = viewModel.mapMenuTypeState(Response.success(body))

        assertTrue(state is UiState.Success)
        assertEquals("1", (state as UiState.Success).data)
    }

    @Test
    fun mapMenuTypeState_returnsBodyMessageWhenStatusFalse() {
        val body = GetMenuTypeResponse(status = false, message = "Invalid code", isMenu = null)
        val state = viewModel.mapMenuTypeState(Response.success(body))

        assertTrue(state is UiState.Error)
        assertEquals("Invalid code", (state as UiState.Error).message)
    }

    @Test
    fun mapTemplateIdState_returnsTemplateIdFromFirstPageDetail() {
        val body = ContentInfoResponse(
            status = true,
            message = "ok",
            pageDetail = listOf(PageDetail(pageCode = "X", templateId = "4")),
            channelData = null
        )
        val state = viewModel.mapTemplateIdState(Response.success(body))

        assertTrue(state is UiState.Success)
        assertEquals("4", (state as UiState.Success).data)
    }

    @Test
    fun mapTemplateIdState_distinguishesServerFailureFromConnectivity() {
        val response = Response.error<ContentInfoResponse>(
            500,
            "{}".toResponseBody("application/json".toMediaType())
        )
        val state = viewModel.mapTemplateIdState(response)

        assertTrue(state is UiState.Error)
        assertEquals("Server request failed (HTTP 500). Please retry.", (state as UiState.Error).message)
    }
}
