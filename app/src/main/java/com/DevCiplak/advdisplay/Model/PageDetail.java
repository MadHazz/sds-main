package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PageDetail {
    @SerializedName("page_code")
    @Expose
    private String page_code;
    @SerializedName("template_id")
    @Expose
    private String template_id;
    @SerializedName("template_name")
    @Expose
    private String template_name;
    @SerializedName("template_slots")
    @Expose
    private String template_slots;
    public  PageDetail(String page_code, String template_id, String template_name, String template_slots){
        this.page_code = page_code;
        this.template_id = template_id;
        this.template_name = template_name;
        this.template_slots = template_slots;
    }
    public String getPage_code() {
        return page_code;
    }
    public String getTemplate_id() { return template_id; }
    public String getTemplate_name() { return template_name; }
    public String getTemplate_slots() { return template_slots; }
}
