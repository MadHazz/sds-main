package com.rihlahidali.advdisplay.network

import com.rihlahidali.advdisplay.constant.Constant
import com.rihlahidali.advdisplay.model.ContentInfoResponse
import com.rihlahidali.advdisplay.model.DataContentResponse
import com.rihlahidali.advdisplay.model.GetMenuTypeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET(Constant.DISPLAY_MENU_TYPE_ENDPOINT)
    suspend fun getMenuType(
        @Query("u") uid: String,
        @Query("c") code: String
    ): Response<GetMenuTypeResponse>

    @GET(Constant.DISPLAY_MENU_ENDPOINT)
    suspend fun getContentInfo(
        @Query("u") uid: String,
        @Query("c") code: String
    ): Response<ContentInfoResponse>

    @GET(Constant.DISPLAY_PAGE_ENDPOINT)
    suspend fun getDataInfo(
        @Query("u") uid: String,
        @Query("c") pageCode: String
    ): Response<DataContentResponse>
}
