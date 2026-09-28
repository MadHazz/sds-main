package com.rihlahidali.advdisplay

import com.rihlahidali.advdisplay.model.*
import com.rihlahidali.advdisplay.network.ApiService
import retrofit2.Response

class TestApiService : ApiService {
    var menu = GetMenuTypeResponse(true, null, "1")
    var content = ContentInfoResponse(true, null, listOf(PageDetail("page", "4")), ChannelData("channel", "5000"))
    var data: suspend (String) -> Response<DataContentResponse> = {
        Response.success(DataContentResponse(true, null, listOf(PageDataInfo("media", "clip.mp4", null))))
    }
    var menuHandler: (suspend (String) -> Response<GetMenuTypeResponse>)? = null
    var pageCalls = 0
    override suspend fun getMenuType(uid: String, code: String) = menuHandler?.invoke(code) ?: Response.success(menu)
    override suspend fun getContentInfo(uid: String, code: String) = Response.success(content)
    override suspend fun getDataInfo(uid: String, pageCode: String): Response<DataContentResponse> {
        pageCalls++
        return data(pageCode)
    }
}
