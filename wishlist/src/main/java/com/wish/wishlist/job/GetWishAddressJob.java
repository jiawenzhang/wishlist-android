package com.wish.wishlist.job;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.RetryConstraint;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.event.EventBus;
import com.wish.wishlist.event.MyWishChangeEvent;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.model.WishItemManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by jiawen on 15-09-20.
 */
public class GetWishAddressJob extends Job {
    public static final int PRIORITY = 1;
    private static final String TAG = "GetWishAddressJob";

    private long mItemId;

    public GetWishAddressJob(long itemId) {
        // This job requires network connectivity,
        // and should be persisted in case the application exits before job is completed.
        super(new Params(PRIORITY).requireNetwork().persist());
        mItemId = itemId;
    }

    @Override
    public void onAdded() {
        Log.d(TAG, "onAdded");
        // Job has been saved to disk.
        // This is a good place to dispatch a UI event to indicate the job will eventually run.
    }

    @Override
    public void onRun() throws Throwable {
        Log.d(TAG, "onRun");
        // Job logic goes here, upload the profile image to Parse
        WishItem item = WishItemManager.getInstance().getItemById(mItemId);
        if (item == null || item.getDeleted()) {
            return;
        }

        double lat = item.getLatitude();
        double lng = item.getLongitude();
        String address;

        //we have a location by gps, but don't have an address
        Geocoder gc = new Geocoder(WishlistApplication.getAppContext(), Locale.getDefault());
        try {
            List<Address> addresses = gc.getFromLocation(lat, lng, 1);
            StringBuilder sb = new StringBuilder();
            if (addresses.size() > 0) {
                Address add = addresses.get(0);
                for (int k = 0; k < add.getMaxAddressLineIndex()+1; k++)
                    sb.append(add.getAddressLine(k)).append("\n");
            }
            address = sb.toString();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            address = "unknown";
        }

        Log.d(TAG, "got address " + address);
        if (!item.getAddress().equals(address)) {
            item.setAddress(address);
            item.setUpdatedTime(System.currentTimeMillis());
            item.save();
            Log.d(TAG, "post");
            EventBus.getInstance().post(new MyWishChangeEvent());
        }
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount,
                                                     int maxRunCount) {
        // An error occurred in onRun.
        // Return value determines whether this job should retry or cancel. You can further
        // specify a backoff strategy or change the job's priority. You can also apply the
        // delay to the whole group to preserve jobs' running order.
        return RetryConstraint.createExponentialBackoff(runCount, 1000);
    }

    @Override
    protected void onCancel() {
        Log.e(TAG, "onCancel");
        // Job has exceeded retry attempts or shouldReRunOnThrowable() has returned false.
    }
}