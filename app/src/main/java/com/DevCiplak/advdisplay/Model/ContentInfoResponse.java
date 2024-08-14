package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Array;
import java.util.Map;

public class ContentInfoResponse {
    @SerializedName("Status")
    @Expose
    private final Boolean status;
    @SerializedName("message")
    @Expose
    private final String message;
    @SerializedName("page_detail")
    @Expose
    private final PageDetail[] page_detail;
    @SerializedName("page_data")
    @Expose
    private final PageData page_data;
    @SerializedName("channel_data")
    @Expose
    private final ChannelData channel_data;
    public ContentInfoResponse(Boolean status, String message, PageDetail[] page_detail , PageData page_data, ChannelData channel_data) {
        this.status = status;
        this.message = message;
        this.page_detail = page_detail;
        this.page_data = page_data;
        this.channel_data = channel_data;
    }
    public Boolean getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
    public PageDetail[] getPage_detail() {
        return page_detail;
    }
    public PageData getPageData() {
        return page_data;
    }
    public ChannelData getChannelData() {
        return channel_data;
    }
}
