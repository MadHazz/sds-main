//package com.DevCiplak.advdisplay;
//
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.constraintlayout.widget.ConstraintLayout;
//
//import android.app.DownloadManager;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.IntentFilter;
//import android.content.SharedPreferences;
//import android.media.MediaPlayer;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.PowerManager;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.EditText;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.content.Intent;
//import android.widget.VideoView;
//
//import com.DevCiplak.advdisplay.Model.ContentInfoResponse;
//import com.DevCiplak.advdisplay.Model.DataContentResponse;
//import com.DevCiplak.advdisplay.Model.PageDetail;
//import com.DevCiplak.advdisplay.Retrofit.APIClient;
//import com.DevCiplak.advdisplay.RetrofitInterfaces.ContentInfoInterface;
//import com.DevCiplak.advdisplay.RetrofitInterfaces.DataContentInterface;
//import com.DevCiplak.advdisplay.constant.Constant;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class Temporary extends AppCompatActivity  {
//
//    private static final String TAG = "VideoPlayerActivity";
//    VideoView videoViewer;
//    ConstraintLayout loadingBack, messageBox;
//    TextView errorTitle;
//    ProgressBar progressBar;
//    SharedPreferences dataForward;
//    String codes, menuType, deviceId;
//    String[] urlsArray, DelUrlsArray;
//    DataContentInterface dataContentInterface;
//    ContentInfoInterface contentInfoInterface;
//    Uri videoUri;
//    int currentIndex = 0;
//    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
//                // Handle download completion here
//                final Handler handler = new Handler();
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        loadingBack.setVisibility(View.GONE);
//                        messageBox.setVisibility(View.GONE);
//                        errorTitle.setVisibility(View.GONE);
//                        progressBar.setVisibility(View.GONE);
//                        videoViewer.setVisibility(View.VISIBLE);
//                        playVideoLoop();
//                    }
//                }, 30000);
//                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
//            } else {
//                showToast("Download failed!\nPlease make sure you have a strong internet connection!");
//            }
//        }
//    };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                "MyApp::MyWakelockTag");
//        wakeLock.acquire();
//        setContentView(R.layout.activity_video_player);
//
//        videoViewer = findViewById(R.id.videoView);
//        loadingBack = findViewById(R.id.loadingBack);
//        messageBox = findViewById(R.id.messageBox);
//        errorTitle = findViewById(R.id.errorTitle);
//        progressBar = findViewById(R.id.progressBar);
//
//        //Set Visibility for views
//        loadingBack.setVisibility(View.VISIBLE);
//        messageBox.setVisibility(View.VISIBLE);
//        errorTitle.setVisibility(View.VISIBLE);
//        progressBar.setVisibility(View.VISIBLE);
//
//        checkFile();
//
//        Handler handler = new Handler();
//
//        Runnable checkFileRunnable = new Runnable() {
//            @Override
//            public void run() {
//                checkFile();
//                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(5));
////                handler.postDelayed(this, TimeUnit.MINUTES.toMillis(30));
//            }
//        };
//
//        handler.post(checkFileRunnable);
//
//        registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
//    }
//
//    @Override
//    protected void onRestart() {
//        super.onRestart();
////        playVideoLoop();
//        videoViewer.resume();
//    }
//    public void resetVideoView() {
//        Log.d("resetVideoView", "Video reset");
//        videoViewer.pause();
//        videoViewer.stopPlayback();
//        videoViewer.suspend();
//        videoViewer.setOnPreparedListener(null);
//        videoViewer.setVideoURI(null);
//        currentIndex = 0;
//        videoViewer.seekTo(0);
//    }
//    public boolean isConnected() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
//        return networkInfo != null && networkInfo.isConnectedOrConnecting();
//    }
//
//    public void checkFile() {
//        resetVideoView();
//        videoViewer.setVisibility(View.GONE);
//
//        Log.d("checkFile", "5 minutes pass");
//        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
//        codes = dataForward.getString("codes", codes);
//        String directoryPath = getExternalFilesDir(null) + File.separator + codes;
//        File dir = new File(directoryPath);
//        File[] children = dir.listFiles();
//
//        if (children != null) {
//            if (isConnected()) {
//                if (children.length > 0) {
//                    loadingBack.setVisibility(View.VISIBLE);
//                    messageBox.setVisibility(View.VISIBLE);
//                    errorTitle.setVisibility(View.VISIBLE);
//                    progressBar.setVisibility(View.VISIBLE);
//                    errorTitle.setText(R.string.initializing);
//                    getFileName();
//                } else {
//                    errorTitle.setText(R.string.downloading);
//                    getContent();
//                }
//            } else {
//                loadingBack.setVisibility(View.GONE);
//                messageBox.setVisibility(View.GONE);
//                errorTitle.setVisibility(View.GONE);
//                progressBar.setVisibility(View.GONE);
//                videoViewer.setVisibility(View.VISIBLE);
//                playVideoLoop();
//            }
//        }
//    }
//    public void getContent() {
//        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
//        codes = dataForward.getString("codes", codes);
//        deviceId = dataForward.getString("deviceId", deviceId);
//        menuType = dataForward.getString("menuType", menuType);
//        loadingBack.setVisibility(View.VISIBLE);
//        messageBox.setVisibility(View.VISIBLE);
//        errorTitle.setVisibility(View.VISIBLE);
//        progressBar.setVisibility(View.VISIBLE);
//        errorTitle.setText(R.string.downloading);
//        contentInfoInterface = APIClient.getClient().create(ContentInfoInterface.class);
//        contentInfoInterface.getContentInfo(Constant.GET_CONTENT + "u=" + deviceId + "&" + "c=" + codes).enqueue(new Callback<ContentInfoResponse>() {
//            @Override
//            public void onResponse(Call<ContentInfoResponse> call, Response<ContentInfoResponse> response) {
//                ContentInfoResponse contentInfoDataVideo = response.body();
//                if (contentInfoDataVideo != null) {
//                    if (contentInfoDataVideo.getStatus().equals(true)) {
//                        PageDetail[] pageDetail = contentInfoDataVideo.getPage_detail();
//                        for (int k = 0; k < pageDetail.length; k++) {
//                            String templateID = pageDetail[k].getTemplate_id();
//                            if (templateID.equals("4")) {
//                                dataContentInterface = APIClient.getClient().create(DataContentInterface.class);
//                                dataContentInterface.getDataInfo(Constant.GET_DATA + "u=" + deviceId + "&" + "c=" + contentInfoDataVideo.getPage_detail()[k].getPage_code()).enqueue(new Callback<DataContentResponse>() {
//                                    @Override
//                                    public void onResponse(Call<DataContentResponse> call, Response<DataContentResponse> response) {
//                                        DataContentResponse answer = response.body();
//                                        for (int l = 0; l < answer.getPage_data().length; l++) {
//                                            urlsArray = new String[]{answer.getPage_data()[l].getFilename()};
//                                            multipleDownload();
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onFailure(Call<DataContentResponse> call, Throwable t) {
//                                        showToast("No Data!\nPlease check your internet connection or call support");
//                                    }
//                                });
//                            }
//                        }
//                    } else {
//                        showToast("No Data!\nPlease check your internet connection or call support");
//                    }
//                } else {
//                    showToast("No Data!\nPlease check your internet connection or call support");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ContentInfoResponse> call, Throwable t) {
//                showToast("No Data!\nPlease check your internet connection or call support");
//            }
//        });
//    }
//
//    private String[] fetchVideoFileNames() {
//        // Construct the correct directory path
//        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
//        codes = dataForward.getString("codes", codes);
//        String directoryPath = getExternalFilesDir(null) + File.separator + codes;
//        // Create a File object representing the directory
//        File dir = new File(directoryPath);
//        // Check if the directory exists and is a directory
//        if (!dir.exists() || !dir.isDirectory()) {
//            Log.e("fetchVideoFileNames", "Directory does not exist or is not a directory: " + dir.getAbsolutePath());
//            return new String[0];
//        }
//        // List files in the directory
//        File[] files = dir.listFiles();
//        // Check if there are files in the directory
//        if (files == null && files.length == 0) {
//            Log.e("fetchVideoFileNames", "No files found in directory: " + dir.getAbsolutePath());
//            return new String[0];
//        }
//        // Filter out non-video files and sort by creation time
//        List<String> videoFileNamesList = new ArrayList<>();
//        for (File file : files) {
//            if (file.isFile() && file.getName().endsWith(".mp4")) {
//                videoFileNamesList.add(file.getName());
//            }
//        }
//        // Convert list to array
//        String[] videoFileNamesArray = videoFileNamesList.toArray(new String[0]);
//        Log.d("fetchVideoFileNames", "Video file names: " + Arrays.toString(videoFileNamesArray));
//        return videoFileNamesArray;
//    }
//
//    private void playVideoLoop() {
//        String[] videoFileNames = fetchVideoFileNames();
//        int videoCount = videoFileNames.length;
//        if (videoCount == 0) {
//            showToast("There are no videos available. Please try again.");
//            return;
//        }
//        if (currentIndex >= videoCount) {
//            // Reset index to loop back to the beginning
//            currentIndex = 0;
//        }
//        // Construct the correct directory path
//        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
//        codes = dataForward.getString("codes", codes);
//        String directoryPath = getExternalFilesDir(null) + File.separator + codes;
//        // Create a File object representing the directory
//        File dir = new File(directoryPath);
//        // Check if the directory exists
//        if (!dir.exists() || !dir.isDirectory()) {
//            showToast("Directory not found: " + dir.getAbsolutePath());
//            return;
//        }
//        // Check if currentIndex is within bounds
//        if (currentIndex < 0 || currentIndex >= videoCount) {
//            showToast("Invalid currentIndex: " + currentIndex);
//            return;
//        }
//        File videoFile = new File(dir, videoFileNames[currentIndex]);
//        // Check if the video file exists
//        if (!videoFile.exists()) {
//            showToast("Video file not found: " + videoFile.getAbsolutePath());
//            return;
//        }
//        videoUri = Uri.fromFile(videoFile);
//        videoViewer.setVideoURI(videoUri);
//        videoViewer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mediaPlayer) {
//                // Play the next video when current video completes playback
//                currentIndex++;
//                playVideoLoop();
//            }
//        });
//        videoViewer.start();
//    }
//
//    public void getFileName() {
//        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
//        codes = dataForward.getString("codes", codes);
//        deviceId = dataForward.getString("deviceId", deviceId);
//        menuType = dataForward.getString("menuType", menuType);
//        contentInfoInterface = APIClient.getClient().create(ContentInfoInterface.class);
//        contentInfoInterface.getContentInfo(Constant.GET_CONTENT + "u=" + deviceId + "&" + "c=" + codes).enqueue(new Callback<ContentInfoResponse>() {
//            @Override
//            public void onResponse(Call<ContentInfoResponse> call, Response<ContentInfoResponse> response) {
//                ContentInfoResponse contentInfoDataVideo = response.body();
//                if (contentInfoDataVideo != null) {
//                    if (contentInfoDataVideo.getStatus().equals(true)) {
//                        PageDetail[] pageDetail = contentInfoDataVideo.getPage_detail();
//                        for (int k = 0; k < pageDetail.length; k++) {
//                            String templateID = pageDetail[k].getTemplate_id();
//                            if (templateID.equals("4")) {
//                                dataContentInterface = APIClient.getClient().create(DataContentInterface.class);
//                                dataContentInterface.getDataInfo(Constant.GET_DATA + "u=" + deviceId + "&" + "c=" + contentInfoDataVideo.getPage_detail()[k].getPage_code()).enqueue(new Callback<DataContentResponse>() {
//                                    @Override
//                                    public void onResponse(Call<DataContentResponse> call, Response<DataContentResponse> response) {
//                                        DataContentResponse answer = response.body();
//                                        for (int l = 0; l < answer.getPage_data().length; l++) {
//                                            DelUrlsArray = new String[]{answer.getPage_data()[l].getFilename()};
//                                            deleteVideo();
//                                            reDownload();
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onFailure(Call<DataContentResponse> call, Throwable t) {
//                                        showToast("No Data!\nPlease check your internet connection or call support");
//                                    }
//                                });
//                            }
//                        }
//                    } else {
//                        showToast("No Data!\nPlease check your internet connection or call support");
//                    }
//                } else {
//                    showToast("No Data!\nPlease check your internet connection or call support");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ContentInfoResponse> call, Throwable t) {
//                showToast("No Data!\nPlease check your internet connection or call support");
//            }
//        });
//    }
//
//    private void deleteVideo() {
//        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
//        codes = dataForward.getString("codes", codes);
//        String directoryPath = getExternalFilesDir(null) + File.separator + codes;
//
//        File dir = new File(directoryPath);
//        if (!dir.exists() || !dir.isDirectory()) {
//            Log.e(TAG, "Directory does not exist or is not a directory: " + dir.getAbsolutePath());
//            return;
//        }
//
//        String[] videoFileNames = dir.list();
//
//        // Delete all files in the directory
//        if (videoFileNames != null) {
//            for (String videoFileName : videoFileNames) {
//                File fileToDelete = new File(dir, videoFileName);
//                boolean deleted = fileToDelete.delete();
//
//                if (deleted) {
//                    Log.d(TAG, "Video file deleted successfully: " + fileToDelete.getAbsolutePath());
//                } else {
//                    Log.e(TAG, "Failed to delete video file: " + fileToDelete.getAbsolutePath());
//                }
//            }
//        } else {
//            Log.d(TAG, "No video files found in directory: " + directoryPath);
//        }
//    }
//
//    public void multipleDownload() {
//        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//        for (String url : urlsArray) {
//            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
//            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
//            request.setTitle("Download");
//            request.setDescription("Downloading File...");
//            request.allowScanningByMediaScanner();
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//            String input = url;
//            input = input.replace("https://sds.par-crm.com/assets/contents/", "");
//            String fileName = input;
//            request.setDestinationInExternalFilesDir(this, codes, fileName);
//            long downloadId = manager.enqueue(request);
//            Log.d("DownloadManager", "Download enqueued with ID: " + downloadId);
//        }
//    }
//
//    public void reDownload() {
//        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//        for (String url : DelUrlsArray) {
//            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
//            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
//            request.setTitle("Download");
//            request.setDescription("Downloading File...");
//            request.allowScanningByMediaScanner();
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//            String input = url;
//            input = input.replace("https://sds.par-crm.com/assets/contents/", "");
//            String fileName = input;
//            request.setDestinationInExternalFilesDir(this, codes, fileName);
//            long downloadId = manager.enqueue(request);
//            Log.d("DownloadManager", "Download enqueued with ID: " + downloadId);
//        }
//    }
//
//    @Override
//    public void onBackPressed() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        LayoutInflater inflater = getLayoutInflater();
//        View dialogView = inflater.inflate(R.layout.dialog_password, null);
//        final EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
//        builder.setView(dialogView)
//                .setTitle("Warning!")
//                .setMessage("All data will be cleared if you proceed!\nAre you sure you want to go back?")
//                .setPositiveButton("Yes", (DialogInterface.OnClickListener) (dialog, which) -> {
//                    String enteredPassword = passwordEditText.getText().toString();
//                    if (enteredPassword.equals("1234")) {
//                        dataForward = getSharedPreferences("advForward", MODE_PRIVATE);
//                        SharedPreferences.Editor keys = dataForward.edit();
//                        keys.remove("codes").apply();
//                        keys.remove("deviceId").apply();
//                        keys.remove("menuType").apply();
//                        super.onBackPressed();
//                    } else {
//                        showToast("Incorrect Password!");
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        dialogInterface.cancel();
//                    }
//                });
//        AlertDialog alertDialog = builder.create();
//        alertDialog.show();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // Unregister the BroadcastReceiver to avoid memory leaks
//        unregisterReceiver(downloadCompleteReceiver);
//    }
//
//    private void showToast(String message) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//    }
//
//}
