package com.wish.wishlist.util.sync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

/**
 * Created by jiawen on 15-08-04.
 */

public class PushBroadcastReceiver extends ParsePushBroadcastReceiver {
    final static String TAG = "PushBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Log.d(TAG, "onPushReceive");
    }

    @Override
    protected void onPushOpen(Context context, Intent intent) {
        Log.d(TAG, "onPushOpen");
    }
}
