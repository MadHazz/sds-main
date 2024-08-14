package com.DevCiplak.advdisplay.RetrofitInterfaces;

import com.DevCiplak.advdisplay.Model.ContentInfoResponse;
import com.DevCiplak.advdisplay.constant.Constant;

import retrofit.http.Headers;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ContentInfoInterface {
    @GET
    public Call<ContentInfoResponse> getContentInfo(@Url String contentRequest);
}
