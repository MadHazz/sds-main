package com.DevCiplak.advdisplay.RetrofitInterfaces;

import com.DevCiplak.advdisplay.Model.DataContentResponse;
import com.DevCiplak.advdisplay.Model.GetMenuTypeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface GetMenuTypeInfoInterface {
    @GET
    public Call<GetMenuTypeResponse> getMenuType(@Url String url);
}
