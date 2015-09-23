package com.wish.wishlist.util.sync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseUser;
import com.wish.wishlist.activity.Profile;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jiawen on 15-08-04.
 */

public class PushBroadcastReceiver extends ParsePushBroadcastReceiver {
    final static String TAG = "PushBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        super.onReceive(context, intent);
    }

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Log.e(TAG, "onPushReceive");
        try {
            JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));
            Log.d(TAG, "json: " + json);
            if (json.has("syncUserProfile")) {
                SyncAgent.getInstance().updateProfileFromParse();
            } else if (json.has("syncWishes")) {
                SyncAgent.getInstance().sync();
            }
        } catch (JSONException e) {
            Log.d(TAG, "JSONException: " + e.getMessage());
        }
    }

    @Override
    protected void onPushOpen(Context context, Intent intent) {
        Log.d(TAG, "onPushOpen");
    }
}
