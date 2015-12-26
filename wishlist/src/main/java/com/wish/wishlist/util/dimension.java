package com.wish.wishlist.util;

import android.content.res.Resources;
import android.util.TypedValue;

import com.wish.wishlist.WishlistApplication;

/**
 * Created by jiawen on 2015-12-25.
 */
public class dimension {
    public static int dp2px(int dp) {
        Resources r = WishlistApplication.getAppContext().getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }
}

