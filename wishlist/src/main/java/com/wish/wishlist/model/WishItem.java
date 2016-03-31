package com.wish.wishlist.model;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.io.ByteArrayOutputStream;
import java.util.List;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.util.Log;
import android.database.Cursor;

import com.parse.ParseObject;
import com.parse.ParseUser;
import com.wish.wishlist.WishlistApplication;
import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.TagItemDBManager;
import com.wish.wishlist.image.ImageManager;
import com.wish.wishlist.image.PhotoFileCreater;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.wish.WebImgMeta;

import android.preference.PreferenceManager;

import org.json.JSONObject;


public class WishItem implements Parcelable, Comparable<WishItem>, Comparator<WishItem> {
    private static final String TAG = "WishItem";
    public static final int PUBLIC = 0;
    public static final int PRIVATE = 1;

    private long _id = -1;
    private String _object_id;
    private int _access = PUBLIC;
    private String _storeName;
    private String _name;
    private String _comments;
    private String _desc;
    private long _updated_time;
    private String mWebImgMetaJSON;
    private String mParseImgMetaJSON;
    private String _fullsizePicPath;
    private int _priority;
    private int _complete;
    private String _link;
    private double _price;
    private double _latitude;
    private double _longitude;
    private String _address;
    private boolean _deleted;
    private boolean _synced_to_server;

    public final static String PARSE_KEY_OWNDER_ID = "owner_id";
    public final static String PARSE_KEY_TAGS = "tags";
    public final static String PARSE_KEY_IMAGE = "image";
    public final static String PARSE_KEY_IMG_META_JSON = "parse_img_meta_json";

    public WishItem(long itemId, String object_id, int access, String storeName, String name, String desc,
                    long updated_time, String webImgMetaJSON, String parseImgMetaJSON, String fullsizePicPath, double price, double latitude, double longitude,
                    String address, int priority, int complete, String link, boolean deleted, boolean synced_to_server) {
        _id = itemId;
        _object_id = object_id;
        _access = access;
        _fullsizePicPath = fullsizePicPath;
        _price = price;
        _latitude = latitude;
        _longitude = longitude;
        _address = address;
        mWebImgMetaJSON = webImgMetaJSON;
        mParseImgMetaJSON = parseImgMetaJSON;
        _storeName = storeName;
        _name = name;
        _desc = desc;
        _updated_time = updated_time;
        _priority = priority;
        _complete = complete;
        _link = link;
        _deleted = deleted;
        _synced_to_server = synced_to_server;
    }

    public long getId() {
        return _id;
    }

    public String getKey() {
        if (_id == -1) {
            return _object_id;
        }
        return String.valueOf(_id);
    }

    public String getObjectId() {
        return _object_id;
    }
    public void setObjectId(final String object_id) {
        _object_id = object_id;
    }

    public int getAccess() {
        return _access;
    }
    public void setAccess(final int access) {
        _access = access;
    }

    public boolean getDeleted() {
        return _deleted;
    }

    public void setDeleted(boolean value) {
        _deleted = value;
    }

    public boolean getSyncedToServer() {
        return _synced_to_server;
    }

    public void setSyncedToServer(boolean value) {
        _synced_to_server = value;
    }

    public void setStoreName(String storeName) {
        _storeName = storeName;
    }

    public String getStoreName() {
        return _storeName;
    }

    public void setPrice(double p) {
        _price = p;
    }

    public double getPrice() {
        return _price;
    }

    public String getPriceAsString() {
        if (_price == Double.MIN_VALUE) {
            return null;
        } else {
            DecimalFormat Dec = new DecimalFormat("0.00");
            String priceStr = (Dec.format(_price));

            return priceStr;
        }
    }

