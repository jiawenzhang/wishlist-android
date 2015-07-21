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

import com.parse.ParseObject;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.util.ImageManager;
import com.wish.wishlist.util.sync.SyncAgent;

import android.preference.PreferenceManager;

public class WishItem {
    private static final String TAG = "WishItem";
    private final Context _ctx;
    private long _id = -1;
    private String _storeName;
    private String _name;
    private String _comments;
    private String _desc;
    private long _updated_time;
    private String _picStr; //this is a uri
    private String _fullsizePicPath;
    private int _priority;
    private int _complete;
    private String _link;
    private double _price;
    private double _latitude;
    private double _longitude;
    private String _address;
    private String _object_id;

    public WishItem(Context ctx, long itemId, String object_id, String storeName, String name, String desc,
                    long updated_time, String picStr, String fullsizePicPath, double price, double latitude, double longitude,
                    String address, int priority, int complete, String link) {
        _id = itemId;
        _object_id = object_id;
        _fullsizePicPath = fullsizePicPath;
        _price = price;
        _latitude = latitude;
        _longitude = longitude;
        _address = address;
        _picStr = picStr;
        _storeName = storeName;
        _ctx = ctx;
        _name = name;
        _desc = desc;
        _updated_time = updated_time;
        _priority = priority;
        _complete = complete;
        _link = link;
    }

    public long getId() {
        return _id;
    }

    public String getObjectId() {
        return _object_id;
    }

    public void setObjectId(String object_id) {
        _object_id = object_id;
    }

    public void setStoreName(String storeName){
        _storeName = storeName;
    }

    public String getStoreName(){
        return _storeName;
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
        } else {
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

    public void setLatitude(double lat)
    {
        _latitude = lat;
    }

    public void setLongitude(double lng)
    {
        _longitude = lng;
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

    public String getLink() {
        return _link;
    }

    public void setLink(String link) {
        _link = link;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        this._name = name;
    }

    public long getUpdatedTime() {
        return _updated_time;
    }

    public void setUpdatedTime(long time) {
        _updated_time = time;
    }

    public String getUpdatedTimeStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(_updated_time));
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
        } //need a db migration to remove this stupid check
        else if (_fullsizePicPath.equals(" ")) {
            return null;
        } else {
            return _fullsizePicPath;
        }
    }

    public void setFullsizePicPath(String path) {
        _fullsizePicPath = path;
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
            c.moveToFirst();
            String id = c.getString(0);
            Log.d("id", id);
            //construct the uri of the photo
            uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        } else {
            Log.d(TAG, "cursor is null");
        }

        return uri;
    }

    public String getPicStr() {
        return _picStr;
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        Date date = new Date(_updated_time);
        String dateString = sdf.format(date);
        return "(" + dateString + ") " + _name + " " + _desc;
    }

    public String getShareMessage(Boolean facebook) {
        String message;
        if (facebook) {
            //facebook will generate "via Beans Wishlist" automatically
            message = "Shared a wish\n\n";
        } else {
            message = "Shared via Beans Wishlist\n\n";
        }
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
        Bitmap bitmap;
        byte[] data = null;
        if (getFullsizePicPath() != null) {
            bitmap = ImageManager.getInstance().decodeSampledBitmapFromFile(_fullsizePicPath, width, height, true);
            ByteArrayOutputStream photoStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, photoStream);
            data = photoStream.toByteArray();
        }
        return data;
    }

    public long save()
    {
        long id = saveToLocal();
        SyncAgent.getInstance(_ctx).sync();
        return id;
    }

    public long saveToLocal()
    {
        ItemDBManager manager = new ItemDBManager(_ctx);
        if (_id == -1) { // new item
            _id = manager.addItem(_object_id, _storeName, _name, _desc, _updated_time, _picStr, _fullsizePicPath,
                    _price, _address, _latitude, _longitude, _priority, _complete, _link);
        } else { // existing item
            updateDB();
        }
        return _id;
    }

    private void updateDB()
    {
        ItemDBManager manager = new ItemDBManager(_ctx);
        manager.updateItem(_id, _object_id, _storeName, _name, _desc, _updated_time, _picStr, _fullsizePicPath,
                _price, _address, _latitude, _longitude, _priority, _complete, _link);
    }

    public ParseObject toParseObject()
    {
        final ParseObject wishObject = new ParseObject(ItemDBManager.DB_TABLE);
        // Fixme how to save id?
        // Parse Keys must start with a letter, and can contain alphanumeric characters and underscores
        //wishObject.put("id", _id);

        wishObject.put(ItemDBManager.KEY_STORENAME, _storeName);
        wishObject.put(ItemDBManager.KEY_NAME, _name);
        wishObject.put(ItemDBManager.KEY_DESCRIPTION, _desc);
        wishObject.put(ItemDBManager.KEY_UPDATED_TIME, _updated_time);
        wishObject.put(ItemDBManager.KEY_PRICE, _price);
        wishObject.put(ItemDBManager.KEY_LATITUDE, _latitude);
        wishObject.put(ItemDBManager.KEY_LONGITUDE, _longitude);
        wishObject.put(ItemDBManager.KEY_ADDRESS, _address);
        wishObject.put(ItemDBManager.KEY_COMPLETE, _complete);
        wishObject.put(ItemDBManager.KEY_LINK, _link);

        return wishObject;
    }
}
