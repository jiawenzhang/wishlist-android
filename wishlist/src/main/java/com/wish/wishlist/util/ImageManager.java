package com.wish.wishlist.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.Resources;

public class ImageManager
{
    private static ImageManager _instance = null;

    private ImageManager() {
    }

    public static ImageManager getInstance() {
        if (_instance == null) {
            _instance = new ImageManager();
        }
        return _instance;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight, boolean fitFullImage) {
        // if fitFullImage is true, the image will be resized so that the whole image will fit into reqWidth * reqHeight
        // in other words, reqWidth*reqHeight is the min rec in which the whole image can be shown
        // if fitFullImage is false, the image can be larger than reqWidth*reqHeight

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (fitFullImage) {
                if (width < height) {
                    inSampleSize = Math.round((float)height / (float)reqHeight);
                } else {
                    inSampleSize = Math.round((float)width / (float)reqWidth);
                }
            }
            else {
                if (width < height) {
                    inSampleSize = Math.round((float)width / (float)reqWidth);
                }
                else {
                    inSampleSize = Math.round((float)height / (float)reqHeight);
                }
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight, boolean fitFullImage) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight, fitFullImage);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static Bitmap decodeSampledBitmapFromFile(String file, int reqWidth, int reqHeight, boolean fitFullImage) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight, fitFullImage);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, options);
    }
}

