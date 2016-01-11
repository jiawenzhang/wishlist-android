package com.wish.wishlist.util;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.wish.wishlist.WishlistApplication;

/**
 * Created by jiawen on 2016-01-09.
 */
public class ImageTinter {
    private static ImageTinter ourInstance = new ImageTinter();

    public static ImageTinter getInstance() {
        return ourInstance;
    }

    private ImageTinter() {
    }

    public Drawable tint(int resId) {
        Drawable drawable = ContextCompat.getDrawable(WishlistApplication.getAppContext(), resId);
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, Color.BLACK);

        // SRC_IN replace the original color instead of mixing with it
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
        return drawable;
    }
}
