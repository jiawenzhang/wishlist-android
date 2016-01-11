package com.wish.wishlist.sync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.friend.FriendManager;
import com.wish.wishlist.friend.FriendRequestActivity;
import com.wish.wishlist.friend.FriendsActivity;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.VisibleActivityTracker;

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

                    if (FriendRequestActivity.class.isInstance(VisibleActivityTracker.getInstance().visibleActivity())) {
                        // if FriendRequestActivity is visible, don't show the notification icon, as user already sees the update
                        return;
                    }
                    EventBus.getInstance().post(new com.wish.wishlist.event.ShowNewFriendRequestNotification());
                    Options.ShowNewFriendRequestNotification showNewFriendRequest = new Options.ShowNewFriendRequestNotification(1);
                    showNewFriendRequest.save();

                    if (FriendsActivity.class.isInstance(VisibleActivityTracker.getInstance().visibleActivity())) {
                        // if FriendsActivity is visible, don't show the notification icon, as user already sees the update
                        Log.e(TAG, "FriendsActivity visible");
                        return;
                    }
                    EventBus.getInstance().post(new com.wish.wishlist.event.ShowNewFriendNotification());
                    Options.ShowNewFriendNotification showNewFriends = new Options.ShowNewFriendNotification(1);
                    showNewFriends.save();
                } else if (status == FriendManager.ACCEPTED) {
                    Log.d(TAG, "accepted");
                    FriendManager.getInstance().fetchFriendsFromNetwork();

                    if (FriendsActivity.class.isInstance(VisibleActivityTracker.getInstance().visibleActivity()) ||
                            FriendRequestActivity.class.isInstance(VisibleActivityTracker.getInstance().visibleActivity())) {
                        // if FriendsActivity or FriendRequestActivity is visible, don't show the notification icon, as user already sees the update
                        return;
                    }
                    EventBus.getInstance().post(new com.wish.wishlist.event.ShowNewFriendNotification());
                    Options.ShowNewFriendNotification showNewFriends = new Options.ShowNewFriendNotification(1);
                    showNewFriends.save();
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
