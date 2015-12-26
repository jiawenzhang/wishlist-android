package com.wish.wishlist.util;

import android.graphics.Bitmap;

/**
 * Created by jiawen on 2015-12-25.
 */

public class NoTransformation implements com.squareup.picasso.Transformation {
    public NoTransformation() {
    }

    @Override
    public Bitmap transform(final Bitmap source) {
        return source;
    }

    @Override
    public String key() {
        return "NoTransformation";
    }
}
