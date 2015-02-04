package com.wish.wishlist.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.SimpleCursorAdapter;

import com.etsy.android.grid.util.DynamicHeightImageView;
import com.wish.wishlist.db.ItemDBManager;


public class WishItemStaggeredCursorAdapter extends SimpleCursorAdapter {

    public WishItemStaggeredCursorAdapter(Context context, int layout, Cursor c,
                                          String[] from, int[] to) {
        super(context, layout, c, from, to);
        setViewBinder(new WishItemStaggeredViewBinder());
    }

    public class WishItemStaggeredViewBinder implements ViewBinder {
        public WishItemStaggeredViewBinder() {
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int nImageIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_FULLSIZE_PHOTO_PATH);
            int nPriceIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_PRICE);
            int nStoreNameIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_STORENAME);
            int nAddIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_ADDRESS);
            int nCompleteIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_COMPLETE);

            // set the photo to the image view
            if (columnIndex == nImageIndex) {
                float w;
                float h;

                //get the ImageView in which the photo should be displayed
                DynamicHeightImageView imageView = (DynamicHeightImageView) view;
                String photo_path = cursor.getString(columnIndex);
                if (photo_path == null) {
                    return true;
                }

                // decode the original image into one with width about half the screen width, keep the aspect ratio
                // this will avoid loading the original image into memory, which could be very slow if the image is large.
                Display display = ((WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(photo_path, options);
                final float ratio = (float) options.outHeight / (float) options.outWidth;

                int width = size.x / 2;
                int height = (int) (width * ratio);

                Bitmap bitmap = ImageManager.getInstance().decodeSampledBitmapFromFile(photo_path, width, height, true);

                imageView.setImageBitmap(bitmap);
                imageView.setHeightRatio(ratio);
            }
            return true;
        }
    }
}

