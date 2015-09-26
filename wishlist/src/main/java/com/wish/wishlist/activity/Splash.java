package com.wish.wishlist.activity;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wish.wishlist.R;
import com.wish.wishlist.db.DBAdapter;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.LocationDBManager;
import com.wish.wishlist.db.StoreDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.util.DialogOnShowListener;
import com.wish.wishlist.util.ImageManager;

import java.util.ArrayList;

public class Splash extends Activity {
    private static final String VERSION_KEY = "version_number";
    private static final String TAG = "Splash";

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
        DBAdapter.getInstance().createDB();
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
            } catch (Exception e) {}


            if (currentVersionNumber > savedVersionNumber) {
                if (savedVersionNumber == 23) {
                    // do db migration, starting from v24, wish latitude and longitude are saved in the item table
                    // instead of the location table, we should drop store and location table in db schema upgrade
                    // from v24->v25
                    // copy the latitude and longitude from location table to item table and save them
                    ItemDBManager itemDBManager = new ItemDBManager();
                    ArrayList<Long> ids = itemDBManager.getAllItemIds();

                    for (Long id : ids) {
                        ItemDBManager.ItemsCursor wishItemCursor = itemDBManager.getItem(id);
                        long storeID = wishItemCursor.getLong(wishItemCursor.getColumnIndexOrThrow(ItemDBManager.KEY_STORE_ID));
                        if (storeID == -1) {
                            continue;
                        }

                        // Open the Store table in the database
                        StoreDBManager storeDBManager = new StoreDBManager();
                        Cursor storeCursor = storeDBManager.getStore(storeID);

                        long locationID = storeCursor.getLong(storeCursor.getColumnIndexOrThrow(StoreDBManager.KEY_LOCATION_ID));

                        LocationDBManager locationDBManager = new LocationDBManager();
                        double latitude = locationDBManager.getLatitude(locationID);
                        double longitude =  locationDBManager.getLongitude(locationID);

                        WishItem item = WishItemManager.getInstance().getItemById(id);
                        item.setLatitude(latitude);
                        item.setLongitude(longitude);
                        item.saveToLocal();
                    }

                    // generate thumbnail image for all wishes. starting from v24, we save a scaled down image as thumbnail
                    // for each wish. we display the thumbnail image in list/grid view, we also sync the thumbnail image using Parse
                    for (Long id : ids) {
                        WishItem item = WishItemManager.getInstance().getItemById(id);
                        String fullsizeImagePath = item.getFullsizePicPath();
                        if (fullsizeImagePath != null) {
                            ImageManager.saveBitmapToThumb(fullsizeImagePath);
                        }
                    }
                }

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(VERSION_KEY, currentVersionNumber);
                editor.commit();
                //startActivity(new Intent(getApplication(), NewFeatureFragmentActivity.class));
                //startActivity(new Intent(getApplication(), WishList.class));
            } else {
                //startActivity(new Intent(getApplication(), WishList.class));
            }

            Intent intent = new Intent(getApplication(), UserLoginActivity.class);
            intent.putExtra(UserLoginActivity.FROM_SPLASH, true);
            startActivity(intent);

            Splash.this.finish();
        }

        private void showWhatsNewDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(Splash.this, R.style.AppCompatAlertDialogStyle);
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
            //dialog.setOnShowListener(new DialogOnShowListener(Splash.this));
            dialog.show();
        }
    }
}
