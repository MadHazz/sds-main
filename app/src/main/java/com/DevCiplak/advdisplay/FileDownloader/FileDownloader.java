package com.DevCiplak.advdisplay.FileDownloader;

import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader {

    public static void main(String[] args) {
        String fileUrl = "https://example.com/file.zip"; // Replace with your file URL
        downloadFileWithProgress(fileUrl);
    }

    public static void downloadFileWithProgress(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // Get the file size
            int fileSize = connection.getContentLength();
            System.out.println("File size: " + fileSize + " bytes");

            InputStream input = new BufferedInputStream(url.openStream());
            FileOutputStream output = new FileOutputStream("downloaded_file.zip"); // Change to your desired file path

            byte[] data = new byte[1024];
            int total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
                int progress = (int) ((total * 100) / fileSize);
                long remainingMB = (fileSize - total) / (1024 * 1024); // Remaining MB
                System.out.println("Progress: " + progress + "% | Remaining: " + remainingMB + " MB");
            }

            output.flush();
            output.close();
            input.close();

            System.out.println("Download complete");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
