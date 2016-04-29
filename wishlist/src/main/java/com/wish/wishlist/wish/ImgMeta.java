package com.wish.wishlist.wish;

/**
 * Created by jiawen on 2016-01-24.
 */

public class ImgMeta {
    private static String TAG = "ImgMeta";

    public static String LOC = "loc";
    public static String URL = "url";
    public static String W = "w";
    public static String H = "h";

    // image location
    public static String WEB = "web";
    public static String PARSE = "parse";

    public String mLocation;
    public String mUrl;
    public int mWidth;
    public int mHeight;

    public ImgMeta(String location, String url, int w, int h) {
        mLocation = location;
        mUrl = url;
        mWidth = w;
        mHeight = h;
    }
}
