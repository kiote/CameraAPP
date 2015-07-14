package com.microcave.cameraapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity {
    public boolean serviceResult;
    public Boolean close = true;
    public Context context = this;            //used for reference in inner classes
    public Timer timer = new Timer();
    public Timer secondTimer = new Timer();
    public TimerTask clickPhoto = new TakePhoto();
    protected ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d("service ", "onServiceConnected");
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("service ", "onServiceDisconnected");
        }
    };
    OrientationEventListener listner;
    ArrayList<String> ImagePath = new ArrayList<String>();
    int count = 10;
    ImageView imageview;
    Intent intent;
    Boolean isConnected = false;
    Thread cameraThread = new CameraIntializer();
    Boolean runThread = true;
    ChangePhoto changeImage = new ChangePhoto();
    private Camera mCamera = null;
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            SaveImageTask saveImageTask = new SaveImageTask();
            saveImageTask.setContext(context);
            saveImageTask.execute(data);
            reset();
            Log.d("TAG", "onPictureTaken - jpeg");
        }
    };
    private CameraView mCameraView = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        Log.e("activity ", "started");
        serviceResult = true;
        intent = new Intent(this, BackgroundService.class);
        setContentView(R.layout.activity_main);
        imageview = (ImageView) findViewById(R.id.imageView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e) {
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }
        if (mCamera != null) {
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
            camera_view.addView(mCameraView);//add the SurfaceView to the layout
        }
        ImageButton imgClose = (ImageButton) findViewById(R.id.imgClose);
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // System.exit(0);
                finish();
                try {
                    close = false;
                    Intent newintent = new Intent(context, BackgroundService.class);
                    newintent.putExtra("close", false);
                    context.stopService(newintent);
                    context.unbindService(mServerConn);
                } catch (Exception e) {
                    Log.e("Camera APP", "Service is not registered yet");
                }
            }
        });
        listner = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (mCamera == null) {
                    mCamera = Camera.open();
                }
                if (orientation == 180 || orientation == 90 || orientation == 270) {
                    CameraView.rotationInProgress = true;
                    Log.e("Camera Status", orientation + "" + CameraView.rotationInProgress);
                }
            }
        };
        listner.enable();
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.e("Camera Status", "Pause called");
        mCamera.release();
        mCameraView.getHolder().removeCallback(mCameraView);
        cameraThread.interrupt();
        if (close) {
            this.bindService(intent, mServerConn, BIND_AUTO_CREATE);
            this.startService(intent);
        }
        isConnected = true;
        serviceResult = false;
        runThread = false;
        finish();
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e("Camera Status", "resume called");
        if (isConnected) {
            mCamera = null;
            Log.e("is connected ", "true");
            Intent newintent = new Intent(this, BackgroundService.class);
            this.stopService(newintent);
            this.unbindService(mServerConn);
            serviceResult = false;        //-----
            try {
                Thread thread = new CameraIntializer();
                thread.start();
                synchronized (thread) {
                    thread.wait(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.finish();
        }
        if (serviceResult) {
            Log.e("service result", "true");
            runThread = true;
            cameraThread.start();
        }
    }
    void reset() {
        mCamera.startPreview();
    }
    void delete() {
        try {
            for (int i = 0; i < ImagePath.size(); i++) {
                String image = ImagePath.get(i);
                Log.d("delte Image path", image);
                File file = new File(image);
                file.delete();
            }
            Toast.makeText(this, "All Images are deleted ", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.getMessage();
            Toast.makeText(this, "nothing to delte ", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Intent intent = new Intent(this, UploadingService.class);
            intent.putExtra("path", "");
            startService(intent);
            intent.putExtra("close", false);
            this.unbindService(mServerConn);
            this.stopService(intent);
        } catch (Exception e) {
            Log.e("Camera app ", "service not registered");
        }
        if (mCamera != null) {
            mCamera.release();
            Log.e("Main activty", "Destroy called;");
        }
    }
    private class CameraIntializer extends Thread {
        @Override
        public void run() {
            if (runThread) {
                if (mCamera == null) {
                    mCamera = Camera.open();
                }
                mCameraView.getHolder().addCallback(mCameraView);
                timer = new Timer();
                clickPhoto = new TakePhoto();
                changeImage = new ChangePhoto();
                secondTimer = new Timer();
                count = 10;
                timer.schedule(changeImage, 0, 1000);//Update text every second
            }
        }
    }
    public class ChangePhoto extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (count > 0) {
                        try {
                            int resourceDrwable = getResources().getIdentifier("a" + count, "drawable", getPackageName());
                            imageview.setVisibility(View.VISIBLE);
                            imageview.setBackgroundResource(resourceDrwable);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        count--;
                    } else {
                        if (count == 0) {
                            imageview.setVisibility(View.GONE);
                            timer.cancel();
                            secondTimer = new Timer();
                            clickPhoto = new TakePhoto();
                            secondTimer.schedule(clickPhoto, 0, 10000);//10 =2
                        }
                    }
                }
            });

        }
    }
    public class TakePhoto extends TimerTask {
        @Override
        public void run() {
            if (!CameraView.rotationInProgress) {
                if (serviceResult)                   //checks activity is on top or not
                {
                    mCamera.takePicture(null, null, jpegCallback);
                }
            }
            if (count == 5) {
                secondTimer.cancel();
                count = 10;
                Log.e("update display", "");
                timer = new Timer();
                changeImage = new ChangePhoto();
                timer.schedule(changeImage, 0, 1000);//Update text every second
            }
            count++;
        }
    }
}