package com.DevCiplak.advdisplay.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PageDataInfo {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("channel_code")
    @Expose
    private String channel_code;
    @SerializedName("page_code")
    @Expose
    private String page_code;
    @SerializedName("slot")
    @Expose
    private String slot;
    @SerializedName("filename")
    @Expose
    private String filename;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("CreatedDate")
    @Expose
    private String CreatedDate;
    @SerializedName("CreatedBy")
    @Expose
    private String CreatedBy;
    @SerializedName("UpdatedDate")
    @Expose
    private String UpdatedDate;
    @SerializedName("UpdatedBy")
    @Expose
    private String UpdatedBy;
    public  PageDataInfo(String id, String channel_code, String page_code, String slot, String filename, String status, String CreatedDate, String CreatedBy, String UpdatedDate, String UpdatedBy){
        this.id = id;
        this.channel_code = channel_code;
        this.page_code = page_code;
        this.slot = slot;
        this.filename = filename;
        this.status = status;
        this.CreatedDate = CreatedDate;
        this.CreatedBy = CreatedBy;
        this.UpdatedDate = UpdatedDate;
        this.UpdatedBy = UpdatedBy;
    }
    public String getId() {
        return id;
    }
    public String getChannel_code() {
        return channel_code;
    }
    public String getPage_code() {
        return page_code;
    }
    public String getSlot() {
        return slot;
    }
    public String getFilename() {
        return filename;
    }
    public String getStatus() {
        return status;
    }
    public String getCreatedDate() {
        return CreatedDate;
    }
    public String getCreatedBy() {
        return CreatedBy;
    }
    public String getUpdatedDate() {
        return UpdatedDate;
    }
    public String getUpdatedBy() {
        return UpdatedBy;
    }
}
