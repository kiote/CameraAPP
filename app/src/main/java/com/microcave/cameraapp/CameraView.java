package com.microcave.cameraapp;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    static boolean rotationInProgress = false;
    public Thread thread;
    public boolean isPreviewRunning = false;
    boolean show = false;
    Context context;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    public CameraView(Context c, Camera camera) {
        super(c);
        context = c;
        mCamera = camera;
        mCamera.setDisplayOrientation(0);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            if (mCamera == null) {
                mCamera = Camera.open();
            }
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceCreated " + e.getMessage());
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        if (isPreviewRunning) {
            mCamera.stopPreview();
        }
        Camera.Parameters parameters = mCamera.getParameters();
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if (display.getRotation() == Surface.ROTATION_0) {
            rotationInProgress = true;
            parameters.setPreviewSize(height, width);
            mCamera.setDisplayOrientation(90);
        }
        if (display.getRotation() == Surface.ROTATION_90) {
            rotationInProgress = true;
            parameters.setPreviewSize(width, height);
            mCamera.setDisplayOrientation(0);
        }
        if (display.getRotation() == Surface.ROTATION_270) {
            rotationInProgress = true;
            parameters.setPreviewSize(width, height);
            mCamera.setDisplayOrientation(180);
        }
        rotationInProgress = false;
        Log.e("rotation value ", "" + rotationInProgress);
        start();
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.e("Camera Status", "Destroy called");
        mCamera.release();
    }
    void pause() {
        mCamera.stopPreview();
        show = false;
    }
    void start() {
        show = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    if (mHolder.getSurface() == null)
                        return;
                    try {
                        mCamera.stopPreview();
                    } catch (Exception e) {
                    }
                    try {
                        mCamera.setPreviewDisplay(mHolder);
                        mCamera.startPreview();
                        isPreviewRunning = true;
                    } catch (IOException e) {
                        Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
                    }
                }
            }
        });
        thread.start();
    }
}