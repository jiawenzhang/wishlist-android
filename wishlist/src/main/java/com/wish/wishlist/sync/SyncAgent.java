package com.wish.wishlist.sync;

import android.app.Activity;
import android.os.AsyncTask;
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
import com.wish.wishlist.util.Analytics;
import com.wish.wishlist.util.NetworkHelper;
import com.wish.wishlist.util.ProfileUtil;
import com.wish.wishlist.util.Util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by jiawen on 15-07-11.
 */
public class SyncAgent implements
        DownloadMetaTask.DownloadMetaTaskDoneListener,
        UploadTask.UploadTaskDoneListener,
        DownloadImageTask.DownloadImageTaskDoneListener {
    private static SyncAgent instance = null;
    private OnSyncWishChangedListener mSyncWishChangedListener;
    private OnDownloadWishMetaDoneListener mDownloadWishMetaDoneListener;
    private OnSyncDoneListener mSyncDoneListener;
    private OnSyncStartListener mSyncStartListener;

    // among all the items synced with the server in my previous sync, get the latest "updatedAt", and save it as lastDownloadStamp
    // we use this to identify items uploaded to or changed on the server by another device since my previous sync down,
    // so I can download them in my next sync with the server. we can trust "updatedAt" since it is a timestamp
    // set by the server

    private DownloadMetaTask mDownloadMetaTask = new DownloadMetaTask();
    private UploadTask mUploadTask = new UploadTask();
    private DownloadImageTask mDownloadImageTask = new DownloadImageTask();

    private boolean mDownloading = false;
    private boolean mSyncing = false;
    private boolean mScheduleToSync = false;
    private static String TAG = "SyncAgent";

    private class CheckServerReachable extends AsyncTask<Void, Void, Boolean> {//<param, progress, result>
        @Override
        protected Boolean doInBackground(Void... arg) {
            // isNetworkAvailable() only is not sufficient to tell if we have internet.
            // it only tells us if the network interface is up. if the device is connected to a
            // wifi hotspot that does not have internet access, isNetworkAvailable will return true, but
            // device still does not have internet. so do an extra http connect to the parse server to check
            // if the device can "actually" reach the server
            if (!NetworkHelper.isNetworkAvailable()) {
                Log.d(TAG, "no network, sync is not started");
                // Fixme: shall we attempt sync later?
                return false;
            }

            try {
                URL urlServer = new URL(WishlistApplication.getAppContext().getString(R.string.parse_server_url));
                HttpURLConnection urlConn = (HttpURLConnection) urlServer.openConnection();
                urlConn.setRequestMethod("HEAD");
                urlConn.setConnectTimeout(5000); // http request from countries like China can take several seconds
                if (urlConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "parse server reachable");
                    return true;
                } else {
                    Log.e(TAG, "parse server unreachable " + urlConn.getResponseMessage());
                    Analytics.send(Analytics.DEBUG, "ParseServerUnreachable", urlConn.getResponseMessage());
                    return false;
                }
            } catch (IOException e) {
                Log.e(TAG, "parse server unreachable, error: " + e.toString());
                Analytics.send(Analytics.DEBUG, "ParseServerUnreachable", e.toString());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean reachable) {
            serverReachable(reachable);
        }
    }

    /* listener */
    public interface OnSyncWishChangedListener {
        void onSyncWishChanged();
    }

    public interface OnDownloadWishMetaDoneListener {
        void onDownloadWishMetaDone(boolean success);
    }

    public void registerListener(Activity activity) {
        try {
            mSyncWishChangedListener = (OnSyncWishChangedListener) activity;
            mDownloadWishMetaDoneListener = (OnDownloadWishMetaDoneListener) activity;
        }
        catch (final ClassCastException e) {
            Log.e(TAG, "fail to registerListener");
            throw new ClassCastException(activity.toString() + " must implement OnSyncWishChanged/OnDownloadWishDone");
        }
    }
    /* listener */


    /* OnSyncDone listener */
    public interface OnSyncDoneListener {
        void onSyncDone(boolean success);
    }

    public void registerSyncDoneListener(Object obj) {
        try {
            mSyncDoneListener = (OnSyncDoneListener) obj;
        } catch (final ClassCastException e) {
            Log.e(TAG, "fail to registerListener");
            throw new ClassCastException(obj.toString() + " must implement OnSyncDone");
        }
    }
    /* OnSyncDone listener */


    /* OnSyncStart listener */
    public interface OnSyncStartListener {
        void onSyncStart();
    }

    public void registerSyncStartListener(Object obj) {
        try {
            mSyncStartListener = (OnSyncStartListener) obj;
        } catch (final ClassCastException e) {
            Log.e(TAG, "fail to registerListener");
            throw new ClassCastException(obj.toString() + " must implement OnSyncStart");
        }
    }
    /* OnSyncStart listener */


    public static SyncAgent getInstance() {
        if (instance == null) {
             instance = new SyncAgent();
        }
        return instance;
    }

    private SyncAgent() {
        mDownloadMetaTask.registerListener(this);
        mUploadTask.registerListener(this);
        mDownloadImageTask.registerListener(this);
    }

    public boolean downloading() {
        return mDownloading;
    }

    public boolean syncing() {
        return mSyncing;
    }

    // call sync on app start up
    // how does parse trigger sync on the client? push notification?
    public void sync() {
        Log.d(TAG, "sync");
        if (!Util.deviceAccountEnabled()) {
            return;
        }

        if (ParseUser.getCurrentUser() == null) {
            Log.e(TAG, "user not login, sync is disabled ");
            return;
        }

        if (mSyncing) {
            // if we are in the process of syncing, run sync again after the current sync is finished
            Log.d(TAG, "mSync true, schedule sync");
            mScheduleToSync = true;

            Analytics.send(Analytics.SYNC, "Schedule", null);
            return;
        }

        mSyncing = true;
        mDownloading = true;

        Analytics.send(Analytics.SYNC, "Start", null);

        if (mSyncStartListener != null) {
            mSyncStartListener.onSyncStart();
        }

        new CheckServerReachable().execute();
    }

    private void serverReachable(boolean reachable) {
        if (!reachable) {
            syncDone(false);
            return;
        }

        // start the sync process with downloadMetaTask, then uploadTask, then DownloadImageTask
        // the next task will be started when the previous task is done
        // if error occurs in any of the tasks, syncDone will be called with success = false
        mDownloadMetaTask.run();
    }

    @Override
    public void downloadMetaTaskDone(boolean success, boolean wishMetaChanged) {
        mDownloading = false;

        if (mDownloadWishMetaDoneListener != null) {
            mDownloadWishMetaDoneListener.onDownloadWishMetaDone(success);
        }

        // we have finished downloading items meta, notify list/grid view to refresh
        if (wishMetaChanged && mSyncWishChangedListener != null) {
            mSyncWishChangedListener.onSyncWishChanged();
        }

        if (success) {
            mUploadTask.run();
        } else {
            syncDone(false);
        }
    }

    @Override
    public void uploadTaskDone(boolean success) {
        if (success) {
            // start downloading image task
            mDownloadImageTask.run();
        } else {
            syncDone(false);
        }
    }

    @Override
    public void downloadImageTaskDone(boolean success, boolean imageChanged) {
        if (imageChanged && mSyncWishChangedListener != null) {
            mSyncWishChangedListener.onSyncWishChanged();
        }
        syncDone(success);
    }

    private void syncDone(boolean success) {
        Log.d(TAG, "syncDone success? " + success);
        mSyncing = false;
        mDownloading = false;

        Analytics.send(Analytics.SYNC, "Done", success ? "Success" : "Fail");

        if (mSyncDoneListener != null) {
            mSyncDoneListener.onSyncDone(success);
        }

        if (!success) {
            // when sync fails (most likely due to network unavailable), let's cancel scheduled sync
            // and wait until the next sync is triggered
            mScheduleToSync = false;
            return;
        }

        if (mScheduleToSync) {
            mScheduleToSync = false;
            sync();
        }
    }

    // Fixme: the following functions shouldn't be here
    public void updateProfileFromParse() {
        if (ParseUser.getCurrentUser() == null) {
            Log.e(TAG, "user not logged in, cannot update profile");
            return;
        }

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
                                    Analytics.send(Analytics.DEBUG, "FetchProfileImageFail", e.toString());
                                }
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "fail to fetch Parse user");
                    Analytics.send(Analytics.DEBUG, "FetchParseUserFail", e.toString());
                }
            }
        });
    }

    public static String parseFileNameToLocal(String parseFileName) {
        // when we save file to parse (aws s3), its file name will be prefixed by a server generated random string.
        // for example:
        // file name we uploaded                    IMG173391503.jpg
        // file name actually saved on server       4ab50fb811da73520a71bf6a6e7c8844_IMG173391503.jpg
        return parseFileName.substring(parseFileName.indexOf('_') + 1);
    }
}

