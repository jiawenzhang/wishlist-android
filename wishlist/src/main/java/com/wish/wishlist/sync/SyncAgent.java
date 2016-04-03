package com.wish.wishlist.sync;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.ProfileChangeEvent;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.util.ProfileUtil;
import com.wish.wishlist.util.StringUtil;

import java.util.Date;

/**
 * Created by jiawen on 15-07-11.
 */
public class SyncAgent implements
        DownloadTask.DownloadTaskDoneListener,
        UploadTask.UploadTaskDoneListener {
    private static SyncAgent instance = null;
    private OnSyncWishChangedListener mSyncWishChangedListener;
    private OnDownloadWishDoneListener mDownloadWishDoneListener;
    private Date mSyncedTime;
    private DownloadTask mDownloadTask = new DownloadTask();
    private UploadTask mUploadTask = new UploadTask();

    private boolean mDownloading = false;
    private boolean mSyncing = false;
    private boolean mScheduleToSync = false;
    private static String TAG = "SyncAgent";
    public static String LAST_SYNCED_TIME = "lastSyncedTime";

    /* listener */
    public interface OnSyncWishChangedListener {
        void onSyncWishChanged();
    }

    public interface OnDownloadWishDoneListener {
        void onDownloadWishDone(boolean success);
    }

    public void registerListener(Activity activity) {
        try {
            mSyncWishChangedListener = (OnSyncWishChangedListener) activity;
            mDownloadWishDoneListener = (OnDownloadWishDoneListener) activity;
        }
        catch (final ClassCastException e) {
            Log.e(TAG, "fail to registerListener");
            throw new ClassCastException(activity.toString() + " must implement OnSyncWishChanged/OnDownloadWishDone");
        }
    }
    /* listener */


    public static SyncAgent getInstance() {
        if (instance == null) {
             instance = new SyncAgent();
        }
        return instance;
    }

    private SyncAgent() {
        mDownloadTask.registerListener(this);
        mUploadTask.registerListener(this);
    }

    public boolean downloading() {
        return mDownloading;
    }

    // call sync on app start up
    // how does parse trigger sync on the client? push notification?
    public void sync() {
        Log.d(TAG, "sync");
        if (!WishlistApplication.getAppContext().getResources().getBoolean(R.bool.enable_account)) {
            return;
        }

        if (ParseUser.getCurrentUser() == null) {
            Log.d(TAG, "user not login, sync is disabled ");
            return;
        }

        if (mSyncing) {
            // if we are in the process of syncing, run sync again after the current sync is finished
            Log.d(TAG, "mSync true, schedule sync");
            mScheduleToSync = true;
            return;
        }

        if (!NetworkHelper.getInstance().isNetworkAvailable()) {
            Log.d(TAG, "no network, sync is not started");
            // Fixme: shall we attempt sync later?
            return;
        }

        mSyncing = true;
        mDownloading = true;

        // start the sync process with downloadTask, then uploadTask
        // the subsequent task will be started when the previous task is done
        // if error occurs in any of the tasks, syncFailed will be called and LAST_SYNCED_TIME is not updated
        mDownloadTask.run();
    }

    @Override
    public void downloadTaskDone(boolean success, Date syncedTime) {
        mDownloading = false;
        mSyncedTime = syncedTime;

        if (mDownloadWishDoneListener != null) {
            mDownloadWishDoneListener.onDownloadWishDone(success);
        }

        // we have finished downloading items, notify list/grid view to refresh
        if (mSyncWishChangedListener != null) {
            mSyncWishChangedListener.onSyncWishChanged();
        }

        if (success) {
            mUploadTask.run(mSyncedTime);
        } else {
            syncFailed();
        }
    }

    @Override
    public void uploadTaskDone(boolean success, Date syncedTime) {
        mSyncedTime = syncedTime;
        if (success) {
            syncDone();
        } else {
            syncFailed();
        }
    }

    private void syncDone() {
        // both download and upload is done, sync is finished
        Log.d(TAG, "sync finished at " + mSyncedTime.getTime() + " " + StringUtil.UTCDate(mSyncedTime));
        // all items are downloaded, save the last synced down time
        final SharedPreferences sharedPref = WishlistApplication.getAppContext().getSharedPreferences(WishlistApplication.getAppContext().getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(LAST_SYNCED_TIME, mSyncedTime.getTime());
        editor.commit();

        mSyncing = false;
        if (mScheduleToSync) {
            mScheduleToSync = false;
            sync();
        }
    }

    private void syncFailed() {
        Log.e(TAG, "syncFailed");

        // when sync fails (most likely network unavailable), let's just do nothing
        // util the next sync is triggered
        mSyncing = false;
        mDownloading = false;
        mScheduleToSync = false;

        // notify listener to stop spinning and show an error
        if (mDownloadWishDoneListener != null) {
            mDownloadWishDoneListener.onDownloadWishDone(false);
        }
    }

    // Fixme: the following functions shouldn't be here
    public void updateProfileFromParse() {
        ParseUser.getCurrentUser().fetchInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "success to fetch Parse user");
                    EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.name));
                    EventBus.getInstance().post(new ProfileChangeEvent(ProfileChangeEvent.ProfileChangeType.email));

                    ParseUser currentUser = (ParseUser) object;
                    final ParseFile parseImage = currentUser.getParseFile("profileImage");
                    if (parseImage != null) {
                        parseImage.getDataInBackground(new GetDataCallback() {
                            @Override
                            public void done(byte[] data, ParseException e) {
                                if (e == null) {
                                    ProfileUtil.saveProfileImageToFile(data);
                                } else {
                                    Log.e(TAG, "fail to get profile image data " + e.toString());
                                }
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "fail to fetch Parse user");
                }
            }
        });
    }

    static public String parseFileNameToLocal(String parseFileName) {
        // when we save file to parse (aws s3), its file name will be prefixed by a server generated random string.
        // for example:
        // file name we uploaded                    IMG173391503.jpg
        // file name actually saved on server       4ab50fb811da73520a71bf6a6e7c8844_IMG173391503.jpg
        return parseFileName.substring(parseFileName.indexOf('_') + 1);
    }
}

