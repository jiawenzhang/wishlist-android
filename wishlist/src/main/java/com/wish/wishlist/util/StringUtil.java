package com.wish.wishlist.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

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

    public static boolean compare(String str1, String str2) {
        return (str1 == null ? str2 == null : str1.equals(str2));
    }

    public static String UTCDate(Date date) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        return fmt.format(date);
    }
}
