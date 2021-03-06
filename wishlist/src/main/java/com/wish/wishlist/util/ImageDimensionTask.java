package com.wish.wishlist.util;

import android.os.AsyncTask;
import android.util.Log;

import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.model.Dimension;


import java.util.ArrayList;

import okhttp3.OkHttpClient;

public class ImageDimensionTask extends AsyncTask<ArrayList<WebImage>, Integer, Void> {
    private final String TAG = "ImageDimensionTask";
    private OnImageDimension listener;

    public interface OnImageDimension {
        void onImageDimension(WebImage webImage, int count, int total);
    }

    public ImageDimensionTask(OnImageDimension listener) {
        super();

        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected Void doInBackground(ArrayList<WebImage>... input) {
        long startTime = System.currentTimeMillis();
        ArrayList<WebImage> webImages = input[0];

        Log.d(TAG, "extracting dimension of " + webImages.size() + " images");

        int count = 0;
        OkHttpClient client = new OkHttpClient();
        int total = webImages.size();
        for (int i = 0; i< webImages.size(); i++) {
            Dimension d = ImageDimension.extractImageDimension(webImages.get(i).mUrl, client);
            WebImage w = null;
            if (d != null && d.getWidth() >= 100 && d.getHeight() >= 100) {
                count++;
                w = webImages.get(i);
                w.mWidth = d.getWidth();
                w.mHeight = d.getHeight();
            } else {
                total--;
            }
            listener.onImageDimension(w, count, total);
        }

        Log.d(TAG, "got " + total + " image dimension, took " + (System.currentTimeMillis() - startTime) + " ms");
        return null;
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "onCancelled");
        super.onCancelled();
    }
}

