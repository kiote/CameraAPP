package com.microcave.cameraapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenLockReciever extends BroadcastReceiver {
    public ScreenLockReciever() {
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("Broadcast", "started");

    }
}
