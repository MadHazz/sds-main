package com.DevCiplak.advdisplay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.DevCiplak.advdisplay.Model.ContentInfoResponse;
import com.DevCiplak.advdisplay.Model.GetMenuTypeResponse;
import com.DevCiplak.advdisplay.Model.PageDetail;
import com.DevCiplak.advdisplay.Retrofit.APIClient;
import com.DevCiplak.advdisplay.RetrofitInterfaces.ContentInfoInterface;
import com.DevCiplak.advdisplay.RetrofitInterfaces.GetMenuTypeInfoInterface;
import com.DevCiplak.advdisplay.constant.Constant;

import java.io.File;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainIdentifierActivity extends AppCompatActivity {
    Context mContext;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
    EditText editIdentify;
    Button IdentifyAction, okBtn;
    TextView deviceID, errorTitle;
    String coders, macAddress, deviceUID, identifierCode, getUID, menuType, menuTypeIdentify, identifierUID;
    SharedPreferences dataForward;
    ConstraintLayout loadingBackground, messageBox;
    ProgressBar progressBar;
    GetMenuTypeInfoInterface getMenuTypeInfoInterface;
    ContentInfoInterface contentInfoInterface;
    private static final int PERMISSION_STORAGE_CODE = 1000;
    Handler handler = new Handler(Looper.getMainLooper());
    @SuppressLint({"HardwareIds", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
        setContentView(R.layout.activity_main_identifier);
        SharedPreferences redirect = getSharedPreferences("advForward", MODE_PRIVATE);
        identifierCode = redirect.getString("codes", identifierCode);
        identifierUID = redirect.getString("deviceId", identifierUID);
        menuTypeIdentify = redirect.getString("menuType", menuTypeIdentify);
        if (identifierCode != null) {
            if (menuTypeIdentify != null) {
                switch (menuTypeIdentify) {
                    case "1":
                        Intent openMenuSlider = new Intent(MainIdentifierActivity.this, MenuSliderActivity.class);
                        startActivity(openMenuSlider);
                        break;
                    case "4":
                        Intent openVideoPlayer = new Intent(MainIdentifierActivity.this, VideoPlayerActivity.class);
                        startActivity(openVideoPlayer);
                        break;
                    case "0":
                        Intent openMain = new Intent(MainIdentifierActivity.this, WebViewActivity.class);
                        startActivity(openMain);
                        break;
                    default:
                        break;
                }
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        progressBar = findViewById(R.id.progressBar);
        messageBox = findViewById(R.id.messageBox);
        loadingBackground = findViewById(R.id.loadingBackground);
        errorTitle = findViewById(R.id.errorTitle);
        okBtn = findViewById(R.id.okBtn);
        editIdentify = findViewById(R.id.editIdentify);
        deviceID = findViewById(R.id.deviceID);
        loadingBackground.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        messageBox.setVisibility(View.GONE);
        editIdentify.setImeOptions(EditorInfo.IME_ACTION_DONE);
//        checkPermission();
        checkStoragePermission();
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        macAddress = wifiInf.getMacAddress();
        deviceUID = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(deviceUID.hashCode(), macAddress.hashCode());
        getUID = String.valueOf(deviceUuid);
//        deviceID.setText("Device ID: " + deviceUuid);
        IdentifyAction = findViewById(R.id.IdentifyAction);
        IdentifyAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMenuType();
            }
        });
    }
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainIdentifierActivity.this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainIdentifierActivity.this,
                    Manifest.permission.READ_PHONE_STATE)) {
            } else {
                ActivityCompat.requestPermissions(MainIdentifierActivity.this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        }
    }
    private void checkStoragePermission(){
        if (ContextCompat.checkSelfPermission(MainIdentifierActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // You can directly request the permission.
            ActivityCompat.requestPermissions(MainIdentifierActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_STORAGE_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_STORAGE_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Storage Access Permission Granted!");
            } else {
                showToast("Storage Access Permission Denied!");
            }
        }
    }
    public void getMenuType() {
        Log.d("getMenuFunc", "activated");
        loadingBackground.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        coders = editIdentify.getText().toString();
        getMenuTypeInfoInterface = APIClient.getClient().create(GetMenuTypeInfoInterface.class);
        getMenuTypeInfoInterface.getMenuType(Constant.GET_MENU + "u=" + getUID + "&" + "c=" + coders).enqueue(new Callback<GetMenuTypeResponse>() {
            @Override
            public void onResponse(Call<GetMenuTypeResponse> call, Response<GetMenuTypeResponse> response) {
                GetMenuTypeResponse getMenuResp = response.body();
                if (response.isSuccessful()){
                    Log.d("getMenuFunc", "respSuccess");
                    if (getMenuResp != null){
                        Log.d("getMenuFunc", "respoNoNull");
                        if(getMenuResp.getStatus().equals(true)){
                            Log.d("getMenuFunc", "respTrue");
                            menuType = getMenuResp.getIs_menu();
                            loadingBackground.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            messageBox.setVisibility(View.GONE);
                            errorTitle.setText(R.string.error);
                            Log.d("getMenuFunc", menuType);
                            if(menuType.equals("1")) {
                                getTemplateId();
                                Log.d("getMenuFunc", "type 1");
                            } else if (menuType.equals("0")) {
                                Log.d("getMenuFunc", "type 2");
                                editIdentify.setText("");
                                dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
                                SharedPreferences.Editor keyCode = dataForward.edit();
                                keyCode.putString("codes", coders);
                                keyCode.putString("deviceId", String.valueOf(getUID));
                                keyCode.putString("menuType", String.valueOf(menuType));
                                keyCode.apply();
                                Intent openMainActivity = new Intent(MainIdentifierActivity.this, WebViewActivity.class);
                                startActivity(openMainActivity);
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            messageBox.setVisibility(View.VISIBLE);
                            errorTitle.setText(getMenuResp.getMessage());
                            okBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    loadingBackground.setVisibility(View.GONE);
                                    messageBox.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.GONE);
                                    errorTitle.setText(R.string.error);
                                }
                            });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        messageBox.setVisibility(View.VISIBLE);
                        errorTitle.setText("Some data is missing! \n Please contact support.");
                        okBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                loadingBackground.setVisibility(View.GONE);
                                messageBox.setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                                errorTitle.setText(R.string.error);
                            }
                        });
                    }
                }
            }
            @Override
            public void onFailure(Call<GetMenuTypeResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                messageBox.setVisibility(View.VISIBLE);
                errorTitle.setText("No Internet Connection! \n Please make sure your device is connected to the internet");
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadingBackground.setVisibility(View.GONE);
                        messageBox.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        errorTitle.setText(R.string.error);
                    }
                });
            }
        });
    }
    public void getTemplateId() {
        contentInfoInterface = APIClient.getClient().create(ContentInfoInterface.class);
        contentInfoInterface.getContentInfo(Constant.GET_CONTENT + "u=" + getUID + "&c=" + coders).enqueue(new Callback<ContentInfoResponse>() {
            @Override
            public void onResponse(Call<ContentInfoResponse> call, Response<ContentInfoResponse> response) {
                ContentInfoResponse contentInfoData = response.body();
                if (contentInfoData != null) {
                    if (contentInfoData.getStatus().equals(true)) {

                        PageDetail[] pageDetail = contentInfoData.getPage_detail();

                        for (PageDetail detail : pageDetail) {
                            String templateID = detail.getTemplate_id();
                            if (templateID.equals("4")) {
                                editIdentify.setText("");
                                dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
                                SharedPreferences.Editor keyCode = dataForward.edit();
                                keyCode.putString("codes", coders);
                                keyCode.putString("deviceId", String.valueOf(getUID));
                                keyCode.putString("menuType", "4");
                                keyCode.apply();
                                createFolder(coders);
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent openMenuSlidActivity = new Intent(MainIdentifierActivity.this, VideoPlayerActivity.class);
                                        startActivity(openMenuSlidActivity);
                                    }
                                }, 3000);
                            } else {
                                editIdentify.setText("");
                                dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
                                SharedPreferences.Editor keyCode = dataForward.edit();
                                keyCode.putString("codes", coders);
                                keyCode.putString("deviceId", String.valueOf(getUID));
                                keyCode.putString("menuType", String.valueOf(menuType));
                                keyCode.apply();
                                Intent openMenuSlidActivity = new Intent(MainIdentifierActivity.this, MenuSliderActivity.class);
                                startActivity(openMenuSlidActivity);
                            }
                        }
                    } else {
                        Toast.makeText(mContext, "No Data! \n Please check internet or call support!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(mContext, "No Data! \n Please check internet or call support!", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<ContentInfoResponse> call, Throwable t) {
                Toast.makeText(mContext, "No Internet Connection! \n Please check your internet connection", Toast.LENGTH_LONG).show();
            }
        });
    }
    private void createFolder(String folderName) {
        // Get the directory where you want to create the folder
        File directory;
        if (isExternalStorageWritable()) {
            // If external storage is available and writable, create the folder in external storage
            directory = new File(getExternalFilesDir(null), folderName);
        } else {
            // If external storage is not available, create the folder in internal storage
            directory = new File(getFilesDir(), folderName);
        }
        // Create the directory if it does not exist
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (success) {
                showToast("Folder created successfully!");
            } else {
                showToast("Failed to create folder!");
            }
        } else {
            showToast("Folder already exists!");
        }
    }
    // Check if external storage is available and writable
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}