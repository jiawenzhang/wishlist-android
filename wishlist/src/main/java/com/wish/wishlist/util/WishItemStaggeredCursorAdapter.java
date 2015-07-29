package com.wish.wishlist.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

import com.etsy.android.grid.util.DynamicHeightImageView;
import com.etsy.android.grid.util.DynamicHeightTextView;
import com.squareup.picasso.Picasso;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.util.camera.PhotoFileCreater;

import java.io.File;
import java.text.DecimalFormat;


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
            int nNameIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_NAME);
            int nImageIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_FULLSIZE_PHOTO_PATH);
            int nDescriptionIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_DESCRIPTION);
            int nPriceIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_PRICE);
            int nStoreNameIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_STORENAME);
            int nAddIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_ADDRESS);
            int nCompleteIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_COMPLETE);

            // set the photo to the image view
            if (columnIndex == nImageIndex) {
                //get the ImageView in which the photo should be displayed
                DynamicHeightImageView imageView = (DynamicHeightImageView) view;
                String photo_path = cursor.getString(columnIndex);
                if (photo_path == null) {
                    imageView.setVisibility(View.GONE);
                    return true;
                }
                imageView.setVisibility(View.VISIBLE);

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

                String thumb_path = PhotoFileCreater.getInstance().thumbFilePath(photo_path);
                Picasso.with(view.getContext()).load(new File(thumb_path)).resize(width, height).into(imageView);
                imageView.setHeightRatio(ratio);
            }
            else if (columnIndex == nNameIndex) {
                String name = cursor.getString(columnIndex);
                DynamicHeightTextView textView = (DynamicHeightTextView) view;
                textView.setText(name);
            }
            else if (columnIndex == nPriceIndex) {
                DynamicHeightTextView viewPrice = (DynamicHeightTextView) view;

                double price = cursor.getDouble(columnIndex);
                //we use float.min_value to indicate price is not available
                if (price != Double.MIN_VALUE) {
                    DecimalFormat Dec = new DecimalFormat("0.00");
                    String priceStr = (Dec.format(price));
                    viewPrice.setText(WishItem.priceStringWithCurrency(priceStr));
                    viewPrice.setVisibility(View.VISIBLE);
                }
                else {
                    viewPrice.setVisibility(View.GONE);
                }
            }
            else if (columnIndex == nDescriptionIndex ) {
                String photo_path = cursor.getString(nImageIndex);
                String description = cursor.getString(columnIndex);
                // show description up to 3 lines if we don't have a photo to show
                if (photo_path == null && !description.isEmpty()) {
                    DynamicHeightTextView textView = (DynamicHeightTextView) view;
                    textView.setText(description);
                    textView.setEllipsize(TextUtils.TruncateAt.END);
                    textView.setVisibility(View.VISIBLE);
                } else {
                    DynamicHeightTextView textView = (DynamicHeightTextView) view;
                    textView.setVisibility(View.GONE);
                }
            }
            else if (columnIndex == nCompleteIndex) {
                ImageView viewComplete = (ImageView) view;
                int complete = cursor.getInt(columnIndex);
                if (complete == 1) {
                    viewComplete.setVisibility(View.VISIBLE);
                }
                else {
                    viewComplete.setVisibility(View.GONE);
                }
                return true;
            }
            return true;
        }
    }
}

