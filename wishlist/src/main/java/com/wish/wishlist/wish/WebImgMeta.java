package com.wish.wishlist.wish;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jiawen on 2016-01-24.
 */

public class WebImgMeta {
    private static String TAG = "WebImgMeta";
    public String mUrl;
    public int mWidth;
    public int mHeight;

    public WebImgMeta(String url, int w, int h) {
        mUrl = url;
        mWidth = w;
        mHeight = h;
    }

    public String toJSON() {
        JSONArray imageArray = new JSONArray();
        JSONObject imgJson = new JSONObject();
        try {
            imgJson.put("url", mUrl);
            imgJson.put("w", mWidth);
            imgJson.put("h", mHeight);
            imageArray.put(imgJson);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
        Log.d(TAG, imageArray.toString());
        return imageArray.toString();
    }

    public static WebImgMeta fromJSON(final String JSON) {
        try {
            JSONArray jsonArray = new JSONArray(JSON);
            if (jsonArray.length() == 0) {
                return null;
            }
            JSONObject jsonObj = jsonArray.getJSONObject(0);
            return new WebImgMeta(jsonObj.getString("url"), jsonObj.getInt("w"), jsonObj.getInt("h"));
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }
}
