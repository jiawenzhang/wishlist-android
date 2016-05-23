package com.wish.wishlist.util;

import com.parse.ParseUser;

/**
 * Created by jiawen on 2016-05-23.
 */
public class Owner {
    public static String id() {
        if (ParseUser.getCurrentUser() == null) {
            return null;
        }
        return ParseUser.getCurrentUser().getObjectId();
    }
}
