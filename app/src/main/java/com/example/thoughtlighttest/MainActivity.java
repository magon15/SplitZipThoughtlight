package com.example.thoughtlighttest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    public static final int REQUEST_VIDEO = 0;
    public static final int REQUEST_READ = 1;
    public static final int REQUEST_WRITE = 2;

    private String videoPath;
    private TextView textFileSelected;
    private Button buttonSelectFile;
    private Button buttonCompress;
    private ProgressBar progressLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textFileSelected = findViewById(R.id.textVideoLocation);
        buttonSelectFile = findViewById(R.id.buttonSelectFile);
        buttonCompress = findViewById(R.id.buttonCompress);
        progressLoading = findViewById(R.id.progressLoading);

        String readPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        int checkValRead = checkCallingOrSelfPermission(readPermission);

        if (checkValRead != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },REQUEST_READ);
        }

        String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int checkValWrite = checkCallingOrSelfPermission(writePermission);

        if (checkValWrite != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },REQUEST_WRITE);
        }

        buttonSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayGalleryIntentForImage();
            }
        });




        buttonCompress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!videoPath.equals("")) {

                    final String destinationPath = getExternalFilesDir(null).getPath() + "/ThoughtLight Test/";
                    ZipManager.zip(MainActivity.this,videoPath,destinationPath + "test.zip",progressLoading);

                    final String zipFilePath = destinationPath + "test.zip";

                    progressLoading.setVisibility(View.VISIBLE);
                    Handler handler = new Handler(Looper.myLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            deleteRecursive(new File(destinationPath));

                            File destinationFile = new File(destinationPath);
                            if(!destinationFile.exists())
                                destinationFile.mkdirs();

                            File fileToZip = new File(videoPath);
                            double fileZipSize = (double) fileToZip.length()/ 1024.0 /1024.0;

                            DateTime timeStarted = new DateTime(DateTimeZone.forTimeZone(TimeZone.getDefault()));

                            ZipFile zipFile = new ZipFile(zipFilePath);
                            ZipParameters parameters = new ZipParameters();

                            // set compression method to store compression
                            parameters.setCompressionMethod(CompressionMethod.DEFLATE);

                            // Set the compression level. This value has to be in between 0 to 9
                            parameters.setCompressionLevel(CompressionLevel.FASTEST);

                            File zipFileToSplit = new File(videoPath);
                            ArrayList<File> filesSplit = new ArrayList<>();
                            filesSplit.add(zipFileToSplit);
                            try {
                                zipFile.createSplitZipFile(filesSplit, parameters, true, 10485760);
                            } catch (ZipException e) {
                                e.printStackTrace();
                            }

                            ProgressMonitor progressMonitor = zipFile.getProgressMonitor();

                            while (progressMonitor.getState() == ProgressMonitor.State.BUSY) {
                                Log.d(TAG,"Percent Done: " + progressMonitor.getPercentDone());
                            }

                            DateTime timeEnded = new DateTime(DateTimeZone.forTimeZone(TimeZone.getDefault()));

                            int numMinutes = Minutes.minutesBetween(timeStarted,timeEnded).getMinutes();
                            int numSeconds = Seconds.secondsBetween(timeStarted,timeEnded).getSeconds() - (numMinutes * 60);


                            textFileSelected.setText("It took " + numMinutes + " minute/s & " + numSeconds +
                                    " seconds to convert a " + fileZipSize + " MB video.\n\nLocation: " + zipFilePath);

                            progressLoading.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public void displayGalleryIntentForImage() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            if(requestCode==REQUEST_VIDEO){
                videoPath = getPath(this,data.getData());
                textFileSelected.setText(videoPath);
            }
        }
    }

    public static String getPath(final Context context, final Uri uri) {

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}