package com.wish.wishlist.test;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

import java.util.Random;

/**
 * Created by jiawen on 2016-04-25.
 */
public class WishService extends IntentService {
    static final String TAG = "WishService";
    final Handler mHandler = new Handler();
    private int mWishCount = 3;
    private int mWishChanged = 0;
    private int mPeriodMin = 1; //second
    private int mPeriodMax = 5; //second
    final private static int SECOND = 1000;

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
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();

        changeWishLater();
    }

    private void changeWishLater() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
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

                EventBus.getInstance().post(new MyWishChangeEvent());

                if (++mWishChanged < mWishCount) {
                    changeWishLater();
                } else {
                    Log.d(TAG, mWishCount + " wishes changed, done");
                }
            }
        }, randomDuration());
    }

    private int randomDuration() {
        Random random = new Random();

        int duration = SECOND * mPeriodMin + SECOND * random.nextInt(mPeriodMax);
        Log.d(TAG, "random duration " + duration/1000 + "s");
        return duration;
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
    }

    private void updateWish(int count) {
        Log.e(TAG, "update " + count + " wish");
        // pick count wishes randomly and update them
        for (int i=0; i<count; i++) {
            Long id = ItemDBManager.getRandomItemId();
            if (id != null) {
                WishItem item = WishItemManager.getInstance().getItemById(id);
                item = Tester.updateWish(item);
                item.setUpdatedTime(System.currentTimeMillis());
                item.save();
            }
        }
    }

    private void addWish(int count) {
        Log.e(TAG, "add " + count + " wish");
        for (int i=0; i<count; i++) {
            WishItem item = Tester.generateWish();
            item.setUpdatedTime(System.currentTimeMillis());
            item.save();
        }
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
}
