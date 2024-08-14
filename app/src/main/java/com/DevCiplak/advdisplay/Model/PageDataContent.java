package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PageDataContent {
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("slot")
    @Expose
    private String slot;
    public  PageDataContent(String url, String slot){
        this.url = url;
        this.slot = slot;
    }
    public String getUrl() {
        return url;
    }
    public String getSlot() { return  slot;}
}
