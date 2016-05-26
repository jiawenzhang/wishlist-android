package com.wish.wishlist.test;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.Owner;
import com.wish.wishlist.wish.ImgMeta;
import com.wish.wishlist.wish.ImgMetaArray;
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
    public static ArrayList<String> mGalleryFiles;

    private static Tester ourInstance = new Tester();

    public static Tester getInstance() {
        return ourInstance;
    }

    private Tester() {
        mGalleryFiles = getAllImagesPath();
    }

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
        return loremIpsum.getWords(1 + new Random().nextInt(20)); // text between 1-20 characters;
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

        // 10% chance to get an invalid url
        String url = r.nextInt(10) == 0 ? "http://" + loremIpsum.getWords(r.nextInt(2)) : webPicUrl;
        return r.nextInt(5) == 0 ? null : new ImgMeta(ImgMeta.WEB, url, width, height);
    }

    public static long randomUpdatedTime() {
        return System.currentTimeMillis() - new Random().nextInt(30) * 3600L * 24L * 1000L; // 30 days
    }

    public static ArrayList<String> randomTags() {
        Random r = new Random();
        ArrayList<String> tags = new ArrayList<>();
        int tagCount = r.nextInt(3);
        for (int i=0; i<tagCount; i++) {
            String tag = loremIpsum.getWords(1 + r.nextInt(6));
            while (tags.contains(tag)) {
                // make sure tags are not duplicated
                tag = loremIpsum.getWords(1 + r.nextInt(6));
            }
            tags.add(tag);
        }
        Log.d(TAG, "random tags " + tags);
        return tags;
    }

    public static WishItem generateWish() {
        // create a new item filled with random data
        String imgMetaJSON = null;
        String itemLink = null;
        String fullsizePhotoPath = null;

        if (new Random().nextInt(5) == 0) {
            // 1/5 chance no image
            Log.d(TAG, "no image");
        } else if (new Random().nextInt(3) == 0) {
            // 1/3 chance get image from local gallery
            Log.d(TAG, "from local gallery");
            String path = mGalleryFiles.get(new Random().nextInt(mGalleryFiles.size()));
            Log.e(TAG, path);
            final Bitmap bitmap = ImageManager.decodeSampledBitmapFromFile(path, ImageManager.IMG_WIDTH);
            fullsizePhotoPath = ImageManager.saveBitmapToFile(bitmap);
            ImageManager.saveBitmapToThumb(bitmap, fullsizePhotoPath);
        } else {
            // 2/3 chance get image from web
            ImgMeta imgMeta = randomImgMeta();
            imgMetaJSON = imgMeta == null ? null : new ImgMetaArray(imgMeta).toJSON();
            itemLink = imgMeta == null ? null : imgMeta.mUrl;
        }

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
                null, // object_id
                Owner.id(),
                randomAccess(),
                randomStoreName(),
                randomName(),
                randomDesc(),
                randomUpdatedTime(),
                imgMetaJSON,
                fullsizePhotoPath,
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

            if (new Random().nextDouble() <= 0.3) {
                item.removeImage();

                Log.d(TAG, "from local gallery");
                String path = mGalleryFiles.get(new Random().nextInt(mGalleryFiles.size()));
                final Bitmap bitmap = ImageManager.decodeSampledBitmapFromFile(path, ImageManager.IMG_WIDTH);
                String fullsizePhotoPath = ImageManager.saveBitmapToFile(bitmap);
                ImageManager.saveBitmapToThumb(bitmap, fullsizePhotoPath);
                item.setFullsizePicPath(fullsizePhotoPath);
                item.setImgMetaArray(null);

                changed = true;
            } else {
                ImgMeta imgMeta = randomImgMeta();
                if (imgMeta != null) {
                    item.removeImage();

                    ArrayList<ImgMeta> metaArray = new ArrayList<>();
                    metaArray.add(imgMeta);
                    item.setImgMetaArray(metaArray);
                    item.setLink(imgMeta.mUrl);

                    changed = true;
                }
            }
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
        if (new Random().nextDouble() <= 0.1) {
            Log.d(TAG, "change tags");
            TagItemDBManager.instance().Update_item_tags(item.getId(), Tester.randomTags());
            changed = true;
        }

        if (!changed) {
            // we are really unlucky, nothing changed, so let's just change the name for sure
            item.setName(randomName());
        }
        item.setSyncedToServer(false);

        return item;
    }

    public static ArrayList<String> getAllImagesPath() {
        Uri uri;
        Cursor cursor;
        int column_index_data;
        int column_index_folder_name;
        ArrayList<String> listOfAllImages = new ArrayList<>();
        String absolutePathOfImage;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME };

        cursor = WishlistApplication.getAppContext().getContentResolver().query(
                uri,
                projection,
                null,
                null,
                null);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        column_index_folder_name = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);
            listOfAllImages.add(absolutePathOfImage);
        }

        return listOfAllImages;
    }

    @Override
    public void onWishImageDownloadDone(boolean success) {
        // WishImageDownload set the access to default settings and completed to 0, and updated time to now, let's reset it to random
        for (WishItem item : mItems) {
            item.setAccess(randomAccess());
            item.setComplete(randomComplete());
            item.setUpdatedTime(randomUpdatedTime());

            item.setDownloadImg(false);
            item.saveToLocal();

            TagItemDBManager.instance().Update_item_tags(item.getId(), randomTags());
            Log.d(TAG, "item saved");
        }

        EventBus.getInstance().post(new MyWishChangeEvent());
    }
}
