package com.microcave.cameraapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SaveImageTask extends AsyncTask<byte[], Void, Void> {
    Context context;
    private String folderName = "Camera Data";
    @Override
    protected Void doInBackground(byte[]... data) {
        FileOutputStream outStream = null;
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/" + folderName);
            dir.mkdirs();
            String fileName = String.format("%d.jpg", System.currentTimeMillis());
            File outFile = new File(dir, fileName);
            outStream = new FileOutputStream(outFile);
            outStream.write(data[0]);
            outStream.flush();
            outStream.close();
            Log.e("TAG", "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());
            refreshGallery(outFile);
            //============ file is succesfully saved ==========================
            Intent intent = new Intent(context, UploadingService.class);
            intent.putExtra("path", outFile.getAbsolutePath());
            context.startService(intent);
            //===================================================================
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }
    public void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        context.sendBroadcast(mediaScanIntent);
    }
    public void setContext(Context c) {
        context = c;
    }
}