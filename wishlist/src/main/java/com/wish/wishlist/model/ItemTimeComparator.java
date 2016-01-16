package com.wish.wishlist.model;

import java.util.Comparator;

/**
 * Created by jiawen on 15-11-11.
 */
public class ItemTimeComparator implements Comparator<WishItem> {
    @Override
    public int compare(WishItem o1, WishItem o2) {
        // Sort by time, most recent first
        return Long.valueOf(o2.getUpdatedTime()).compareTo(Long.valueOf(o1.getUpdatedTime()));
    }
}
