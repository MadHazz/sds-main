package com.DevCiplak.advdisplay.RetrofitInterfaces;

import com.DevCiplak.advdisplay.Model.ContentInfoResponse;
import com.DevCiplak.advdisplay.Model.DataContentResponse;
import com.DevCiplak.advdisplay.constant.Constant;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface DataContentInterface {
    @GET
    public Call<DataContentResponse> getDataInfo(@Url String url);
}
