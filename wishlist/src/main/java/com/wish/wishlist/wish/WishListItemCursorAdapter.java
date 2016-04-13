package com.wish.wishlist.wish;
import java.io.File;
import java.text.DecimalFormat;

import com.squareup.picasso.Picasso;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.model.WishItem;
import com.wish.wishlist.image.PhotoFileCreater;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class WishListItemCursorAdapter extends SimpleCursorAdapter {
    public WishListItemCursorAdapter(Context context, int layout, Cursor c,
                                     String[] from, int[] to) {
        super(context, layout, c, from, to);
        setViewBinder(new WishListItemViewBinder());
    }

    /***
     * WishListItemViewBinder defines how the item's photo and updated_time are displayed in
     * the view.
     *
     * It retrieves the image file from the picture_uri saved in database and set the image
     * to the view
     *
     * It retrieves the updated_time from the database and converts it to "July 6, 1983" format
     * for display in the view
     */
    public class WishListItemViewBinder implements SimpleCursorAdapter.ViewBinder {

        boolean _hasStoreName = false;
        int _photoWidth;

        public WishListItemViewBinder() {
            //we show 3  or 4 columns of photo in grid view, so photo width should be 1/3 of screen width
            int columnCount = 3;
            if (Resources.getSystem().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                columnCount = 4;
            }

            int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            _photoWidth = screenWidth / columnCount;
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int nImageIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_FULLSIZE_PHOTO_PATH);
//			int nDateIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_DATE_TIME);
            int nPriceIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_PRICE);
            int nStoreNameIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_STORENAME);
            int nAddIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_ADDRESS);
            int nCompleteIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_COMPLETE);

            // set the photo to the image view
            if (columnIndex == nImageIndex) {
                ImageView imageView = (ImageView) view;
                String photo_path = cursor.getString(columnIndex);
                if (photo_path == null) {
                    imageView.setVisibility(View.GONE);
                    return true;
                }
                imageView.setVisibility(View.VISIBLE);
                String thumb_path = PhotoFileCreater.getInstance().thumbFilePath(photo_path);
                Picasso.with(view.getContext()).load(new File(thumb_path)).fit().centerCrop().into(imageView);
                return true;
            }

//			// set date and time to the text view in appropriate format
//			if (columnIndex == nDateIndex) {
//				
//				//get the TextView in which the date and time will be displayed
//				TextView viewDate = (TextView) view;
//				
//				//get the updated_time string from db and reformat it
//				String dateTimeStr = cursor.getString(columnIndex);
//				SimpleDateFormat sdfFrom = new SimpleDateFormat("yyyy-MM-dd");
//				SimpleDateFormat sdfTo = new SimpleDateFormat("MMM dd, yyyy");
//
//				String dateTimeStrNew = null;
//				try {
//					dateTimeStrNew = sdfTo.format(sdfFrom.parse(dateTimeStr));
//				} catch (ParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//				//set the reformatted updated_time
//				viewDate.setText(dateTimeStrNew);
//				return true;
//			}

            else if (columnIndex == nPriceIndex) {
                TextView viewPrice = (TextView) view;
                // format the price
                //	String priceStr = item.getPriceAsString();
                //	if (priceStr != null) {
                //		viewPrice.setText("$" + priceStr);
                //		viewPrice.setVisibility(View.VISIBLE);
                //	}
                //	else {
                //		viewPrice.setVisibility(View.GONE);
                //	}

                Double price = cursor.isNull(columnIndex) ? null : cursor.getDouble(columnIndex);
                //we use float.min_value to indicate price is not available
                if (price != null) {
                    DecimalFormat Dec = new DecimalFormat("0.00");
                    String priceStr = (Dec.format(price));
                    viewPrice.setText(WishItem.priceStringWithCurrency(priceStr));
                    viewPrice.setVisibility(View.VISIBLE);
                } else {
                    viewPrice.setVisibility(View.GONE);
                }
                return true;
            }
            else if (columnIndex == nStoreNameIndex){
                TextView viewStore = (TextView) view;
                //	String storeName = item.getStoreName();
                //	if (!storeName.equals("")) {
                //		_hasStoreName = true;
                //		storeName = "At " + storeName;
                //		viewStore.setText(storeName);
                //		viewStore.setVisibility(View.VISIBLE);
                //	}
                //	else {
                //		viewStore.setVisibility(View.GONE);
                //	}

                String storeName = cursor.getString(columnIndex);
                if (!storeName.equals("")) {
                    _hasStoreName = true;
                    viewStore.setText(storeName);
                    viewStore.setVisibility(View.VISIBLE);
                }
                else {
                    viewStore.setVisibility(View.GONE);
                }
                return true;
            }
            else if (columnIndex == nAddIndex) {
                TextView viewAddress = (TextView) view;
                //	String address = item.getAddress();
                //	if (!address.equals("unknown") && !address.equals("")) {
                //		if (!_hasStoreName) {
                //			address = "At " + address;
                //		}
                //		viewAddress.setText(address);
                //		viewAddress.setVisibility(View.VISIBLE);
                //	}
                //	else {
                //		viewAddress.setVisibility(View.GONE);
                //	}

                String Address = cursor.getString(columnIndex);
                if (!Address.equals("unknown") && !Address.equals("")) {
                    if (!_hasStoreName) {
                        Address = "At " + Address;
                    }
                    viewAddress.setText(Address);
                    viewAddress.setVisibility(View.VISIBLE);
                }
                else {
                    viewAddress.setVisibility(View.GONE);
                }
                return true;
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
            return false;
        }
    }
}

