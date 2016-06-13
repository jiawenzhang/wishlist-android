package com.wish.wishlist;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.github.stkent.amplify.tracking.Amplify;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import com.parse.Parse;
import com.parse.ParseACL;
//import com.parse.ParseCrashReporting;
import com.parse.ParseFacebookUtils;
//import com.parse.interceptors.ParseLogInterceptor;
import com.path.android.jobqueue.JobManager;
import com.wish.wishlist.util.Options;
import com.wish.wishlist.util.Util;

/**
 * Created by jiawen on 14-12-23.
 */

public class WishlistApplication extends Application {
    private JobManager mJobManager = null;
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<>();
    private static Context mContext;

    public WishlistApplication()
    {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        Options.DeviceCountry deviceCountry = new Options.DeviceCountry(null);
        deviceCountry.read();
        if (deviceCountry.val() == null) {
            deviceCountry.setVal(Util.getDeviceCountry(this));
            deviceCountry.save();
        }

        Util.initDeviceAccountEnabled();

        // Initialize Crash Reporting.
        //ParseCrashReporting.enable(this);

        // Enable Local Datastore.
        //Parse.enableLocalDatastore(this);

        // Add your initialization code here

        if (Util.deviceAccountEnabled()) {
            Parse.initialize(new Parse.Configuration.Builder(getAppContext())
                    .applicationId(getResources().getString(R.string.parse_application_id))
                    .clientKey(getResources().getString(R.string.parse_client_key))
                            //.server("http://localhost:1337/parse")
                    .server(getString(R.string.parse_server_url) + "/parse/")
                            //.addNetworkInterceptor(new ParseLogInterceptor())
                    .build());

            Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);

            // Optional - If you don't want to allow Twitter login, you can
            // remove this line (and other related ParseTwitterUtils calls)
            //ParseTwitterUtils.initialize(getString(R.string.twitter_consumer_key),
            //getString(R.string.twitter_consumer_secret));

//        ParseUser.enableAutomaticUser();
            ParseACL defaultACL = new ParseACL();
            // Optionally enable public read access.
            // defaultACL.setPublicReadAccess(true);
            ParseACL.setDefaultACL(defaultACL, true);

            FacebookSdk.sdkInitialize(getApplicationContext());
            ParseFacebookUtils.initialize(this);
        }

        configJobManager();

        Amplify.get(this)
                .setFeedbackEmailAddress("beanswishlist@gmail.com")
                //.setAlwaysShow(true)
                .applyAllDefaultRules();
    }

    public static Context getAppContext() {
        return mContext;
    }

    public static void restart() {
        // re-launch the app
        Intent i = mContext.getPackageManager()
                .getLaunchIntentForPackage(mContext.getPackageName());

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(i);
    }

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.setLocalDispatchPeriod(30);

            //When true, dryRun flag prevents data from being processed with reports. (for testing)
            analytics.setDryRun(BuildConfig.DEBUG);

            //analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);

            Tracker t = analytics.newTracker(getResources().getString(R.string.analytics_property_id));
            t.setSessionTimeout(300);
            t.enableExceptionReporting(true);

            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    private void configJobManager() {
        mJobManager = new JobManager(mContext);
    }

    public JobManager getJobManager() {
        return mJobManager;
    }
}
