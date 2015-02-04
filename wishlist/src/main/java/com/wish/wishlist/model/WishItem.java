package com.wish.wishlist.model;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.ByteArrayOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.util.Log;
import android.database.Cursor;

import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.util.DateTimeFormatter;
import com.wish.wishlist.util.ImageManager;
import android.preference.PreferenceManager;


public class WishItem {
	private static final String TAG = "WishList";
	private final Context _ctx;
	private long _id = -1;
	private long _storeId;
	private String _storeName;
	private String _name;
	private String _comments;
	private String _desc;
	private String _date;
	private String _picStr; //this is a uri
	private String _fullsizePicPath;
	private int _priority;
	private int _complete;
	//private Bitmap _fullsizePhoto;
	//public static final String KEY_PHOTO_URL = "picture";
	private double _price;
//	private long _locationId;
	private double _latitude;
	private double _longitude;
	private String _address;

	public WishItem(Context ctx ,String name) {
		this(ctx, name, null, null);
		Date now = new Date(java.lang.System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
		_date = sdf.format(now);
	}

	public WishItem(Context ctx, String name, String addr) {
		this(ctx, name, null, addr);
		Date now = new Date(java.lang.System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
		_date = sdf.format(now);
	}

	public WishItem(Context ctx, String name, String created, String addr) {
		_ctx = ctx;
		_name = name;
		_date = created;
		_desc = addr;
	}

	public WishItem(Context ctx, long itemId, long storeId, String storeName, String name, String desc, 
			String date, String picStr, String fullsizePicPath, double price, double latitude, double longitude, 
			String address, int priority, int complete) {
		_id = itemId;
		_fullsizePicPath = fullsizePicPath;
		_price = price;
//		_locationId = locationId;
		_latitude = latitude;
		_longitude = longitude;
		_address = address;
		_picStr = picStr;
		_storeId = storeId;
		_storeName = storeName;
		_ctx = ctx;
		_name = name;
		_desc = desc;
		_date = date;
		_priority = priority;
		_complete = complete;
	}

	public long getId() {
		return _id;
	}

	public void setStoreName(String storeName){
		_storeName = storeName;
	}
	
	public String getStoreName(){
		return _storeName;
	}
	
	public long getStoreId(){
		return _storeId;
	}
	
	public void setPrice(double p){
		_price = p;
	}
	
	public double getPrice(){
		return _price;
	}
	
	public String getPriceAsString() {
		if (_price == Double.MIN_VALUE) {
			return null;
		}
		else {
//			String priceStr = String.format("%20.2f", _price);
			DecimalFormat Dec = new DecimalFormat("#.##");
			String priceStr = (Dec.format(_price));

			return priceStr;
		}
	}

    public static String priceStringWithCurrency(String price, Context ctx) {
        String currencySymbol = PreferenceManager.getDefaultSharedPreferences(ctx).getString("currency", "");
        if (currencySymbol.isEmpty()) {
            return price;
        }
        return currencySymbol + " " + price;
    }
	
	public long getLocatonId() {
		ItemDBManager mItemDBManager = new ItemDBManager(_ctx);
		mItemDBManager.open();
		long locationId = mItemDBManager.getlocationIdbyItemId(_id);
		mItemDBManager.close();
		return locationId;
	}
	
	public double getLatitude() {
		return _latitude;
	}
	
	public double getLongitude() {
		return _longitude;
	}
	
	public void setAddress(String add){
		_address = add;
	}
	
	public String getAddress(){
		return _address;
	}

	public String getPriorityStr() {
		return Integer.toString(_priority);
	}
	
	public int getPriority() {
		return _priority;
	}

	public void setPriority(String priority) {
		this._priority = Integer.getInteger(priority);
	}
	
	public int getComplete() {
		return _complete;
	}
	
	public void setComplete(int complete) {
		this._complete = complete;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		this._name = name;
	}

	public String getDate() {
		return _date;
	}

	public void setDate(String date) {
		_date = date;
	}

	public String getDesc() {
		return _desc;
	}

	public void setDesc(String desc) {
		_desc = desc;
	}
	
	public String getComments() {
		return _comments;
	}

	public void setComments(String com) {
		_comments = com;
	}
	
	public String getFullsizePicPath() {
		if (_fullsizePicPath == null) {
			return null;
		}
		
		//need a db migration to remove this stupid check
		else if (_fullsizePicPath.equals(" ")) {
			return null;
		}
		
		else return _fullsizePicPath;
	}

	public Uri getFullsizePicUri() {
		//google+ bug, cannot share image/video with Uri starts with file://
		//workaround is to save the image to mediastore
		ContentValues values = new ContentValues(2);
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
		values.put(MediaStore.Images.Media.DATA, getFullsizePicPath());
		if (getFullsizePicPath() != null) {
			Log.d("fullsizepicpath", getFullsizePicPath());
		}
		Uri uri = _ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		if (uri != null) {
			return uri;
		}

		//we have already inserted the image to the image content provider before, so retrive the uri
		//the uri is constructed by MediaStore.Images.Media.EXTERNAL_CONTENT_URI + rowId
		//the image uri should be saved in the db in future so we don't need to retrive it here
		String[] projection =
		{
			MediaStore.Images.ImageColumns._ID,
//			MediaStore.Images.Media.DATA 
		};
		String selectionClause = MediaStore.Images.Media.DATA + " = ?";
		String[] selectionArgs = {getFullsizePicPath()};
		Cursor c = _ctx.getContentResolver().query (
			MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
			projection,
			selectionClause,
			selectionArgs,
			null
		);

		if (c != null) {
			//Log.d("count", String.valueOf(c.getCount()));
			c.moveToFirst();
			String id = c.getString(0);
			Log.d("id", id);
			//construct the uri of the photo
			uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
		}
		else {
			Log.d(TAG, "cursor is null");
		}

		return uri;
		//return Uri.fromFile(new File(getFullsizePicPath()));
	}

	public String getPicStr() {
		return _picStr;
	}
	
	@Override
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
		String dateString = sdf.format(_date);
		return "(" + dateString + ") " + _name + " " + _desc;
	}

	public String getShareMessage(Boolean facebook) {
		String message;
		if (facebook) {
			//facebook will generate "via Beans Wishlist" automatically
			message = "Shared a wish\n\n";
		}
		else {
			message = "Shared via Beans Wishlist\n\n";
		}
		String dateTimeStr = getDate();
		String dateTimeStrNew = DateTimeFormatter.getInstance().getDateTimeString(dateTimeStr);
		
		//message = getName() + "\n" + dateTimeStrNew + "\n";
		message += getName() + "\n";
		
		// format the price
		String priceStr = getPriceAsString();
		if (priceStr != null) {
            message += priceStringWithCurrency(priceStr, _ctx) + "\n";
		}
		
		//used as a note
		String descrptStr = getDesc();
		if (!descrptStr.equals("")) {
			message += (descrptStr + "\n");
		}
		
		String storeName = getStoreName();
		if (!storeName.equals("")) {
			message += ("At " + getStoreName() + "\n");
		}
		
		String address = getAddress();
		if (!address.equals("unknown") && !address.equals("")) {
			if (storeName.equals("")) {
				address = "At " + address;
			}
			message += (address + "\n");
		}
		
		return message;
	}
	
	public byte[] getPhotoData() {
		int width = 1024;  
		int height = 1024; 
		Bitmap bitmap = null;
		byte[] data = null;
		if (getFullsizePicPath() != null) { 
			bitmap = ImageManager.getInstance().decodeSampledBitmapFromFile(_fullsizePicPath, width, height, true);
			ByteArrayOutputStream photoStream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, photoStream);
			data = photoStream.toByteArray();
		}
		return data;
	}

	public long save() {
		ItemDBManager mItemDBManager = new ItemDBManager(_ctx);
		mItemDBManager.open();
		if(_id == -1) {
			_id = mItemDBManager.addItem(_storeId, _storeName, _name, _desc, _date, _picStr, _fullsizePicPath,
					_price, _address, _priority, _complete);
		}
		else {
			mItemDBManager.updateItem(_id, _storeId, _storeName, _name, _desc, _date, _picStr, _fullsizePicPath,
					_price, _address, _priority, _complete);
		}
		mItemDBManager.close();
		return _id;
	}
}
