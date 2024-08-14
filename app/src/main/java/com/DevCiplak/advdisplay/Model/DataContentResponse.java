package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DataContentResponse {
    @SerializedName("Status")
    @Expose
    private final Boolean status;
    @SerializedName("message")
    @Expose
    private final String message;
    @SerializedName("page_data")
    @Expose
    private final PageDataInfo[] page_data;
    public DataContentResponse(Boolean status, String message, PageDataInfo[] page_data) {
        this.status = status;
        this.message = message;
        this.page_data = page_data;
    }
    public Boolean getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
    public PageDataInfo[] getPage_data() {
        return page_data;
    }
}
