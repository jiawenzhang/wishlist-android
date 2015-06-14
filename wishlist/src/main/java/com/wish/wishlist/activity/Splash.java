package com.wish.wishlist.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.R;
import com.wish.wishlist.db.DBAdapter;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.util.DialogOnShowListener;

public class Splash extends Activity{
    private static final String VERSION_KEY = "version_number";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tracker t = ((WishlistApplication) getApplication()).getTracker(WishlistApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory("App")
                .setAction("Start")
                .build());

        setContentView(R.layout.splash);

        Handler x = new Handler();
        x.postDelayed(new splashhandler(), 2000);
        DBAdapter.getInstance(this).createDB();
    }

    class splashhandler implements Runnable {
        public void run() {
            //show the what's new dialog if necessary
            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
            int currentVersionNumber = 0;
            int savedVersionNumber = sharedPref.getInt(VERSION_KEY, 0);
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                currentVersionNumber = pi.versionCode;
            } catch (Exception e) {
            }

            if (currentVersionNumber > savedVersionNumber) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(VERSION_KEY, currentVersionNumber);
                editor.commit();
                //startActivity(new Intent(getApplication(), NewFeatureFragmentActivity.class));
                startActivity(new Intent(getApplication(), WishList.class));
            }
            else {
                startActivity(new Intent(getApplication(), WishList.class));
            }
            Splash.this.finish();
        }

        private void showWhatsNewDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(Splash.this);
            builder.setTitle("What's new");
            builder.setMessage("Version 1.1.1\n\n" +
                    "Completely renovated grid view, showing wishes in multi-column staggered fashion.\n\n" +
                    "Improved list view.\n\n" +
                    "Loads wish images more efficiently.\n");

            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    startActivity(new Intent(getApplication(), WishList.class));
                    Splash.this.finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(new DialogOnShowListener(Splash.this));
            dialog.show();
        }
    }
}
