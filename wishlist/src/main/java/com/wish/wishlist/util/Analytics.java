package com.wish.wishlist.util;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.WishlistApplication;

/**
 * Created by jiawen on 2016-02-01.
 */
public class Analytics {

    // Category
    public static final String APP = "App";
    public static final String WISH = "Wish";
    public static final String TAG = "Tag";
    public static final String SOCIAL = "Social";
    public static final String MAP = "Map";
    public static final String DEBUG = "Debug";

    public static void send(String category, String action, String label) {
        Tracker t = ((WishlistApplication) WishlistApplication.getAppContext()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);

        if (label == null) {
            t.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .build());
        } else {
            t.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .build());
        }
    }

    public static void sendScreen(String screenName) {
        Tracker t = ((WishlistApplication) WishlistApplication.getAppContext()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.setScreenName(screenName);
        t.send(new HitBuilders.AppViewBuilder().build());
    }
}



