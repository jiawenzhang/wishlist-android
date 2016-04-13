package com.wish.wishlist.util;

/**
 * Created by jiawen on 2016-03-11.
 */
public class DoubleUtil {
    public static boolean compare(Double d1, Double d2) {
        return (d1 == null ? d2 == null : d1.equals(d2));
    }
}
