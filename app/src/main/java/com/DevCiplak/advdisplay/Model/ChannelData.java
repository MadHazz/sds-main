package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.SerializedName;

public class ChannelData {
    @SerializedName("id")
    String id;
    @SerializedName("orientation")
    String orientation;
    @SerializedName("menu")
    String menu;
    @SerializedName("code")
    String code;
    @SerializedName("name")
    String name;
    @SerializedName("status")
    String status;
    @SerializedName("refresh_rate")
    String refresh_rate;
    public String getId() {
        return id;
    }
    public String getOrientation() {
        return orientation;
    }
    public String getMenu() {
        return menu;
    }
    public String getCode() {
        return code;
    }
    public String getName() {
        return name;
    }
    public String getStatus() {
        return status;
    }
    public String getRefresh_rate() {
        return refresh_rate;
    }
}
