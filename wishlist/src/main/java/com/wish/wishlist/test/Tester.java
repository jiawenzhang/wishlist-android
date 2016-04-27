package com.wish.wishlist.test;

import android.util.Log;

import com.google.android.gms.common.images.WebImage;
import com.google.android.gms.maps.model.LatLng;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.wish.ImgMeta;
import com.wish.wishlist.wish.WishImageDownloader;

import java.util.ArrayList;
import java.util.Random;

import de.svenjacobs.loremipsum.LoremIpsum;

/**
 * Created by jiawen on 2016-01-21.
 */
public class Tester implements WishImageDownloader.onWishImageDownloadDoneListener {
    private WishImageDownloader mImageDownloader = new WishImageDownloader();
    private ArrayList<WishItem> mItems = new ArrayList<>();
    private static final String TAG = "Tester";
    private static final LoremIpsum loremIpsum = new LoremIpsum();

    private static Tester ourInstance = new Tester();

    public static Tester getInstance() {
        return ourInstance;
    }

    private Tester() {}

    private static LatLng getLocation(double x0, double y0, int radius/*meters*/) {
        Random random = new Random();

        if (random.nextInt(3) == 0) {
            return null;
        }

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

    public static int randomAccess() {
        return new Random().nextInt(2); // int between [0, 2);
    }

    public static String randomName() {
        return loremIpsum.getWords(new Random().nextInt(20)); // text between 0-20 characters;
    }

    public static String randomDesc() {
        Random r = new Random();
        return r.nextInt(5) == 0 ? null : loremIpsum.getWords(r.nextInt(50)); // text between 0-50 characters or null
    }

    public static String randomStoreName() {
        Random r = new Random();
        return r.nextInt(3) == 0 ? null : loremIpsum.getWords(r.nextInt(10));
    }

    public static Double randomPrice() {
        Random r = new Random();
        return r.nextInt(5) == 0 ? null : r.nextDouble() * r.nextInt(10000);
    }

    public static String randomAddress() {
        Random r = new Random();
        return r.nextInt(5) == 0 ? null : loremIpsum.getWords(r.nextInt(20));
    }

    public static int randomComplete() {
        return new Random().nextInt(2);
    }

    public static ImgMeta randomImgMeta() {
        Random r = new Random();

        int width = 256 + r.nextInt(256); // 256 - 512
        int height = 256 + r.nextInt(256); // 256 - 512
        //String webPicUrl = "http://placehold.it/" + String.valueOf(width) + "x" + String.valueOf(height) + ".jpg";
        String webPicUrl = "http://loremflickr.com/" + String.valueOf(width) + "/" + String.valueOf(height);
        return r.nextInt(5) == 0 ? null : new ImgMeta(ImgMeta.WEB, webPicUrl, width, height);
    }

    public static long randomUpdatedTime() {
        return System.currentTimeMillis() - new Random().nextInt(30) * 3600L * 24L * 1000L; // 30 days
    }

    public static WishItem generateWish() {
        // create a new item filled with random data
        ImgMeta imgMeta = randomImgMeta();
        String imgMetaJSON = imgMeta == null ? null : imgMeta.toJSON();
        String itemLink = imgMeta == null ? null : imgMeta.mUrl;

        LatLng randomLatLng = getLocation(-79, 44, 1000);
        Double lat = null;
        Double lng = null;
        if (randomLatLng != null) {
            lat = randomLatLng.latitude;
            lng = randomLatLng.longitude;
        }
        int itemPriority = 1;

        return new WishItem(
                -1,
                "", // object_id
                randomAccess(),
                randomStoreName(),
                randomName(),
                randomDesc(),
                randomUpdatedTime(),
                imgMetaJSON,
                null,
                randomPrice(),
                lat,
                lng,
                randomAddress(),
                itemPriority,
                randomComplete(),
                itemLink,
                false,
                false,
                true);
    }

    public void addWishes() {
        mImageDownloader.setWishImageDownloadDoneListener(this);
        // create a new item
        int n = 20;
        for (int i = 0; i < n; i++ ) {
            mItems.add(generateWish());
        }

        mImageDownloader.download(mItems);
    }

    public static WishItem updateWish(WishItem item) {
        boolean changed = false;
        if (new Random().nextDouble() <= 0.2) {
            Log.d(TAG, "change access");
            item.setAccess(item.getAccess() == 0 ? 1 : 0);
            changed = true;
        }
        if (new Random().nextDouble() <= 0.2) {
            Log.d(TAG, "change store");
            item.setStoreName(randomStoreName());
            changed = true;
        }
        if (new Random().nextDouble() <= 0.5) {
            Log.d(TAG, "change name");
            // 50% change to change name
            item.setName(randomName());
            changed = true;
        }
        if (new Random().nextDouble() <= 0.2) {
            Log.d(TAG, "change description");
            item.setDesc(randomDesc());
            changed = true;
        }
        if (new Random().nextDouble() <= 0.2) {
            Log.d(TAG, "change image");
            ImgMeta imgMeta = randomImgMeta();
            if (imgMeta == null) {
                item.setImgMeta(null, null, 0, 0);
                item.setLink(null);
            } else {
                item.setImgMeta(ImgMeta.WEB, imgMeta.mUrl, imgMeta.mWidth, imgMeta.mHeight);
                item.setLink(imgMeta.mUrl);
            }
            changed = true;
        }
        if (new Random().nextDouble() <= 0.2) {
            Log.d(TAG, "change price");
            item.setPrice(randomPrice());
            changed = true;
        }
        if (new Random().nextDouble() <= 0.2) {
            Log.d(TAG, "change address");
            item.setAddress(randomAddress());
            changed = true;
        }
        if (new Random().nextDouble() <= 0.2) {
            Log.d(TAG, "change complete");
            item.setComplete(item.getComplete() == 0 ? 1 : 0);
            changed = true;
        }

        if (!changed) {
            // we are really unlucky, nothing changed, so let's just change the name for sure
            item.setName(randomName());
        }
        item.setSyncedToServer(false);

        return item;
    }

    @Override
    public void onWishImageDownloadDone(boolean success) {
        // WishImageDownload set the access to default settings and completed to 0, and updated time to now, let's reset it to random
        Random r = new Random();
        for (WishItem item : mItems) {
            int itemAccess = r.nextInt(2); // int between [0, 2);
            item.setAccess(itemAccess);

            int itemCompleted = r.nextInt(2);
            item.setComplete(itemCompleted);

            item.setUpdatedTime(System.currentTimeMillis() - r.nextInt(30) * 3600L * 24L * 1000L); // 30 days

            item.setDownloadImg(false);
            item.saveToLocal();
            Log.d(TAG, "item saved");
        }

        EventBus.getInstance().post(new MyWishChangeEvent());
    }
}
