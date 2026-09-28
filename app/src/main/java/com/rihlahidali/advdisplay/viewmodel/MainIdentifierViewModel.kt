package com.rihlahidali.advdisplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rihlahidali.advdisplay.model.ContentInfoResponse
import com.rihlahidali.advdisplay.model.GetMenuTypeResponse
import com.rihlahidali.advdisplay.model.PlaybackMode
import com.rihlahidali.advdisplay.repository.ContentRepository
import com.rihlahidali.advdisplay.util.IdentifierCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

sealed class IdentificationState {
    object Idle : IdentificationState()
    object Loading : IdentificationState()
    data class Ready(val code: String, val deviceId: String, val mode: PlaybackMode, val fromInvite: Boolean) : IdentificationState()
    data class Error(val message: String) : IdentificationState()
}

class MainIdentifierViewModel(private val repository: ContentRepository = ContentRepository()) : ViewModel() {
    private val mutableState = MutableStateFlow<IdentificationState>(IdentificationState.Idle)
    val state = mutableState.asStateFlow()
    private var request: Job? = null

    fun resetState() {
        request?.cancel()
        mutableState.value = IdentificationState.Idle
    }

    fun showError(message: String) {
        request?.cancel()
        mutableState.value = IdentificationState.Error(message)
    }

    fun identify(uid: String, input: String, fromInvite: Boolean = false) {
        request?.cancel()
        request = viewModelScope.launch {
            mutableState.value = IdentificationState.Loading
            try {
                val code = IdentifierCode.normalize(input)
                val menu = mapMenuTypeState(repository.getMenuType(uid, code)).valueOrThrow()
                val mode = if (menu == "0") PlaybackMode.WEB else {
                    val template = mapTemplateIdState(repository.getContentInfo(uid, code)).valueOrThrow()
                    if (template == "4") PlaybackMode.VIDEO else PlaybackMode.SLIDER
                }
                mutableState.value = IdentificationState.Ready(code, uid, mode, fromInvite)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = IdentificationState.Error(error.message ?: "Could not identify this screen. Please retry.")
            }
        }
    }

    internal fun mapMenuTypeState(response: Response<GetMenuTypeResponse>): UiState<String> {
        if (!response.isSuccessful) return UiState.Error("Server request failed (HTTP ${response.code()}). Please retry.")
        val body = response.body()
        return when {
            body?.status != true -> UiState.Error(body?.message ?: "The server returned no screen information.")
            body.isMenu !in setOf("0", "1") -> UiState.Error("Unsupported menu type. Contact your content administrator.")
            else -> UiState.Success(requireNotNull(body.isMenu))
        }
    }

    internal fun mapTemplateIdState(response: Response<ContentInfoResponse>): UiState<String> {
        if (!response.isSuccessful) return UiState.Error("Server request failed (HTTP ${response.code()}). Please retry.")
        val body = response.body()
        val templates = body?.pageDetail.orEmpty().map { it.templateId }.toSet()
        return when {
            body?.status != true -> UiState.Error(body?.message ?: "The server returned no content information.")
            templates == setOf("4") -> UiState.Success("4")
            templates.isNotEmpty() && templates.all { it == "2" || it == "3" } -> UiState.Success(requireNotNull(templates.first()))
            else -> UiState.Error("Assign either image templates (2/3) or video template (4) to this screen.")
        }
    }

    private fun UiState<String>.valueOrThrow(): String = when (this) {
        is UiState.Success -> data
        is UiState.Error -> error(message)
        UiState.Loading -> error("Identification has not completed.")
    }
}