    public static String priceStringWithCurrency(String price) {
        String currencySymbol = PreferenceManager.getDefaultSharedPreferences(WishlistApplication.getAppContext()).getString("currency", "");
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

    public boolean hasGeoLocation() {
        return (getLatitude() != Double.MIN_VALUE && getLongitude() != Double.MAX_VALUE);
    }

    public void setAddress(String add) {
        _address = add;
    }

    public String getAddress() {
        return _address;
    }

    public void setLatitude(double lat) {
        _latitude = lat;
    }

    public void setLongitude(double lng) {
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

    public String getThumbPicPath() {
        if (getFullsizePicPath() == null) {
            return null;
        }
        return PhotoFileCreater.getInstance().thumbFilePath(getFullsizePicPath());
    }

    public String getPicName() {
        if (getFullsizePicPath() == null) {
            return null;
        }
        return _fullsizePicPath.substring(_fullsizePicPath.lastIndexOf("/") + 1);
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
        Uri uri = WishlistApplication.getAppContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

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
        Cursor c = WishlistApplication.getAppContext().getContentResolver().query (
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

    public String getWebImgMetaJSON() {
        return mWebImgMetaJSON;
    }

    public WebImgMeta getWebImgMeta() {
        if (mWebImgMetaJSON == null) {
            return null;
        }
        return WebImgMeta.fromJSON(mWebImgMetaJSON);
    }

    public String getParseImgMetaJSON() {
        return mParseImgMetaJSON;
    }

    public WebImgMeta getParseImgMeta() {
        if (mParseImgMetaJSON == null) {
            return null;
        }
        return WebImgMeta.fromJSON(mParseImgMetaJSON);
    }

    public void setWebImgMeta(String url, int w, int h) {
        if (url == null) {
            mWebImgMetaJSON = null;
            return;
        }
        mWebImgMetaJSON = new WebImgMeta(url, w, h).toJSON();
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        Date date = new Date(_updated_time);
        String dateString = sdf.format(date);
        return "(" + dateString + ") " + _name + " " + _desc;
    }

    public void clear() {
        // clear the attributes of a wish, called when deleting a wish,
        // so that the attributes in db can be cleared. We will keep the object_id
        // so that server still knows it
        _name = "";
        _desc = "";
        _storeName = "";
        _address = "";
        _link = "";

        removeImage();
        _fullsizePicPath = "";
        mWebImgMetaJSON = null;
        mParseImgMetaJSON = "";
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
            message += priceStringWithCurrency(priceStr) + "\n";
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
        SyncAgent.getInstance().sync();
        return id;
    }

    public long saveToLocal()
    {
        Log.d(TAG, "saveToLocal");
        ItemDBManager manager = new ItemDBManager();
        if (_id == -1) { // new item
            _id = manager.addItem(_object_id, _access, _storeName, _name, _desc, _updated_time, mWebImgMetaJSON, _fullsizePicPath,
                    _price, _address, _latitude, _longitude, _priority, _complete, _link, _deleted, _synced_to_server);
        } else { // existing item
            updateDB();
        }
        return _id;
    }

    private void updateDB()
    {
        ItemDBManager manager = new ItemDBManager();
        manager.updateItem(_id, _object_id, _access, _storeName, _name, _desc, _updated_time, mWebImgMetaJSON, _fullsizePicPath,
                _price, _address, _latitude, _longitude, _priority, _complete, _link, _deleted, _synced_to_server);
    }

    public static WishItem fromParseObject(final ParseObject wishObject, long item_id)
    {
        final String parseImgMetaJSON = wishObject.getString(WishItem.PARSE_KEY_IMG_META_JSON);
        WishItem wishItem = new WishItem(
                item_id,
                wishObject.getObjectId(),
                wishObject.getInt(ItemDBManager.KEY_ACCESS),
                wishObject.getString(ItemDBManager.KEY_STORENAME),
                wishObject.getString(ItemDBManager.KEY_NAME),
                wishObject.getString(ItemDBManager.KEY_DESCRIPTION),
                wishObject.getLong(ItemDBManager.KEY_UPDATED_TIME),
                wishObject.getString(ItemDBManager.KEY_WEB_IMG_META_JSON),
                parseImgMetaJSON,
                null, // _fullsizePhotoPath,
                wishObject.getDouble(ItemDBManager.KEY_PRICE),
                wishObject.getDouble(ItemDBManager.KEY_LATITUDE),
                wishObject.getDouble(ItemDBManager.KEY_LONGITUDE),
                wishObject.getString(ItemDBManager.KEY_ADDRESS),
                0, // priority
                wishObject.getInt(ItemDBManager.KEY_COMPLETE),
                wishObject.getString(ItemDBManager.KEY_LINK),
                wishObject.getBoolean(ItemDBManager.KEY_DELETED),
                true);

        return wishItem;
    }

    public static void toParseObject(final WishItem item, ParseObject wishObject)
    {
        final ParseUser user = ParseUser.getCurrentUser();
        if (user != null) {// user here should never be null
            wishObject.put(WishItem.PARSE_KEY_OWNDER_ID, user.getObjectId());
        }
        wishObject.put(ItemDBManager.KEY_ACCESS, item.getAccess());
        wishObject.put(ItemDBManager.KEY_STORENAME, item.getStoreName());
        wishObject.put(ItemDBManager.KEY_NAME, item.getName());
        wishObject.put(ItemDBManager.KEY_DESCRIPTION, item.getDesc());
        wishObject.put(ItemDBManager.KEY_UPDATED_TIME, item.getUpdatedTime());
        if (item.getWebImgMetaJSON() != null) {
            wishObject.put(ItemDBManager.KEY_WEB_IMG_META_JSON, item.getWebImgMetaJSON());
        } else {
            wishObject.put(ItemDBManager.KEY_WEB_IMG_META_JSON, JSONObject.NULL);
        }

        if (item.getParseImgMetaJSON() != null) {
            wishObject.put(WishItem.PARSE_KEY_IMG_META_JSON, item.getParseImgMetaJSON());
        } else {
            wishObject.put(WishItem.PARSE_KEY_IMG_META_JSON, JSONObject.NULL);
        }

        wishObject.put(ItemDBManager.KEY_PRICE, item.getPrice());
        wishObject.put(ItemDBManager.KEY_LATITUDE, item.getLatitude());
        wishObject.put(ItemDBManager.KEY_LONGITUDE, item.getLongitude());
        wishObject.put(ItemDBManager.KEY_ADDRESS, item.getAddress());
        wishObject.put(ItemDBManager.KEY_COMPLETE, item.getComplete());
        wishObject.put(ItemDBManager.KEY_LINK, item.getLink());
        wishObject.put(ItemDBManager.KEY_DELETED, item.getDeleted());
        List<String> tags = TagItemDBManager.instance().tags_of_item(item.getId());
        wishObject.put(WishItem.PARSE_KEY_TAGS, tags);
    }

    public ParseObject toParseObject()
    {
        final ParseObject wishObject = new ParseObject(ItemDBManager.DB_TABLE);
        // Fixme how to save id?
        // Parse Keys must start with a letter, and can contain alphanumeric characters and underscores
        //wishObject.put("id", _id);

        toParseObject(this, wishObject);
        return wishObject;
    }

    public void removeImage()
    {
        String fullsizePicPath= getFullsizePicPath();
        if (fullsizePicPath != null) {
            File file = new File(fullsizePicPath);
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    Log.d(TAG, "delete " + fullsizePicPath);
                } else {
                    Log.e(TAG, "fail to delete " + fullsizePicPath);
                }
            }
        }

        String thumbPath = getThumbPicPath();
        if (thumbPath != null ) {
            File file = new File(thumbPath);
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    Log.d(TAG, "delete " + thumbPath);
                } else {
                    Log.e(TAG, "fail to delete " + thumbPath);
                }
            }
        }

        setFullsizePicPath(null);
        saveToLocal();
    }

