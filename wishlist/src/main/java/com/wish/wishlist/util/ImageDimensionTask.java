package com.wish.wishlist.util;

import android.os.AsyncTask;
import android.util.Log;

import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.model.Dimension;


import java.util.ArrayList;

public class ImageDimensionTask extends AsyncTask<ArrayList<WebImage>, Integer, ArrayList<WebImage>> {
    private final String TAG = "ImageDimensionTask";
    private OnImageDimension listener;
    private long startTime;

    public interface OnImageDimension {
        void onImageDimension(ArrayList<WebImage> webImages);
    }

    public ImageDimensionTask(OnImageDimension listener) {
        super();

        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected ArrayList<WebImage> doInBackground(ArrayList<WebImage>... input) {
        startTime = System.currentTimeMillis();
        ArrayList<WebImage> webImages = input[0];

        Log.d(TAG, "extracting dimension of " + webImages.size() + " images");
        ArrayList<WebImage> newWebImages = new ArrayList<>();
        for (int i = 0; i< webImages.size(); i++) {
            Dimension d = ImageDimension.extractImageDimension(webImages.get(i).mUrl);
            if (d != null && d.getWidth() >= 100 && d.getHeight() >= 100) {
                WebImage w = webImages.get(i);
                w.mWidth = d.getWidth();
                w.mHeight = d.getHeight();
                newWebImages.add(w);
            }
        }

        Log.d(TAG, "extracted dimension of " + newWebImages.size() + " images");
        return newWebImages;
    }

    protected void onProgressUpdate(Integer... progress) {}

    protected void onPostExecute(ArrayList<WebImage> webImages) {
        listener.onImageDimension(webImages);
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "onCancelled");
        super.onCancelled();
    }
}

