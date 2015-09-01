package com.wish.wishlist;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseCrashReporting;
import com.parse.ParseUser;

/**
 * Created by jiawen on 14-12-23.
 */

public class WishlistApplication extends Application {
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Crash Reporting.
        ParseCrashReporting.enable(this);

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Add your initialization code here
        Parse.initialize(this,
                getResources().getString(R.string.parse_application_id),
                getResources().getString(R.string.parse_client_id));

        Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);

        // Optional - If you don't want to allow Twitter login, you can
        // remove this line (and other related ParseTwitterUtils calls)
        //ParseTwitterUtils.initialize(getString(R.string.twitter_consumer_key),
                //getString(R.string.twitter_consumer_secret));

        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        // Optionally enable public read access.
        // defaultACL.setPublicReadAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);
    }

    public WishlistApplication()
    {
        super();
    }

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.setLocalDispatchPeriod(30);

            //When true, dryRun flag prevents data from being processed with reports. (for testing)
            analytics.setDryRun(BuildConfig.DEBUG);

            //analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);

            Tracker t = analytics.newTracker(R.string.analytics_property_id);
            t.setSessionTimeout(300);
            t.enableExceptionReporting(true);

            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }
}
