package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GetMenuTypeResponse {
    @SerializedName("Status")
    @Expose
    private final Boolean status;
    @SerializedName("message")
    @Expose
    private final String message;
    @SerializedName("is_menu")
    @Expose
    private final String is_menu;
    public GetMenuTypeResponse(Boolean status, String message, String is_menu) {
        this.status = status;
        this.message = message;
        this.is_menu = is_menu;
    }
    public Boolean getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
    public String getIs_menu() {
        return is_menu;
    }
}
