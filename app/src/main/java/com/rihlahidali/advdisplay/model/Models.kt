package com.rihlahidali.advdisplay.model

import com.google.gson.annotations.SerializedName

data class ChannelData(
    @SerializedName("id") val id: String?,
    @SerializedName("refresh_rate") val refreshRate: String?
)

data class PageDetail(
    @SerializedName("page_code") val pageCode: String?,
    @SerializedName("template_id") val templateId: String?
)

data class PageDataInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("filename") val filename: String?,
    @SerializedName("slot") val slot: String?
)

data class ContentInfoResponse(
    @SerializedName("Status") val status: Boolean?,
    @SerializedName("Message") val message: String?,
    @SerializedName("page_detail") val pageDetail: List<PageDetail>?,
    @SerializedName("channel_data") val channelData: ChannelData?
)

data class DataContentResponse(
    @SerializedName("Status") val status: Boolean?,
    @SerializedName("Message") val message: String?,
    @SerializedName("page_data") val pageData: List<PageDataInfo>?
)

data class GetMenuTypeResponse(
    @SerializedName("Status") val status: Boolean?,
    @SerializedName("Message") val message: String?,
    @SerializedName("is_menu") val isMenu: String?
)
