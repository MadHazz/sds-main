package com.rihlahidali.advdisplay.repository

import com.rihlahidali.advdisplay.model.ContentInfoResponse
import com.rihlahidali.advdisplay.model.DataContentResponse
import com.rihlahidali.advdisplay.model.GetMenuTypeResponse
import com.rihlahidali.advdisplay.model.MediaItem
import com.rihlahidali.advdisplay.model.MediaPage
import com.rihlahidali.advdisplay.model.PlaybackMode
import com.rihlahidali.advdisplay.model.Playlist
import com.rihlahidali.advdisplay.constant.Constant
import com.rihlahidali.advdisplay.util.ContentUrlResolver
import com.rihlahidali.advdisplay.network.APIClient
import com.rihlahidali.advdisplay.network.ApiService
import retrofit2.Response

class ContentRepository(
    private val apiService: ApiService = APIClient.client.create(ApiService::class.java),
    private val baseUrl: String = Constant.BASE_URL
) {

    suspend fun getMenuType(uid: String, code: String): Response<GetMenuTypeResponse> {
        return apiService.getMenuType(uid, code)
    }

    suspend fun getContentInfo(uid: String, code: String): Response<ContentInfoResponse> {
        return apiService.getContentInfo(uid, code)
    }

    suspend fun getDataInfo(uid: String, pageCode: String): Response<DataContentResponse> {
        return apiService.getDataInfo(uid, pageCode)
    }

    suspend fun getPlaylist(uid: String, code: String, mode: PlaybackMode): Playlist {
        val content = getContentInfo(uid, code).requiredBody()
        check(content.status == true) { content.message ?: "The server could not load this screen." }
        val details = content.pageDetail.orEmpty()
        check(details.all {
            if (mode == PlaybackMode.VIDEO) it.templateId == "4" else it.templateId in setOf("2", "3")
        }) { "The screen's playback mode changed. Return to setup and identify again." }
        check(details.isNotEmpty()) { "No supported content is assigned to this screen." }
        val pages = details.flatMap { detail ->
            val pageCode = detail.pageCode?.takeIf { it.isNotBlank() }
                ?: error("A page is missing its code.")
            val data = getDataInfo(uid, pageCode).requiredBody()
            // A partial response must never become an authoritative replacement playlist.
            check(data.status == true) { data.message ?: "A content page could not be loaded." }
            val items = data.pageData.orEmpty().map { info ->
                MediaItem(ContentUrlResolver.resolveDownloadUrl(baseUrl, info.filename.orEmpty()), info.slot)
            }
            check(items.isNotEmpty()) { "A content page has no media." }
            if (detail.templateId == "3") {
                check(items.map { it.slot }.toSet() == setOf("1", "2", "3") && items.size == 3) {
                    "The three-panel template requires slots 1, 2 and 3."
                }
                listOf(MediaPage("3", items))
            } else {
                items.map { MediaPage(requireNotNull(detail.templateId), listOf(it)) }
            }
        }
        return Playlist(pages, parseRefreshRateSeconds(content.channelData?.refreshRate))
    }

    companion object {
        // Existing presets are milliseconds; retain support for legacy custom values in seconds.
        fun parseRefreshRateSeconds(raw: String?): Int {
            val value = raw?.trim()?.toLongOrNull() ?: return 5
            if (value <= 0) return 5
            return (if (value >= 1000) value / 1000 else value).coerceIn(1, 86400).toInt()
        }
    }
}

internal fun <T> Response<T>.requiredBody(): T {
    check(isSuccessful) { "Server request failed (HTTP ${code()}). Please retry." }
    return body() ?: error("The server returned an empty response.")
}
