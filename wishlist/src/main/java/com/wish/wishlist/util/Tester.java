package com.wish.wishlist.util;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.wish.WebImgMeta;
import com.wish.wishlist.wish.WishImageDownloader;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jiawen on 2016-01-21.
 */
public class Tester implements WishImageDownloader.onWishImageDownloadDoneListener {
    private WishImageDownloader mImageDownloader = new WishImageDownloader();
    private ArrayList<WishItem> mItems = new ArrayList<>();
    private static final String TAG = "Tester";

    private static Tester ourInstance = new Tester();

    public static Tester getInstance() {
        return ourInstance;
    }

    private Tester() {}

    private LatLng getLocation(double x0, double y0, int radius/*meters*/) {
        Random random = new Random();

        // Convert radius from meters to degrees
        double radiusInDegrees = radius / 111000f;

        double u = random.nextDouble();
        double v = random.nextDouble();
        double w = radiusInDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;
        double x = w * Math.cos(t);
        double y = w * Math.sin(t);

        // Adjust the x-coordinate for the shrinking of the east-west distances
        double new_x = x / Math.cos(y0);

        double foundLongitude = new_x + x0;
        double foundLatitude = y + y0;
        System.out.println("Longitude: " + foundLongitude + "  Latitude: " + foundLatitude );
        return new LatLng(foundLatitude, foundLongitude);
    }

    public void addWishes() {
        mImageDownloader.setWishImageDownloadDoneListener(this);
        // create a new item
        Random r = new Random();
        int n = 20;

        for (int i=0; i < n; i++ ) {
            int itemAccess = r.nextInt(2); // int between [0, 2);
            String itemStoreName = "Store";
            String itemName = "Name";
            String itemDesc = "Description";

            int width = 256 + r.nextInt(256); // 256 - 512
            int height = 256 + r.nextInt(256); // 256 - 512
            //String webPicUrl = "http://placehold.it/" + String.valueOf(width) + "x" + String.valueOf(height) + ".jpg";
            String webPicUrl = "http://loremflickr.com/" + String.valueOf(width) + "/" + String.valueOf(height);
            String webImgMetaJSON = new WebImgMeta(webPicUrl, width, height).toJSON();
            Double itemPrice = r.nextDouble();

            LatLng randomLatLng = getLocation(-79, 44, 1000);
            Double lat = randomLatLng.latitude;
            Double lng = randomLatLng.longitude;
            String addStr = "Address";
            int itemPriority = 1;
            int itemComplete = r.nextInt(2);
            String itemLink = "";

            WishItem item = new WishItem(
                    -1,
                    "", // object_id
                    itemAccess,
                    itemStoreName,
                    itemName,
                    itemDesc,
                    System.currentTimeMillis(),
                    webImgMetaJSON,
                    null,
                    null,
                    itemPrice,
                    lat,
                    lng,
                    addStr,
                    itemPriority,
                    itemComplete,
                    itemLink,
                    false,
                    false);

            mItems.add(item);
        }

        mImageDownloader.download(mItems);
    }

    @Override
    public void onWishImageDownloadDone(boolean success) {
        // WishImageDownload set the access to default settings and completed to 0, let's reset it to random
        Random r = new Random();
        for (WishItem item : mItems) {
            int itemAccess = r.nextInt(2); // int between [0, 2);
            item.setAccess(itemAccess);

            int itemCompleted = r.nextInt(2);
            item.setComplete(itemCompleted);

            item.saveToLocal();
            Log.d(TAG, "item saved");
        }

        EventBus.getInstance().post(new MyWishChangeEvent());
    }
}
