package com.DevCiplak.advdisplay;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.DevCiplak.advdisplay.Model.ContentInfoResponse;
import com.DevCiplak.advdisplay.Model.DataContentResponse;
import com.DevCiplak.advdisplay.Model.PageDetail;
import com.DevCiplak.advdisplay.Retrofit.APIClient;
import com.DevCiplak.advdisplay.RetrofitInterfaces.ContentInfoInterface;
import com.DevCiplak.advdisplay.RetrofitInterfaces.DataContentInterface;
import com.DevCiplak.advdisplay.constant.Constant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";
    private VideoView videoViewer;
    private ConstraintLayout loadingBack, messageBox;
    private TextView errorTitle;
    private ProgressBar progressBar;
    private SharedPreferences dataForward;
    private String codes, menuType, deviceId;
    private String[] urlsArray, DelUrlsArray;
    private DataContentInterface dataContentInterface;
    private ContentInfoInterface contentInfoInterface;
    private Uri videoUri;
    private int currentIndex = 0;

    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    loadingBack.setVisibility(View.GONE);
                    messageBox.setVisibility(View.GONE);
                    errorTitle.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    videoViewer.setVisibility(View.VISIBLE);
                    playVideoLoop();
                }, 30000);
            } else {
                showToast("Download failed!\nPlease make sure you have a strong internet connection!");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag");
        wakeLock.acquire();

        setContentView(R.layout.activity_video_player);

        videoViewer = findViewById(R.id.videoView);
        loadingBack = findViewById(R.id.loadingBack);
        messageBox = findViewById(R.id.messageBox);
        errorTitle = findViewById(R.id.errorTitle);
        progressBar = findViewById(R.id.progressBar);

        // Set Visibility for views
        setLoadingVisibility(View.VISIBLE);

        checkFile();

        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkFile();
                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(5));
            }
        });

        registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        videoViewer.resume();
    }

    private void setLoadingVisibility(int visibility) {
        loadingBack.setVisibility(visibility);
        messageBox.setVisibility(visibility);
        errorTitle.setVisibility(visibility);
        progressBar.setVisibility(visibility);
    }

    public void resetVideoView() {
        Log.d("resetVideoView", "Video reset");
        videoViewer.pause();
        videoViewer.stopPlayback();
        videoViewer.suspend();
        videoViewer.setOnPreparedListener(null);
        videoViewer.setVideoURI(null);
        currentIndex = 0;
        videoViewer.seekTo(0);
    }

    public boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public void checkFile() {
        resetVideoView();
        videoViewer.setVisibility(View.GONE);

        Log.d("checkFile", "5 minutes pass");
        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
        codes = dataForward.getString("codes", codes);
        String directoryPath = getExternalFilesDir(null) + File.separator + codes;
        File dir = new File(directoryPath);
        File[] children = dir.listFiles();

        if (children != null) {
            if (isConnected()) {
                if (children.length > 0) {
                    setLoadingVisibility(View.VISIBLE);
                    errorTitle.setText(R.string.initializing);
                    getFileName();
                } else {
                    errorTitle.setText(R.string.downloading);
                    getContent();
                }
            } else {
                setLoadingVisibility(View.GONE);
                videoViewer.setVisibility(View.VISIBLE);
                playVideoLoop();
            }
        }
    }

    public void getContent() {
        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
        codes = dataForward.getString("codes", codes);
        deviceId = dataForward.getString("deviceId", deviceId);
        menuType = dataForward.getString("menuType", menuType);
        setLoadingVisibility(View.VISIBLE);
        errorTitle.setText(R.string.downloading);

        contentInfoInterface = APIClient.getClient().create(ContentInfoInterface.class);
        contentInfoInterface.getContentInfo(Constant.GET_CONTENT + "u=" + deviceId + "&" + "c=" + codes).enqueue(new Callback<ContentInfoResponse>() {
            @Override
            public void onResponse(Call<ContentInfoResponse> call, Response<ContentInfoResponse> response) {
                ContentInfoResponse contentInfoDataVideo = response.body();
                if (contentInfoDataVideo != null && contentInfoDataVideo.getStatus()) {
                    PageDetail[] pageDetail = contentInfoDataVideo.getPage_detail();
                    for (PageDetail detail : pageDetail) {
                        if ("4".equals(detail.getTemplate_id())) {
                            dataContentInterface = APIClient.getClient().create(DataContentInterface.class);
                            dataContentInterface.getDataInfo(Constant.GET_DATA + "u=" + deviceId + "&" + "c=" + detail.getPage_code()).enqueue(new Callback<DataContentResponse>() {
                                @Override
                                public void onResponse(Call<DataContentResponse> call, Response<DataContentResponse> response) {
                                    DataContentResponse answer = response.body();
                                    if (answer != null && answer.getPage_data() != null) {
                                        for (int l = 0; l < answer.getPage_data().length; l++) {
                                            urlsArray = new String[]{answer.getPage_data()[l].getFilename()};
                                            multipleDownload();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<DataContentResponse> call, Throwable t) {
                                    showToast("No Data!\nPlease check your internet connection or call support");
                                }
                            });
                        }
                    }
                } else {
                    showToast("No Data!\nPlease check your internet connection or call support");
                }
            }

            @Override
            public void onFailure(Call<ContentInfoResponse> call, Throwable t) {
                showToast("No Data!\nPlease check your internet connection or call support");
            }
        });
    }

    private String[] fetchVideoFileNames() {
        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
        codes = dataForward.getString("codes", codes);
        String directoryPath = getExternalFilesDir(null) + File.separator + codes;
        File dir = new File(directoryPath);

        if (!dir.exists() || !dir.isDirectory()) {
            Log.e("fetchVideoFileNames", "Directory does not exist or is not a directory: " + dir.getAbsolutePath());
            return new String[0];
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            Log.e("fetchVideoFileNames", "No files found in directory: " + dir.getAbsolutePath());
            return new String[0];
        }

        List<String> videoFileNamesList = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".mp4")) {
                videoFileNamesList.add(file.getName());
            }
        }

        String[] videoFileNamesArray = videoFileNamesList.toArray(new String[0]);
        Log.d("fetchVideoFileNames", "Video file names: " + Arrays.toString(videoFileNamesArray));
        return videoFileNamesArray;
    }

    private void playVideoLoop() {
        String[] videoFileNames = fetchVideoFileNames();
        int videoCount = videoFileNames.length;

        if (videoCount == 0) {
            Log.e(TAG, "playVideoLoop: No videos found to play.");
            return;
        }

        Log.d(TAG, "playVideoLoop: Playing video at index " + currentIndex);

        String directoryPath = getExternalFilesDir(null) + File.separator + codes;
        videoUri = Uri.parse(directoryPath + File.separator + videoFileNames[currentIndex]);
        videoViewer.setVideoURI(videoUri);
        videoViewer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoViewer.start();
            }
        });
        videoViewer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                currentIndex = (currentIndex + 1) % videoCount;
                playVideoLoop();
            }
        });
        videoViewer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "playVideoLoop: Error occurred while playing video. Skipping to next video.");
                currentIndex = (currentIndex + 1) % videoCount;
                playVideoLoop();
                return true; // Returning true indicates that we've handled the error
            }
        });
    }

    private void getFileName() {
        Log.d("getFileName", "Executing getFileName");

        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
        codes = dataForward.getString("codes", codes);
        deviceId = dataForward.getString("deviceId", deviceId);

        contentInfoInterface = APIClient.getClient().create(ContentInfoInterface.class);
        contentInfoInterface.getContentInfo(Constant.GET_CONTENT + "u=" + deviceId + "&" + "c=" + codes).enqueue(new Callback<ContentInfoResponse>() {
            @Override
            public void onResponse(Call<ContentInfoResponse> call, Response<ContentInfoResponse> response) {
                ContentInfoResponse contentInfoDataVideo = response.body();
                if (contentInfoDataVideo != null && contentInfoDataVideo.getStatus()) {
                    List<String> dataFromAPI = new ArrayList<>();
                    for (PageDetail pageDetail : contentInfoDataVideo.getPage_detail()) {
                        if ("4".equals(pageDetail.getTemplate_id())) {
                            dataFromAPI.add(pageDetail.getPage_code());
                        }
                    }

                    String directoryPath = getExternalFilesDir(null) + File.separator + codes;
                    File directory = new File(directoryPath);
                    if (!directory.exists() || !directory.isDirectory()) {
                        Log.e("getFileName", "Directory does not exist or is not a directory: " + directory.getAbsolutePath());
                        return;
                    }

                    File[] files = directory.listFiles();
                    if (files == null) {
                        Log.e("getFileName", "Failed to list files in directory: " + directory.getAbsolutePath());
                        return;
                    }

                    List<String> currentFiles = new ArrayList<>();
                    for (File file : files) {
                        currentFiles.add(file.getName());
                    }

                    DelUrlsArray = currentFiles.toArray(new String[0]);

                    for (String fileName : DelUrlsArray) {
                        if (!dataFromAPI.contains(fileName)) {
                            File fileToDelete = new File(directoryPath + File.separator + fileName);
                            if (fileToDelete.exists() && fileToDelete.isFile()) {
                                if (!fileToDelete.delete()) {
                                    Log.e("getFileName", "Failed to delete file: " + fileToDelete.getAbsolutePath());
                                }
                            }
                        }
                    }

                    for (String pageCode : dataFromAPI) {
                        if (!currentFiles.contains(pageCode)) {
                            dataContentInterface = APIClient.getClient().create(DataContentInterface.class);
                            dataContentInterface.getDataInfo(Constant.GET_DATA + "u=" + deviceId + "&" + "c=" + pageCode).enqueue(new Callback<DataContentResponse>() {
                                @Override
                                public void onResponse(Call<DataContentResponse> call, Response<DataContentResponse> response) {
                                    DataContentResponse answer = response.body();
                                    if (answer != null && answer.getPage_data() != null) {
                                        for (int l = 0; l < answer.getPage_data().length; l++) {
                                            urlsArray = new String[]{answer.getPage_data()[l].getFilename()};
                                            multipleDownload();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<DataContentResponse> call, Throwable t) {
                                    showToast("No Data!\nPlease check your internet connection or call support");
                                }
                            });
                        }
                    }
                } else {
                    showToast("No Data!\nPlease check your internet connection or call support");
                }
            }

            @Override
            public void onFailure(Call<ContentInfoResponse> call, Throwable t) {
                showToast("No Data!\nPlease check your internet connection or call support");
            }
        });
    }

    private void multipleDownload() {
        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
        codes = dataForward.getString("codes", codes);
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        for (String url : urlsArray) {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Download");
            request.setDescription("Downloading File...");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            String input = url;
            input = input.replace("https://sds.par-crm.com/assets/contents/", "");
            String fileName = input;
            request.setDestinationInExternalFilesDir(this, codes, fileName);
            downloadManager.enqueue(request);
        }
    }

    private void showToast(String message) {
        Toast.makeText(VideoPlayerActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(downloadCompleteReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }
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
                        showToast("Incorrect Password!");
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