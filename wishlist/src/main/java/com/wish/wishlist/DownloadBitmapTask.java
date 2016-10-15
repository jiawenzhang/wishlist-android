package com.wish.wishlist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.wish.wishlist.activity.WebImage;
import com.wish.wishlist.util.dimension;

import java.util.ArrayList;


/**
 * Created by jiawen on 2016-10-14.
 */

public class DownloadBitmapTask {
    private final static String TAG = "DownloadBitmapTask";
    private ArrayList<String> imageUrls;
    public ArrayList<WebImage> webImages = new ArrayList<>();
    private Context context;
    private Target target;
    private WebImagesListener listener;

    public interface WebImagesListener {
        void gotWebImages(ArrayList<WebImage> webImages);
    }

    public DownloadBitmapTask(
            ArrayList<String> imgUrls,
            Context context,
            WebImagesListener listener) {
        this.imageUrls = imgUrls;
        this.context = context;
        this.listener = listener;
    }

    public void execute() {
        scaleDownBitmap(imageUrls.get(0));
    }

    private void scaleDownBitmap(final String url) {
        final int maxImageWidth = dimension.screenWidth() / 2;
        final int maxImageHeight = dimension.screenHeight() / 2;

        target = new Target() {
            private void nextBitmap() {
                int index = imageUrls.indexOf(url);
                if (index < imageUrls.size() - 1) {
                    scaleDownBitmap(imageUrls.get(index + 1));
                } else {
                    // we have tried all the urls in result.imageUrls but still fail to get a bitmap
                    // Fixme: shall we proceed to next stage?
                    Log.d(TAG, "tried all, no valid bitmap");
                    listener.gotWebImages(webImages);
                }
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (bitmap != null) {
                    Log.d(TAG, "got valid bitmap");
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();

                    if (w < 100 || h < 100) {
                        Log.d(TAG, "bitmap width < 100 or height < 100, skip");
                        nextBitmap();
                        return;
                    }

                    if (w / h > 8 || h / w > 8) {
                        Log.d(TAG, "bitmap aspect ratio > 8, skip");
                        nextBitmap();
                        return;
                    }

                    webImages.add(new WebImage(url, bitmap.getWidth(), bitmap.getHeight(), "", bitmap));
                    //Log.e(TAG, stage + " loading: Time: " + (System.currentTimeMillis() - startTime));
                    //printResult(result);
                    listener.gotWebImages(webImages);
                } else {
                    Log.e(TAG, "null bitmap");
                    nextBitmap();
                }
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.e(TAG, "onBitmapFailed");
                nextBitmap();
            }
        };

        //final Bitmap image = Picasso.with(mContext).load(src).resize(imageWidth, 0).centerInside().onlyScaleDown().get();
        // onlyScaleDown() has no effect when working together with resize(targetWidth, 0) on Android 5.1, 6.0
        // it works on Android 4.4

        // workaround the issue by using centerInside
        Picasso.with(context).load(url).resize(maxImageWidth, maxImageHeight).centerInside().onlyScaleDown().into(target);
    }
}
