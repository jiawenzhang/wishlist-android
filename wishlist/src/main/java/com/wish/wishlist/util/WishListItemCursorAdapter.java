package com.wish.wishlist.util;
import java.text.DecimalFormat;

import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.model.WishItem;
//import com.wish.wishlist.model.WishItemManager;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.util.Log;

public class WishListItemCursorAdapter extends SimpleCursorAdapter {
	static final String TAG = "WishList";
	public WishListItemCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
		setViewBinder(new WishListItemViewBinder());
	}
	
	/***
	 * WishListItemViewBinder defines how the item's photo and date_time are displayed in 
	 * the view. 
	 * 
	 * It retrieves the image file from the picture_uri saved in database and set the image
	 * to the view
	 * 
	 * It retrieves the date_time from the database and converts it to "July 6, 1983" format
	 * for display in the view 
	 */
	public class WishListItemViewBinder implements SimpleCursorAdapter.ViewBinder {
		
		boolean _hasStoreName = false;
		int _photoWidth;
		int _columnSpace;

		public WishListItemViewBinder() {
			//we show 3 columes of photo in gridview, so photo width should be 1/3 of screen width
            int columnCount = 3;
            if (Resources.getSystem().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                columnCount = 4;
            }

			int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
			int columnSpace = screenWidth / 100;
			//_photoWidth = (screenWidth - columnCount * columnSpace) / 3;
			_photoWidth = screenWidth / columnCount;
			Log.d(TAG, "screen width" + String.valueOf(screenWidth));
			Log.d(TAG, "photo width" + String.valueOf(_photoWidth));
		}

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			//int nIdIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_ID);
			//long id = cursor.getLong(nIdIndex);
			//WishItem item = WishItemManager.getInstance(_ctx).retrieveItembyId(id);

			int nImageIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_PHOTO_URL);
//			int nDateIndex = cursor
//					.getColumnIndexOrThrow(ItemDBManager.KEY_DATE_TIME);
			int nPriceIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_PRICE);
			
			int nStoreNameIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_STORENAME);

			int nAddIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_ADDRESS);

			int nCompleteIndex = cursor.getColumnIndexOrThrow(ItemDBManager.KEY_COMPLETE);
			
			// set the photo to the image view
			if (columnIndex == nImageIndex) {
				Bitmap bitmap = null;
				
				//get the ImageView in which the photo should be displayed
				ImageView photoView = (ImageView) view;
				photoView.setLayoutParams(new LinearLayout.LayoutParams(_photoWidth, _photoWidth));
				photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);

				// read in the picture string from db
				// the string could be a resource id or a uri from content provider
				String pic_str = cursor.getString(columnIndex);
				//String pic_str = item.getPicStr();
				//check if pic_str is null, which user added this item without taking a pic.
				if (pic_str == null) {
					//do nothing
					return true;
				}
				
				// check if pic_str is a resId or a uri
				try {
					//if it's a resID, the following decodeResource will not 
					//throw exception (this need to be changed for performance)
					int picResId = Integer.valueOf(pic_str, 16).intValue();
					Log.d(TAG, pic_str);
					bitmap = BitmapFactory.decodeResource(view.getContext().getResources(), picResId);
				} catch (NumberFormatException e) {
					// Not a resId, so it must be a content provider uri
					// thus set image from uri (not a photo taken by cameara, but
					// a photo we manually added to the drawable folder
					Uri pic_uri = Uri.parse(pic_str);
					photoView.setImageURI(pic_uri);
					return true;
//					
//					picUrl = Integer.toHexString(R.drawable.mini_cooper);
//					bitmap = BitmapFactory.decodeResource(view.getContext()
//							.getResources(), R.drawable.mini_cooper);
				}

				// exception is not thrown, so it is resID.
				// set the image decoded from resID
				photoView.setImageBitmap(bitmap);

				return true;
			}

//			// set date and time to the text view in appropriate format
//			if (columnIndex == nDateIndex) {
//				
//				//get the TextView in which the date and time will be displayed
//				TextView viewDate = (TextView) view;
//				
//				//get the date_time string from db and reformat it
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
//				//set the reformatted date_time
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

				double price = cursor.getDouble(columnIndex);
				//we use float.min_value to indicate price is not available
				if (price != Double.MIN_VALUE) {
					DecimalFormat Dec = new DecimalFormat("0.00");
					String priceStr = (Dec.format(price));
					viewPrice.setText(WishItem.priceStringWithCurrency(priceStr, view.getContext()));
					viewPrice.setVisibility(View.VISIBLE);
				}
				else {
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
					storeName = "At " + storeName;
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
//			return true;
		}

	}

}

