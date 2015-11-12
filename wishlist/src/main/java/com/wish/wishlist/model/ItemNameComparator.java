package com.wish.wishlist.model;

import java.util.Comparator;

/**
 * Created by jiawen on 15-11-11.
 */
public class ItemNameComparator implements Comparator<WishItem> {
    @Override
    public int compare(WishItem o1, WishItem o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
