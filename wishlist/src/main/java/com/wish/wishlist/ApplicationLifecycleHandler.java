package com.wish.wishlist;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.wish.wishlist.activity.ActivityBase;
import com.wish.wishlist.activity.SplashActivity;
import com.wish.wishlist.util.Link;
import com.wish.wishlist.wish.AddWishFromLinkActivity;

import java.util.ArrayList;

/**
 * Created by jiawen on 2016-06-19.
 */
class ApplicationLifecycleHandler implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private static final String TAG = ApplicationLifecycleHandler.class.getSimpleName();
    private static boolean isInBackground = true;

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    private void performClipboardCheck(ActivityBase activityBase) {
        ClipboardManager cb = (ClipboardManager) WishlistApplication.getAppContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (!cb.hasPrimaryClip()) {
            Log.d(TAG, "no clip");
            return;
        }

        if (!(cb.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {
            Log.d(TAG, "no TEXT/PLAIN clip");
            return;
        }

        ClipData cd = cb.getPrimaryClip();
        if (cd == null || cd.getItemCount() == 0) {
            return;
        }

        ClipData.Item item = cd.getItemAt(0);
        CharSequence text = item.coerceToText(activityBase);
        if (text == null) {
            return;
        }

        String textString = text.toString();
        ArrayList<String> links = Link.extract(textString);
        if (links.isEmpty()) {
            Log.d(TAG, "no link in clipboard");
            return;
        }

        // clear the clipboard so snackbar won't show up again next time
        ClipData clipData = ClipData.newPlainText("", "");
        cb.setPrimaryClip(clipData);
        activityBase.showLinkSnack(links);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isInBackground) {
            if ((activity instanceof SplashActivity) ||
                 activity instanceof AddWishFromLinkActivity) {
                Log.d(TAG, "ignore activity");
                return;
            }

            Log.d(TAG, "app come to foreground");
            isInBackground = false;
            try {
                ActivityBase activityBase = (ActivityBase) activity;
                performClipboardCheck(activityBase);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int i) {
        if (i >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            Log.d(TAG, "app went to background");
            isInBackground = true;
        }
    }
}
