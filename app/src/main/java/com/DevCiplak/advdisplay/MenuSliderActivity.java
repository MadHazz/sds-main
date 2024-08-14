package com.DevCiplak.advdisplay;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.DevCiplak.advdisplay.Adapter.SliderAdapter;
import com.DevCiplak.advdisplay.Model.ContentInfoResponse;
import com.DevCiplak.advdisplay.Model.PageDetail;
import com.DevCiplak.advdisplay.Retrofit.APIClient;
import com.DevCiplak.advdisplay.RetrofitInterfaces.ContentInfoInterface;
import com.DevCiplak.advdisplay.RetrofitInterfaces.DataContentInterface;
import com.DevCiplak.advdisplay.constant.Constant;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderPager;
import com.smarteist.autoimageslider.SliderView;

import android.widget.Toast;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuSliderActivity extends AppCompatActivity {
    Context mContext;
    SliderView sliderView;
    ContentInfoInterface contentInfoInterface;
    DataContentInterface dataContentInterface;
    SharedPreferences dataForward;
    String codes, deviceId, menuType;
    ConstraintLayout loadingBack, messageBoxSlider;
    TextView messageTitle;
    Button okBtn2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
        setContentView(R.layout.activity_menu_slider);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        loadingBack = findViewById(R.id.loadingBack);
        messageBoxSlider = findViewById(R.id.messageBoxSlider);
        messageTitle = findViewById(R.id.messageTitle);
        okBtn2 = findViewById(R.id.okBtn2);
        sliderView = findViewById(R.id.slider);
        loadingBack.setVisibility(View.GONE);
        messageBoxSlider.setVisibility(View.GONE);
        messageTitle.setVisibility(View.GONE);
        okBtn2.setVisibility(View.GONE);
        sliderView.setVisibility(View.GONE);
        getContent();
    }
    public void getContent() {
        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
        codes = dataForward.getString("codes", codes);
        deviceId = dataForward.getString("deviceId", deviceId);
        menuType = dataForward.getString("menuType", menuType);
        contentInfoInterface = APIClient.getClient().create(ContentInfoInterface.class);
        contentInfoInterface.getContentInfo(Constant.GET_CONTENT + "u=" + deviceId + "&" + "c=" + codes).enqueue(new Callback<ContentInfoResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<ContentInfoResponse> call, Response<ContentInfoResponse> response) {
                ContentInfoResponse reply = response.body();
                //Check if response is not null
                if (reply != null) {
                    //Check if the status is true
                    if (reply.getStatus().equals(true)) {
                        //Get page detail data
                        PageDetail[] pageDetail = reply.getPage_detail();
                        //Loop array data for page detail
                        for (PageDetail detail : pageDetail) {
                            //Fetch template id as string
                            String templateID = detail.getTemplate_id();
                            //Check if template id is 4
                            if (templateID.equals("2")) {
                                sliderView.setVisibility(View.VISIBLE);
                                mContext = getApplicationContext();
                                SliderAdapter sliderAdapter = new SliderAdapter(Arrays.asList((reply.getPage_detail())), getApplicationContext(), deviceId);
                                sliderView.setSliderAdapter(sliderAdapter);
                                sliderView.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION);
                                switch (reply.getChannelData().getRefresh_rate()) {
                                    case "5000":
                                        sliderView.setScrollTimeInSec(5);
                                        break;
                                    case "10000":
                                        sliderView.setScrollTimeInSec(10);
                                        break;
                                    case "15000":
                                        sliderView.setScrollTimeInSec(15);
                                        break;
                                    case "30000":
                                        sliderView.setScrollTimeInSec(30);
                                        break;
                                    case "300000":
                                        sliderView.setScrollTimeInSec(500);
                                        break;
                                    case "900000":
                                        sliderView.setScrollTimeInSec(1500);
                                        break;
                                    default:
                                        int time = Integer.parseInt(reply.getChannelData().getRefresh_rate());
                                        sliderView.setScrollTimeInSec(time);
                                        break;
                                }
                                sliderView.setAutoCycleDirection(1);
                                sliderView.startAutoCycle();
                            }
                        }
                    } else {
                        loadingBack.setVisibility(View.VISIBLE);
                        messageBoxSlider.setVisibility(View.VISIBLE);
                        messageTitle.setVisibility(View.VISIBLE);
                        okBtn2.setVisibility(View.VISIBLE);
                        messageTitle.setText(reply.getMessage());
                        okBtn2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                loadingBack.setVisibility(View.GONE);
                                messageBoxSlider.setVisibility(View.GONE);
                                messageTitle.setVisibility(View.GONE);
                                okBtn2.setVisibility(View.GONE);
                                finish();
                            }
                        });
                    }
                } else {
                    loadingBack.setVisibility(View.VISIBLE);
                    messageBoxSlider.setVisibility(View.VISIBLE);
                    messageTitle.setVisibility(View.VISIBLE);
                    okBtn2.setVisibility(View.VISIBLE);
                    messageTitle.setText(R.string.someDataMissing);
                    okBtn2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            loadingBack.setVisibility(View.GONE);
                            messageBoxSlider.setVisibility(View.GONE);
                            messageTitle.setVisibility(View.GONE);
                            okBtn2.setVisibility(View.GONE);
                            finish();
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call<ContentInfoResponse> call, Throwable t) {
                loadingBack.setVisibility(View.VISIBLE);
                messageBoxSlider.setVisibility(View.VISIBLE);
                messageTitle.setVisibility(View.VISIBLE);
                okBtn2.setVisibility(View.VISIBLE);
                messageTitle.setText(R.string.noInternet);
                okBtn2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadingBack.setVisibility(View.GONE);
                        messageBoxSlider.setVisibility(View.GONE);
                        messageTitle.setVisibility(View.GONE);
                        okBtn2.setVisibility(View.GONE);
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_password, null);
        final EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);

        builder.setView(dialogView)
                .setTitle("Warning!")
                .setMessage("All data will be cleared if you proceed!\nAre you sure you want to go back?")
                .setPositiveButton("Yes", (DialogInterface.OnClickListener) (dialog, which) -> {
                        String enteredPassword = passwordEditText.getText().toString();
                        if (enteredPassword.equals("1234")) {
                            dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
                            SharedPreferences.Editor keys = dataForward.edit();
                            keys.remove("codes").apply();
                            keys.remove("deviceId").apply();
                            keys.remove("menuType").apply();
                            super.onBackPressed();
                        } else {
                            Toast.makeText(mContext, "Incorrect password", Toast.LENGTH_SHORT).show();
                        }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}