package com.wish.wishlist.util;

import android.util.Patterns;

import java.util.ArrayList;
import java.util.regex.Matcher;

/**
 * Created by jiawen on 2016-06-19.
 */
public class Link {
    public static ArrayList<String> extract(String text) {
        ArrayList<String> links = new ArrayList<>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            links.add(url);
        }
        return links;
    }
}
