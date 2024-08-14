package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PageDataID {
    @SerializedName("1")
    @Expose
    private PageDataContent sort_id;
    public  PageDataID(PageDataContent sort_id){
        this.sort_id = sort_id;
    }
    public PageDataContent getSort_id() {
        return sort_id;
    }
}
