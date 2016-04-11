package com.wish.wishlist.wish;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jiawen on 2016-01-24.
 */

public class ImgMeta {
    private static String TAG = "ImgMeta";

    private static String LOC = "loc";
    private static String URL = "url";
    private static String W = "w";
    private static String H = "h";

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

    public String toJSON() {
        JSONArray imageArray = new JSONArray();
        JSONObject imgJson = new JSONObject();
        try {
            imgJson.put(LOC, mLocation);
            imgJson.put(URL, mUrl);
            imgJson.put(W, mWidth);
            imgJson.put(H, mHeight);
            imageArray.put(imgJson);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
        Log.d(TAG, imageArray.toString());
        return imageArray.toString();
    }

    public static ImgMeta fromJSON(final String JSON) {
        try {
            JSONArray jsonArray = new JSONArray(JSON);
            if (jsonArray.length() == 0) {
                return null;
            }
            JSONObject jsonObj = jsonArray.getJSONObject(0);
            return new ImgMeta(jsonObj.getString(LOC), jsonObj.getString(URL), jsonObj.getInt(W), jsonObj.getInt(H));
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }
}
