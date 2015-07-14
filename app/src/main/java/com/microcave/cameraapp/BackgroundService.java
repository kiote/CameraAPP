package com.microcave.cameraapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundService extends Service {
    public Timer timer = new Timer();
    public Timer secondTimer = new Timer();
    public TimerTask clickPhoto = new TakePhoto();
    BroadcastReceiver mReceiver;
    ChangePhoto changeImage = new ChangePhoto();
    int count = 5;
    SurfaceTexture surfaceTexture;
    Context context;
    private Camera mCamera;
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            SaveImageTask saveImageTask = new SaveImageTask();
            saveImageTask.setContext(context);
            saveImageTask.execute(data);
            reset();
            Log.d("TAG", "onPictureTaken - jpeg");
        }
    };
    private Camera.Parameters parameters;
    public BackgroundService() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        context = getApplicationContext();
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        mReceiver = new ScreenLockReciever();
        registerReceiver(mReceiver, filter);
        mCamera = Camera.open();
        surfaceTexture = new SurfaceTexture(10);
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e1) {
            Log.e("Version.APP_ID", e1.getMessage());
        }
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        params.setPictureFormat(ImageFormat.JPEG);
        mCamera.setParameters(params);
        mCamera.startPreview();
        mCamera.setPreviewCallbackWithBuffer(null);
        timer = new Timer();
        clickPhoto = new TakePhoto();
        changeImage = new ChangePhoto();
        secondTimer = new Timer();
        count = 10;
        timer.schedule(changeImage, 0, 1000);//Update text every second
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("Camera Status", "service destry");
    }
    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(mReceiver);
        secondTimer.cancel();
        timer.cancel();
        clickPhoto.cancel();
        changeImage.cancel();
        mCamera.release();
        if (intent.getBooleanExtra("close", true)) {
            Log.e("service ", "called");
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(i);
        }
        Log.e("service ", "unbind  called");
        return super.onUnbind(intent);
    }
    void reset() {
        mCamera.startPreview();
    }
    public class ChangePhoto extends TimerTask {
        @Override
        public void run() {
            count--;
            Log.e("count", "" + count);
            if (count == 0) {
                timer.cancel();
                secondTimer = new Timer();
                clickPhoto = new TakePhoto();
                Log.e("count", "0");
                secondTimer.schedule(clickPhoto, 0, 10000);//10 =2
            }
        }
    }
    public class TakePhoto extends TimerTask {
        @Override
        public void run() {
            Log.e("camera ", "pic" + count);
            mCamera.takePicture(null, null, jpegCallback);
            if (count == 5) {
                secondTimer.cancel();
                clickPhoto.cancel();
                count = 10;
                timer = new Timer();
                changeImage = new ChangePhoto();
                timer.schedule(changeImage, 0, 1000);//Update text every second
            }
            count++;
        }
    }
}
