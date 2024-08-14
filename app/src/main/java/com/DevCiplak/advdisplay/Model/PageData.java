package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Array;
import java.util.Map;

public class PageData {
    @SerializedName("page_code")
    @Expose
    private PageDataID main_id;
    public  PageData(PageDataID main_id){
        this.main_id = main_id;
    }
    public PageDataID getMain_id() {
        return main_id;
    }
}
