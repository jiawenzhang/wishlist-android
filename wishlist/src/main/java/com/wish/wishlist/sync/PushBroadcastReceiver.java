package com.wish.wishlist.sync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;
import com.wish.wishlist.friend.FriendManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jiawen on 15-08-04.
 */

public class PushBroadcastReceiver extends ParsePushBroadcastReceiver {
    final static String TAG = "PushBroadcastReceiver";

    final static String SYNC_USER_PROFILE = "syncUserProfile";
    final static String SYNC_WISHES = "syncWishes";
    final static String FRIEND_REQUEST_UPDATE = "friendRequestUpdate";

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
            if (json.has(SYNC_USER_PROFILE)) {
                SyncAgent.getInstance().updateProfileFromParse();
            } else if (json.has(SYNC_WISHES)) {
                SyncAgent.getInstance().sync();
            } else if (json.has(FRIEND_REQUEST_UPDATE)) {
                Log.d(TAG, FRIEND_REQUEST_UPDATE);
                int status = json.getInt(FRIEND_REQUEST_UPDATE);
                if (status == FriendManager.REQUESTED) {
                    Log.d(TAG, "requested");
                    FriendManager.getInstance().fetchFriendRequestFromNetwork();
                } else if (status == FriendManager.ACCEPTED) {
                    Log.d(TAG, "accepted");
                    FriendManager.getInstance().fetchFriendsFromNetwork();
                } else if (status == FriendManager.REJECTED) {
                    Log.d(TAG, "rejected");
                    FriendManager.getInstance().fetchFriendRequestFromNetwork();
                }
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
