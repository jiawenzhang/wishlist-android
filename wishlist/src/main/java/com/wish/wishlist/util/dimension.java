package com.wish.wishlist.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.wish.wishlist.WishlistApplication;

/**
 * Created by jiawen on 2015-12-25.
 */
public class dimension {
    public static int dp2px(int dp) {
        Resources r = WishlistApplication.getAppContext().getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public static int screenWidth() {
        Display display = ((WindowManager) WishlistApplication.getAppContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        return screenSize.x;
    }

    public static int screenHeight() {
        Display display = ((WindowManager) WishlistApplication.getAppContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        return screenSize.y;
    }
}

