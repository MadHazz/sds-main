package com.DevCiplak.advdisplay;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.DevCiplak.advdisplay.Model.ContentInfoResponse;
import com.DevCiplak.advdisplay.Model.DataContentResponse;
import com.DevCiplak.advdisplay.Model.PageData;
import com.DevCiplak.advdisplay.Model.PageDataInfo;
import com.DevCiplak.advdisplay.Model.PageDetail;
import com.DevCiplak.advdisplay.Retrofit.APIClient;
import com.DevCiplak.advdisplay.RetrofitInterfaces.ContentInfoInterface;
import com.DevCiplak.advdisplay.RetrofitInterfaces.DataContentInterface;
import com.DevCiplak.advdisplay.constant.Constant;

import java.util.ArrayList;
import java.util.HashSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

import java.io.File;
import java.util.List;


public class PlaygroundActivity extends AppCompatActivity {
    TextView indicator;
    SharedPreferences dataForward;
    ProgressBar vertProgress;
    String codes, deviceId, menuType;
    ContentInfoInterface contentInfoInterface;
    DataContentInterface dataContentInterface;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playground);
        getUIItem();
        getIndicator();
    }
    public void getUIItem() {
        indicator = findViewById(R.id.indicator);
        vertProgress = findViewById(R.id.vertProgress);
    }
    public void getIndicator() {
        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
        codes = dataForward.getString("codes", codes);
        deviceId = dataForward.getString("deviceId", deviceId);
        menuType = dataForward.getString("menuType", menuType);
        contentInfoInterface = APIClient.getClient().create(ContentInfoInterface.class);
        contentInfoInterface.getContentInfo(Constant.GET_CONTENT + "u=" + deviceId + "&" + "c=" + codes).enqueue(new Callback<ContentInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<ContentInfoResponse> call, @NonNull Response<ContentInfoResponse> response) {
                ContentInfoResponse contentInfoDataVideo = response.body();
                if (contentInfoDataVideo != null) {
                    if (contentInfoDataVideo.getStatus().equals(true)) {
                        PageDetail[] pageDetail = contentInfoDataVideo.getPage_detail();
                        //To get the template ID
                        for (int k = 0; k < pageDetail.length; k++) {
                            String templateID = pageDetail[k].getTemplate_id();
                            if (templateID.equals("4")) {
                                int vidIndicator = pageDetail.length;
                                String vidInd = (String.valueOf(vidIndicator));
                                String indicatorText = getString(R.string.itemDownloadIndicator, vidInd);
                                indicator.setText(indicatorText);
                                dataContentInterface = APIClient.getClient().create(DataContentInterface.class);
                                dataContentInterface.getDataInfo(Constant.GET_DATA + "u=" + deviceId + "&" + "c=" + contentInfoDataVideo.getPage_detail()[k].getPage_code()).enqueue(new Callback<DataContentResponse>() {
                                    @Override
                                    public void onResponse(@NonNull Call<DataContentResponse> call, @NonNull Response<DataContentResponse> response) {
                                        DataContentResponse answer = response.body();
                                        if (response.isSuccessful()) {
                                            if (answer != null) {
                                                PageDataInfo[] pageDataInfo = answer.getPage_data();
                                                for (int l = 0; l < pageDataInfo.length; l++) {
                                                    vertProgress.setProgress(75);
                                                }
                                            }
                                        }
                                    }
                                    @Override
                                    public void onFailure(@NonNull Call<DataContentResponse> call, @NonNull Throwable t) {
                                        showToast("Unable to reach server \n Please make sure you're connected to the internet");
                                    }
                                });
                            }
                        }
                    } else {
                        showToast("No Data!\nPlease check your internet connection or call support");
                    }
                } else {
                    showToast("No Data!\nPlease check your internet connection or call support");
                }
            }
            @Override
            public void onFailure(@NonNull Call<ContentInfoResponse> call, @NonNull Throwable t) {
                showToast("Unable to reach server \n Please make sure you're connected to the internet");
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}