package com.wish.wishlist.util;

import com.parse.ParseUser;
import com.wish.wishlist.R;
import com.wish.wishlist.WishlistApplication;

/**
 * Created by jiawen on 2016-05-23.
 */
public class Owner {
    public static String id() {
        if (!Util.deviceAccountEnabled()) {
            return null;
        }

        if (ParseUser.getCurrentUser() == null) {
            return null;
        }

        return ParseUser.getCurrentUser().getObjectId();
    }
}
