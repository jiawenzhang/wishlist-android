package com.wish.wishlist.wish;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by jiawen on 2016-01-24.
 */

public class ImgMetaArray {
    private static String TAG = "ImgMetaArray";

    private ArrayList<ImgMeta> mImgMetaArray = new ArrayList<>();

    public ImgMetaArray(ImgMeta meta) {
        mImgMetaArray.add(meta);
    }

    public ImgMetaArray(ArrayList<ImgMeta> metaArray) {
        mImgMetaArray = metaArray;
    }

    public String toJSON() {
        JSONArray imageArray = new JSONArray();

        for (ImgMeta meta : mImgMetaArray) {
            JSONObject imgJson = new JSONObject();
            try {
                imgJson.put(ImgMeta.LOC, meta.mLocation);
                imgJson.put(ImgMeta.URL, meta.mUrl);
                imgJson.put(ImgMeta.W, meta.mWidth);
                imgJson.put(ImgMeta.H, meta.mHeight);
                imageArray.put(imgJson);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return null;
            }
            Log.d(TAG, imageArray.toString());
        }
        return imageArray.toString();
    }

    public static ArrayList<ImgMeta> fromJSON(final String JSON) {
        try {
            JSONArray jsonArray = new JSONArray(JSON);
            if (jsonArray.length() == 0) {
                return null;
            }
            ArrayList<ImgMeta> imgMetaArray = new ArrayList<>();
            for (int i=0; i<jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                imgMetaArray.add(
                        new ImgMeta(
                        jsonObj.getString(ImgMeta.LOC),
                        jsonObj.getString(ImgMeta.URL),
                        jsonObj.getInt(ImgMeta.W),
                        jsonObj.getInt(ImgMeta.H)));
            }
            return imgMetaArray;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }
}
