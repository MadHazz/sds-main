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
    private static final String PREF_NAME = "advForward";
    private static final String KEY_CODES = "codes";
    private static final String KEY_DEVICE_ID = "deviceId";
    private static final String KEY_MENU_TYPE = "menuType";
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
    private static final int PERMISSION_STORAGE_CODE = 1000;

    Context mContext;
    EditText editIdentify;
    Button IdentifyAction, okBtn;
    TextView deviceID, errorTitle;
    String coders, macAddress, deviceUID, identifierCode, getUID, menuType, menuTypeIdentify, identifierUID;
    ConstraintLayout loadingBackground, messageBox;
    ProgressBar progressBar;
    GetMenuTypeInfoInterface getMenuTypeInfoInterface;
    ContentInfoInterface contentInfoInterface;
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

        initializeViews();
        setupInitialState();
        checkStoragePermission();
        setupDeviceIdentifier();

        IdentifyAction.setOnClickListener(v -> getMenuType());
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        messageBox = findViewById(R.id.messageBox);
        loadingBackground = findViewById(R.id.loadingBackground);
        errorTitle = findViewById(R.id.errorTitle);
        okBtn = findViewById(R.id.okBtn);
        editIdentify = findViewById(R.id.editIdentify);
        deviceID = findViewById(R.id.deviceID);
        IdentifyAction = findViewById(R.id.IdentifyAction);
    }

    private void setupInitialState() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        loadingBackground.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        messageBox.setVisibility(View.GONE);
        editIdentify.setImeOptions(EditorInfo.IME_ACTION_DONE);

        SharedPreferences redirect = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        identifierCode = redirect.getString(KEY_CODES, null);
        identifierUID = redirect.getString(KEY_DEVICE_ID, null);
        menuTypeIdentify = redirect.getString(KEY_MENU_TYPE, null);

        if (identifierCode != null && menuTypeIdentify != null) {
            startActivityBasedOnMenuType(menuTypeIdentify);
        }
    }

    private void setupDeviceIdentifier() {
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        macAddress = wifiInf.getMacAddress();
        deviceUID = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(deviceUID.hashCode(), macAddress.hashCode());
        getUID = deviceUuid.toString();
    }

    private void startActivityBasedOnMenuType(String menuType) {
        Intent intent;
        switch (menuType) {
            case "1":
                intent = new Intent(this, MenuSliderActivity.class);
                break;
            case "4":
                intent = new Intent(this, VideoPlayerActivity.class);
                break;
            case "0":
                intent = new Intent(this, WebViewActivity.class);
                break;
            default:
                return;
        }
        startActivity(intent);
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_STORAGE_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_STORAGE_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                handleMenuTypeResponse(response);
            }

            @Override
            public void onFailure(Call<GetMenuTypeResponse> call, Throwable t) {
                handleApiError("No Internet Connection! \n Please make sure your device is connected to the internet");
            }
        });
    }

    private void handleMenuTypeResponse(Response<GetMenuTypeResponse> response) {
        GetMenuTypeResponse getMenuResp = response.body();
        if (response.isSuccessful() && getMenuResp != null) {
            if (getMenuResp.getStatus().equals(true)) {
                menuType = getMenuResp.getIs_menu();
                loadingBackground.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                messageBox.setVisibility(View.GONE);
                errorTitle.setText(R.string.error);
                Log.d("getMenuFunc", menuType);
                if (menuType.equals("1")) {
                    getTemplateId();
                } else if (menuType.equals("0")) {
                    handleWebViewRedirect();
                }
            } else {
                handleApiError(getMenuResp.getMessage());
            }
        } else {
            handleApiError("Some data is missing! \n Please contact support.");
        }
    }

    private void handleWebViewRedirect() {
        editIdentify.setText("");
        savePreferences(coders, getUID, menuType);
        Intent openMainActivity = new Intent(this, WebViewActivity.class);
        startActivity(openMainActivity);
    }

    private void handleApiError(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        messageBox.setVisibility(View.VISIBLE);
        errorTitle.setText(errorMessage);
        okBtn.setOnClickListener(v -> {
            loadingBackground.setVisibility(View.GONE);
            messageBox.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            errorTitle.setText(R.string.error);
        });
    }

    public void getTemplateId() {
        contentInfoInterface = APIClient.getClient().create(ContentInfoInterface.class);
        contentInfoInterface.getContentInfo(Constant.GET_CONTENT + "u=" + getUID + "&c=" + coders).enqueue(new Callback<ContentInfoResponse>() {
            @Override
            public void onResponse(Call<ContentInfoResponse> call, Response<ContentInfoResponse> response) {
                handleContentInfoResponse(response);
            }

            @Override
            public void onFailure(Call<ContentInfoResponse> call, Throwable t) {
                Toast.makeText(mContext, "No Internet Connection! \n Please check your internet connection", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleContentInfoResponse(Response<ContentInfoResponse> response) {
        ContentInfoResponse contentInfoData = response.body();
        if (contentInfoData != null && contentInfoData.getStatus().equals(true)) {
            PageDetail[] pageDetail = contentInfoData.getPage_detail();
            for (PageDetail detail : pageDetail) {
                String templateID = detail.getTemplate_id();
                if (templateID.equals("4")) {
                    handleVideoPlayerRedirect();
                } else {
                    handleMenuSliderRedirect();
                }
            }
        } else {
            Toast.makeText(mContext, "No Data! \n Please check internet or call support!", Toast.LENGTH_LONG).show();
        }
    }

    private void handleVideoPlayerRedirect() {
        editIdentify.setText("");
        savePreferences(coders, getUID, "4");
        createFolder(coders);
        handler.postDelayed(() -> {
            Intent openVideoPlayerActivity = new Intent(this, VideoPlayerActivity.class);
            startActivity(openVideoPlayerActivity);
        }, 3000);
    }

    private void handleMenuSliderRedirect() {
        editIdentify.setText("");
        savePreferences(coders, getUID, menuType);
        Intent openMenuSliderActivity = new Intent(this, MenuSliderActivity.class);
        startActivity(openMenuSliderActivity);
    }

    private void savePreferences(String code, String deviceId, String menuType) {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_CODES, code);
        editor.putString(KEY_DEVICE_ID, deviceId);
        editor.putString(KEY_MENU_TYPE, menuType);
        editor.apply();
    }

    private void createFolder(String folderName) {
        File directory = isExternalStorageWritable() ?
                new File(getExternalFilesDir(null), folderName) :
                new File(getFilesDir(), folderName);

        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            showToast(success ? "Folder created successfully!" : "Failed to create folder!");
        } else {
            showToast("Folder already exists!");
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
