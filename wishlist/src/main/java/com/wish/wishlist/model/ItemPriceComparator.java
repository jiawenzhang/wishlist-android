package com.wish.wishlist.model;

import java.util.Comparator;

/**
 * Created by jiawen on 15-11-11.
 */
public class ItemPriceComparator implements Comparator<WishItem> {
    @Override
    public int compare(WishItem o1, WishItem o2) {
        Double d1 = o1.getPrice();
        Double d2 = o2.getPrice();

        if (d1 == null && d2 == null) {
            return 0;
        }

        if (d1 == null) {
            // d1 < d2
            return -1;
        }

        if (d2 == null) {
            // d1 > d2
            return 1;
        }

        return Double.compare(o1.getPrice(), o2.getPrice());
    }
}
