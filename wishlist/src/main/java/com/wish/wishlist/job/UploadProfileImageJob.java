package com.wish.wishlist.job;

import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.activity.Profile;
import com.wish.wishlist.util.ImageManager;

import java.io.File;

/**
 * Created by jiawen on 15-09-20.
 */
public class UploadProfileImageJob extends Job {
    public static final int PRIORITY = 1;
    private static final String TAG = "UpdateProfileImageJob";

    public UploadProfileImageJob() {
        // This job requires network connectivity,
        // and should be persisted in case the application exits before job is completed.
        super(new Params(PRIORITY).requireNetwork().persist());
    }

    @Override
    public void onAdded() {
        Log.d(TAG, "onAdded");
        // Job has been saved to disk.
        // This is a good place to dispatch a UI event to indicate the job will eventually run.
        // In this example, it would be good to update the UI with the newly posted tweet.
    }

    @Override
    public void onRun() throws Throwable {
        Log.d(TAG, "onRun");
        // Job logic goes here, upload the profile image to Parse
        File profileImageFile = new File(WishlistApplication.getAppContext().getFilesDir(), Profile.profileImageName());
        final byte[] data = ImageManager.readFile(profileImageFile.getAbsolutePath());
        final ParseFile profileImage = new ParseFile(Profile.profileImageName(), data);
        profileImage.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "succeed in saving profile image to Parse");
                    ParseUser user = ParseUser.getCurrentUser();
                    user.put("profileImage", profileImage);
                    user.saveEventually();
                } else {
                    Log.e(TAG, "fail to save profile image to Parse " + e.toString());
                }
            }
        });
    }
    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        // An error occurred in onRun.
        // Return value determines whether this job should retry running (true) or abort (false).
        return true;
    }
    @Override
    protected void onCancel() {
        Log.e("A", "onCancel");
        // Job has exceeded retry attempts or shouldReRunOnThrowable() has returned false.
    }
}