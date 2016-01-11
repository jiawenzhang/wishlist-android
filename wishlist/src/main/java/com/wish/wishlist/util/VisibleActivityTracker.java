package com.wish.wishlist.util;

import android.app.Activity;

/**
 * Created by jiawen on 2016-01-10.
 */
public class VisibleActivityTracker {

    private Activity mVisibleActivity;

    private static VisibleActivityTracker ourInstance = new VisibleActivityTracker();

    public static VisibleActivityTracker getInstance() {
        return ourInstance;
    }

    private VisibleActivityTracker() {
    }

    public void activityResumed(Activity a) {
        mVisibleActivity = a;
    }

    public void activityPaused() {
        mVisibleActivity = null;
    }

    public Activity visibleActivity() {
        return mVisibleActivity;
    }
}
