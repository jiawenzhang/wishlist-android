package com.wish.wishlist.test;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.util.StringUtil;

import java.io.IOException;
import java.util.Random;

/**
 * Created by jiawen on 2016-04-25.
 */
public class WishService extends IntentService {
    static final String TAG = "WishService";
    private int mWishChanged = 0;
    private final static int mWishCount = 20;
    final private static int MS_PER_SECOND = 1000;
    private final static int mPeriodMin = 1; //10; //second
    private final static int mPeriodMaxVariation = 5; //1; //second

    enum WishAction {
        Add,
        Update,
        Delete,
    }

    public WishService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d(TAG, "onHandleIntent");
        changeWishLater();
    }

    private void changeWishLater() {
        if (mWishChanged++ == mWishCount) {
            Log.d(TAG, mWishCount + " wishes changed, done");
            return;
        }

        SystemClock.sleep(randomDuration());

        WishAction action = wishAction();
        int count = wishCount();
        if (action == WishAction.Add) {
            addWish(count);
        } else if (action == WishAction.Update) {
            updateWish(count);
        } else if (action == WishAction.Delete){
            deleteWish(count);
        } else {
            Log.e(TAG, "error, unknown wish action!");
        }
    }

    private int randomDuration() {
        Random random = new Random();

        int duration = MS_PER_SECOND * mPeriodMin + MS_PER_SECOND * random.nextInt(mPeriodMaxVariation);
        Log.d(TAG, "random duration " + duration/1000 + "s");
        return duration;
    }

    private void updateWish(int count) {
        Log.e(TAG, "update " + count + " wish");
        // pick count wishes randomly and update them
        for (int i=0; i<count; i++) {
            Long id = ItemDBManager.getRandomItemId();
            if (id == null) {
                continue;
            }

            WishItem item = WishItemManager.getInstance().getItemById(id);
            String imgMetaJSON = item.getImgMetaJSON();
            item = Tester.updateWish(item);
            if (!StringUtil.compare(item.getImgMetaJSON(), imgMetaJSON)) {
                // image changed, remove the old one and download new one if necessary
                item.removeImage();
                if (item.getImgMetaJSON() != null) {
                    downloadWishImage(item);
                }
            }
            item.setSyncedToServer(false);
            item.setUpdatedTime(System.currentTimeMillis());
            item.saveToLocal();
        }

        EventBus.getInstance().post(new MyWishChangeEvent());
        syncInMainThread();
        changeWishLater();
    }

    private void addWish(int count) {
        Log.e(TAG, "add " + count + " wish");
        for (int i=0; i<count; i++) {
            WishItem item = Tester.generateWish();
            if (item.getImgMetaJSON() != null) {
                downloadWishImage(item);
            }
            item.setSyncedToServer(false);
            item.setUpdatedTime(System.currentTimeMillis());
            item.saveToLocal();
            TagItemDBManager.instance().Update_item_tags(item.getId(), Tester.randomTags());
        }

        EventBus.getInstance().post(new MyWishChangeEvent());
        syncInMainThread();
        changeWishLater();
    }

    private void downloadWishImage(WishItem item) {
        try {
            // we are in a background thread, so load the image synchronously
            Bitmap bitmap = Picasso.with(WishlistApplication.getAppContext()).load(item.getImgMetaArray().get(0).mUrl).resize(ImageManager.IMG_WIDTH, 0).get();
            String fullsizePath;
            if (bitmap != null) {
                // save the bitmap and a thumbnail as a local file
                fullsizePath = ImageManager.saveBitmapToFile(bitmap);
                ImageManager.saveBitmapToThumb(bitmap, fullsizePath);
                item.setFullsizePicPath(fullsizePath);
                item.setDownloadImg(false);
            } else {
                Log.e(TAG, "bitmap null");
            }
        } catch (IOException e) {
            Log.e(TAG, "fail to download image: " + e.toString());
        }
    }

    private void deleteWish(int count) {
        Log.e(TAG, "delete " + count + " wish");
        // pick count wishes randomly and delete them
        for (int i=0; i<count; i++) {
            Long id = ItemDBManager.getRandomItemId();
            if (id != null) {
                WishItemManager.getInstance().deleteItemById(id);
            }
        }

        EventBus.getInstance().post(new MyWishChangeEvent());
        syncInMainThread();
        changeWishLater();
    }

    private WishAction wishAction() {
        Random random = new Random();
        // 80% add, 10% update, 10% delete
        int r = random.nextInt(10);
        if (r < 8) {
            return WishAction.Add;
        } else if (r < 9) {
            return WishAction.Update;
        } else {
            return WishAction.Delete;
        }
    }

    private int wishCount() {
        Random random = new Random();
        // 90% 1, 5% 2, 5% 3
        int r = random.nextInt(100);
        if (r < 90) {
            return 1;
        } else if (r < 95) {
            return 2;
        } else {
            return 3;
        }
    }

    private void syncInMainThread() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                SyncAgent.getInstance().sync();
            }
        };
        mainHandler.post(runnable);
    }
}
