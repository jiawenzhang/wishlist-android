package com.wish.wishlist.util;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by jiawen on 2016-03-11.
 */
public class StringUtil {
    public static boolean sameArrays(ArrayList<String> arr1, ArrayList<String> arr2) {
        if (arr1.size() != arr2.size()) {
            return false;
        }
        HashSet<String> set1 = new HashSet<>(arr1);
        HashSet<String> set2 = new HashSet<>(arr2);
        return set1.equals(set2);
    }
}
