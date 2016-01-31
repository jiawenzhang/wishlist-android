package com.wish.wishlist.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.LocationDBManager;
import com.wish.wishlist.db.StoreDBManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by jiawen on 2016-01-31.
 */
public class MigrationTask extends AsyncTask<Void, Void, Void> {//<param, progress, result>
    static private final String TAG = "Migration";

    /* done listener */
    public interface OnMigrationDone {
        void onMigrationDone();
    }

    private OnMigrationDone mListener;
    private void migrationDone() {
        mListener.onMigrationDone();
    }
    /* done listener */


    public MigrationTask(OnMigrationDone listener) {
        this.mListener = listener;
    }

    @Override
    protected Void doInBackground(Void... arg) {
        migrate();
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        migrationDone();
    }

    private void migrate() {
        Context ctx = WishlistApplication.getAppContext();
        SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getString(R.string.app_name), Context.MODE_PRIVATE);
        int currentVersionNumber = 0;
        int savedVersionNumber = sharedPref.getInt(ctx.getString(R.string.version_number), 0);
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            currentVersionNumber = pi.versionCode;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return;
        }

        if (currentVersionNumber > savedVersionNumber) {
            if (savedVersionNumber == 23) {
                migrate_to_24();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(ctx.getString(R.string.version_number), currentVersionNumber);
                editor.commit();
            }
        }
    }

    private void migrate_to_24() {
        Log.d(TAG, "migrate_to_24");
        // do data migration, starting from v24, wish latitude and longitude are saved in the item table
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
        Log.d(TAG, "location migration done");

        // Starting from v24, we save full size photo in the "image" folder in app's sandbox
        // instead of album folder in external storage,
        // copy full size photo from album folder to app sandbox and update the fullsizePicPath column in WishItem db
        ArrayList<WishItem> items = WishItemManager.getInstance().getAllItems();
        for (WishItem item : items) {
            if (item.getFullsizePicPath() != null) {
                File f = new File(item.getFullsizePicPath());
                if (f.getParent().endsWith("/image")) {
                    // we have already copied the images, continue
                    // this can happen when app crashes or exits in the middle of coping images, and now on-restart, we
                    // are trying to copy again.
                    continue;
                }

                final Bitmap bitmap = ImageManager.decodeSampledBitmapFromFile(item.getFullsizePicPath(), 1024);
                if (bitmap != null) {
                    final String newFullsizePhotoPath = ImageManager.saveBitmapToAlbum(bitmap);
                    ImageManager.saveBitmapToThumb(bitmap, newFullsizePhotoPath);
                    item.setFullsizePicPath(newFullsizePhotoPath);
                    item.saveToLocal();
                }
            }
        }
        Log.d(TAG, "copy image migration done");
    }
}


