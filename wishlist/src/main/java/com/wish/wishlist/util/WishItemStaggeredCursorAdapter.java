package com.wish.wishlist.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
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

                Bitmap bitmap = BitmapFactory.decodeFile(photo_path);

                w = bitmap.getWidth();
                h = bitmap.getHeight();
                imageView.setImageBitmap(bitmap);
                Log.v("setViewValue", "h/w " + h / w);
                imageView.setHeightRatio(h / w);
            }
            return true;
        }
    }
}

