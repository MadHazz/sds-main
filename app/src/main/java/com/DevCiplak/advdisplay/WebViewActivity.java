package com.DevCiplak.advdisplay;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class WebViewActivity extends AppCompatActivity {
    ProgressBar progressBar;
    SharedPreferences sharedPreferences;
    String codes, deviceId, UserSA;
    ConstraintLayout loadingBack, messageBoxSlider;
    TextView messageTitle;
    Button okBtn2;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
        setContentView(R.layout.activity_web_view);
        loadingBack = findViewById(R.id.loadingBack);
        messageBoxSlider = findViewById(R.id.messageBoxSlider);
        messageTitle = findViewById(R.id.messageTitle);
        okBtn2 = findViewById(R.id.okBtn2);
        progressBar = findViewById(R.id.progressBar);
        loadingBack.setVisibility(View.GONE);
        messageBoxSlider.setVisibility(View.GONE);
        messageTitle.setVisibility(View.GONE);
        okBtn2.setVisibility(View.GONE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        sharedPreferences = getSharedPreferences("advForward", MODE_PRIVATE);
        codes = sharedPreferences.getString("codes", codes);
        deviceId = sharedPreferences.getString("deviceId", deviceId);
        String url = "https://sds.par-crm.com/display?c="+codes+"&u="+deviceId;
        WebView view = findViewById(R.id.webView);
        view.getSettings().setRenderPriority(WebSettings.RenderPriority.NORMAL);
        view.getSettings().getCacheMode();
        view.getSettings().setDatabaseEnabled(true);
        view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        view.getSettings().setDomStorageEnabled(true);
        view.getSettings().setUseWideViewPort(true);
        view.getSettings().setSavePassword(true);
        view.getSettings().setSaveFormData(true);
        view.getSettings().setEnableSmoothTransition(true);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setGeolocationEnabled(true);
        view.getSettings().setLightTouchEnabled(true);
        view.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        view.getSettings().setLoadWithOverviewMode(true);
        view.getSettings().setUseWideViewPort(true);
        view.getSettings().setMediaPlaybackRequiresUserGesture(false);
        view.getSettings().setPluginState(WebSettings.PluginState.ON);
        view.requestFocus();
        view.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });
        view.loadUrl(url);
        CookieManager.getInstance().acceptCookie();
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);
    }
//    @Override
//    public void onBackPressed() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
//        builder.setTitle("Warning!");
//        builder.setMessage("All data will be cleared if you proceed! \n Are you sure you want to go back?");
//        builder.setCancelable(false);
//        builder.setPositiveButton("Yes", (DialogInterface.OnClickListener) (dialog, which) -> {
//            super.onBackPressed();
//            sharedPreferences = getSharedPreferences("advForward", MODE_PRIVATE);
//            SharedPreferences.Editor keys = sharedPreferences.edit();
//            keys.remove("codes").apply();
//            keys.remove("deviceId").apply();
//            keys.remove("menuType").apply();
//            WebView view = findViewById(R.id.webView);
//            view.clearCache(true);
//        });
//        builder.setNegativeButton("No", (DialogInterface.OnClickListener) (dialog, which) -> {
//            dialog.cancel();
//        });
//        AlertDialog alertDialog = builder.create();
//        alertDialog.show();
//    }

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
                        sharedPreferences = getSharedPreferences("advForward", MODE_PRIVATE);
                        SharedPreferences.Editor keys = sharedPreferences.edit();
                        keys.remove("codes").apply();
                        keys.remove("deviceId").apply();
                        keys.remove("menuType").apply();
                        WebView view = findViewById(R.id.webView);
                        view.clearCache(true);
                        super.onBackPressed();
                    } else {
                        Toast.makeText(WebViewActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
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