    @Override
    public int compareTo(WishItem w)
    {
        if (getId() != -1) {
            return Long.valueOf(getId()).compareTo(Long.valueOf(w.getId()));
        } else {
            return getObjectId().compareTo(w.getObjectId());
        }
    }

    @Override
    public int compare(WishItem w1, WishItem w2)
    {
        if (w1.getId() != -1 && w2.getId() != -1) {
            return Long.valueOf(w1.getId()).compareTo(Long.valueOf(w2.getId()));
        } else {
            return w1.getObjectId().compareTo(w2.getObjectId());
        }
    }

    /****************** everything below here is for implementing Parcelable *********************/

    // 99.9% of the time you can just ignore this
    public int describeContents() {
        return 0;
    }

    // write object's data to the passed-in Parcel
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(_id);
        out.writeString(_object_id);
        out.writeInt(_access);
        out.writeString(_storeName);
        out.writeString(_name);
        out.writeString(_comments);
        out.writeString(_desc);
        out.writeLong(_updated_time);
        out.writeString(mWebImgMetaJSON);
        out.writeString(mParseImgMetaJSON);
        out.writeString(_fullsizePicPath);
        out.writeInt(_priority);
        out.writeInt(_complete);
        out.writeString(_link);
        out.writeDouble(_price);
        out.writeDouble(_latitude);
        out.writeDouble(_longitude);
        out.writeString(_address);
        out.writeByte((byte) (_deleted ? 1 : 0));
        out.writeByte((byte) (_synced_to_server ? 1 : 0));
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<WishItem> CREATOR = new Parcelable.Creator<WishItem>() {
        public WishItem createFromParcel(Parcel in) {
            return new WishItem(in);
        }

        public WishItem[] newArray(int size) {
            return new WishItem[size];
        }
    };

    private WishItem(Parcel in) {
        // data in Parcel is FIFO, make sure to read data in the same order of write
        _id = in.readLong();
        _object_id = in.readString();
        _access = in.readInt();
        _storeName = in.readString();
        _name = in.readString();
        _comments = in.readString();
        _desc = in.readString();
        _updated_time = in.readLong();
        mWebImgMetaJSON = in.readString();
        mParseImgMetaJSON = in.readString();
        _fullsizePicPath = in.readString();
        _priority = in.readInt();
        _complete = in.readInt();
        _link = in.readString();
        _price = in.readDouble();
        _latitude = in.readDouble();
        _longitude = in.readDouble();
        _address = in.readString();
        _deleted = in.readByte() != 0;
        _synced_to_server = in.readByte() != 0;
    }
}
