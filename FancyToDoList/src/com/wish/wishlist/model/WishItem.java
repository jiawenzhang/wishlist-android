package com.wish.wishlist.model;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

import com.wish.wishlist.db.ItemDBAdapter;
import com.wish.wishlist.util.DateTimeFormatter;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.ContentValues;

public class WishItem {
	private final Context _ctx;
	private long _id = -1;
	private long _storeId;
	private String _storeName;
	private String _name;
	private String _comments;
	private String _desc;
	private String _date;
	private String _picStr;
	private String _fullsizePicPath;
	private int _priority;
	private Bitmap _thumbnail;
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
			String address, int priority) {
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
		//_thumbnail = thumbnail;
	}

	public long getId() {
		return _id;
	}

	public Bitmap getThumbnail() {
		return _thumbnail;
	}

	public void setThumbnail(Bitmap thumnail) {
		_thumbnail = thumnail;
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
	
	public long getLocatonId() {
		ItemDBAdapter mItemDBAdapter = new ItemDBAdapter(_ctx);
		mItemDBAdapter.open();
		long locationId = mItemDBAdapter.getlocationIdbyItemId(_id);
		mItemDBAdapter.close();
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
		Uri uri = _ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
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

	public String getShareMessage() {
		String message;
		String dateTimeStr = getDate();
		String dateTimeStrNew = DateTimeFormatter.getInstance().getDateTimeString(dateTimeStr);
		
		message = getName() + "\n" + 
			dateTimeStrNew + "\n";
		
		// format the price
		String priceStr = getPriceAsString();
		if (priceStr != null) {
			message += ("$" + priceStr + "\n");
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
		
		message += "\n" + "Shared via Beans Wishlist";
		return message;
	}
	
	public long save() {
		ItemDBAdapter mItemDBAdapter = new ItemDBAdapter(_ctx);
		mItemDBAdapter.open();
		if(_id == -1) {
			_id = mItemDBAdapter.addItem(_storeId, _storeName, _name, _desc, _date, _picStr, _fullsizePicPath, 
					_price, _address, _priority);
		}
		else {
			mItemDBAdapter.updateItem(_id, _storeId, _storeName, _name, _desc, _date, _picStr, _fullsizePicPath, 
					_price, _address, _priority);
		}
		mItemDBAdapter.close();
		return _id;
	}
}