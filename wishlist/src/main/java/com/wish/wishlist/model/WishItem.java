package com.wish.wishlist.model;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
import com.wish.wishlist.wish.ImgMeta;

import android.preference.PreferenceManager;

import org.json.JSONObject;


public class WishItem implements Parcelable {
    private static final String TAG = "WishItem";
    public static final int PUBLIC = 0;
    public static final int PRIVATE = 1;

    private long mId = -1;
    private String mObjectId;
    private int mAccess = PUBLIC;
    private String mStoreName;
    private String mName;
    private String mComments;
    private String mDescription;
    private long mUpdatedTime;
    private String mImgMetaJSON;
    private String mFullsizePicPath;
    private int mPriority;
    private int mComplete;
    private String mLink;
    private Double mPrice;
    private Double mLatitude;
    private Double mLongitude;
    private String mAddress;
    private boolean mDeleted;
    private boolean mSyncedToServer;
    private boolean mDownloadImg;

    public final static String PARSE_KEY_OWNDER_ID = "owner_id";
    public final static String PARSE_KEY_TAGS = "tags";
    public final static String PARSE_KEY_IMAGE = "image";

    public WishItem(
            long itemId,
            String object_id,
            int access,
            String storeName,
            String name,
            String desc,
            long updated_time,
            String imgMetaJSON,
            String fullsizePicPath,
            Double price,
            Double latitude,
            Double longitude,
            String address,
            int priority,
            int complete,
            String link,
            boolean deleted,
            boolean synced_to_server,
            boolean download_img) {
        mId = itemId;
        mObjectId = object_id;
        mAccess = access;
        mFullsizePicPath = fullsizePicPath;
        mPrice = price;
        mLatitude = latitude;
        mLongitude = longitude;
        mAddress = address;
        mImgMetaJSON = imgMetaJSON;
        mStoreName = storeName;
        mName = name;
        mDescription = desc;
        mUpdatedTime = updated_time;
        mPriority = priority;
        mComplete = complete;
        mLink = link;
        mDeleted = deleted;
        mSyncedToServer = synced_to_server;
        mDownloadImg = download_img;
    }

    public long getId() {
        return mId;
    }

    public String getKey() {
        if (mId == -1) {
            return mObjectId;
        }
        return String.valueOf(mId);
    }

    public String getObjectId() {
        return mObjectId;
    }
    public void setObjectId(final String object_id) {
        mObjectId = object_id;
    }

    public int getAccess() {
        return mAccess;
    }
    public void setAccess(final int access) {
        mAccess = access;
    }

    public boolean getDeleted() {
        return mDeleted;
    }

    public void setDeleted(boolean value) {
        mDeleted = value;
    }

    public boolean getSyncedToServer() {
        return mSyncedToServer;
    }

    public void setSyncedToServer(boolean value) {
        mSyncedToServer = value;
    }

    public boolean getDownloadImg() {
        return mDownloadImg;
    }

    public void setDownloadImg(boolean value) {
        mDownloadImg = value;
    }

    public void setStoreName(String storeName) {
        mStoreName = storeName;
    }

    public String getStoreName() {
        return mStoreName;
    }

    public void setPrice(Double p) {
        mPrice = p;
    }

    public Double getPrice() {
        return mPrice;
    }

    public String getPriceAsString() {
        if (mPrice == null) {
            return null;
        } else {
            DecimalFormat Dec = new DecimalFormat("0.00");
            String priceStr = (Dec.format(mPrice));

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

    public Double getLatitude() {
        return mLatitude;
    }

    public Double getLongitude() {
        return mLongitude;
    }

    public boolean hasGeoLocation() {
        return (getLatitude() != null && getLongitude() != null);
    }

    public void setAddress(String add) {
        mAddress = add;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setLatitude(Double lat) {
        mLatitude = lat;
    }

    public void setLongitude(Double lng) {
        mLongitude = lng;
    }

    public String getPriorityStr() {
        return Integer.toString(mPriority);
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(String priority) {
        this.mPriority = Integer.getInteger(priority);
    }

    public int getComplete() {
        return mComplete;
    }

    public void setComplete(int complete) {
        this.mComplete = complete;
    }

    public String getLink() {
        return mLink;
    }

    public void setLink(String link) {
        mLink = link;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public long getUpdatedTime() {
        return mUpdatedTime;
    }

    public void setUpdatedTime(long time) {
        mUpdatedTime = time;
    }

    public String getUpdatedTimeStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(mUpdatedTime));
    }

    public String getDesc() {
        return mDescription;
    }

    public void setDesc(String desc) {
        mDescription = desc;
    }

    public String getComments() {
        return mComments;
    }

    public void setComments(String com) {
        mComments = com;
    }

    public String getFullsizePicPath() {
        if (mFullsizePicPath == null) {
            return null;
        } //need a db migration to remove this stupid check
        else if (mFullsizePicPath.equals(" ")) {
            return null;
        } else {
            return mFullsizePicPath;
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
        return mFullsizePicPath.substring(mFullsizePicPath.lastIndexOf("/") + 1);
    }

    public void setFullsizePicPath(String path) {
        mFullsizePicPath = path;
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

    public String getImgMetaJSON() {
        return mImgMetaJSON;
    }

    public ImgMeta getImgMeta() {
        if (mImgMetaJSON == null) {
            return null;
        }
        return ImgMeta.fromJSON(mImgMetaJSON);
    }

    public void setImgMeta(String location, String url, int w, int h) {
        if (url == null) {
            mImgMetaJSON = null;
            return;
        }
        mImgMetaJSON = new ImgMeta(location, url, w, h).toJSON();
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        Date date = new Date(mUpdatedTime);
        String dateString = sdf.format(date);
        return "(" + dateString + ") " + mName + " " + mDescription;
    }

    public void clear() {
        // clear the attributes of a wish, called when deleting a wish,
        // so that the attributes in db can be cleared. We will keep the object_id
        // so that server still knows it
        mName = null;
        mDescription = null;
        mStoreName = null;
        mAddress = null;
        mLink = null;

        mFullsizePicPath = null;
        mImgMetaJSON = null;
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
        if (descrptStr != null) {
            message += (descrptStr + "\n");
        }

        String storeName = getStoreName();
        if (storeName != null) {
            message += ("At " + getStoreName() + "\n");
        }

        String address = getAddress();
        if (address != null) {
            if (storeName == null) {
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
            bitmap = ImageManager.getInstance().decodeSampledBitmapFromFile(mFullsizePicPath, width, height, true);
            ByteArrayOutputStream photoStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, photoStream);
            data = photoStream.toByteArray();
        }
        return data;
    }

    public long save() {
        long id = saveToLocal();
        SyncAgent.getInstance().sync();
        return id;
    }

    public long saveToLocal() {
        Log.d(TAG, "saveToLocal");
        ItemDBManager manager = new ItemDBManager();
        if (mId == -1) { // new item
            mId = manager.addItem(
                    mObjectId,
                    mAccess,
                    mStoreName,
                    mName,
                    mDescription,
                    mUpdatedTime,
                    mImgMetaJSON,
                    mFullsizePicPath,
                    mPrice,
                    mAddress,
                    mLatitude,
                    mLongitude,
                    mPriority,
                    mComplete,
                    mLink,
                    mDeleted,
                    mSyncedToServer,
                    mDownloadImg);
        } else { // existing item
            updateDB();
        }
        return mId;
    }

    private void updateDB() {
        ItemDBManager manager = new ItemDBManager();
        manager.updateItem(
                mId,
                mObjectId,
                mAccess,
                mStoreName,
                mName,
                mDescription,
                mUpdatedTime,
                mImgMetaJSON,
                mFullsizePicPath,
                mPrice,
                mAddress,
                mLatitude,
                mLongitude,
                mPriority,
                mComplete,
                mLink,
                mDeleted,
                mSyncedToServer,
                mDownloadImg);
    }

    public static WishItem fromParseObject(final ParseObject wishObject, long item_id) {
        // wishObject.getDouble() will return 0 if the double is null on server,
        // use getNumber so we can check null
        Number priceNumber = wishObject.getNumber(ItemDBManager.KEY_PRICE);
        Number latNumber = wishObject.getNumber(ItemDBManager.KEY_LATITUDE);
        Number lngNumber = wishObject.getNumber(ItemDBManager.KEY_LONGITUDE);

        return new WishItem(
                item_id,
                wishObject.getObjectId(),
                wishObject.getInt(ItemDBManager.KEY_ACCESS),
                wishObject.getString(ItemDBManager.KEY_STORENAME),
                wishObject.getString(ItemDBManager.KEY_NAME),
                wishObject.getString(ItemDBManager.KEY_DESCRIPTION),
                wishObject.getLong(ItemDBManager.KEY_UPDATED_TIME),
                wishObject.getString(ItemDBManager.KEY_IMG_META_JSON),
                null, // _fullsizePhotoPath, will be updated when we save the image
                priceNumber == null ? null : priceNumber.doubleValue(),
                latNumber == null ? null : latNumber.doubleValue(),
                lngNumber == null ? null : lngNumber.doubleValue(),
                wishObject.getString(ItemDBManager.KEY_ADDRESS),
                0, // priority, not used
                wishObject.getInt(ItemDBManager.KEY_COMPLETE),
                wishObject.getString(ItemDBManager.KEY_LINK),
                wishObject.getBoolean(ItemDBManager.KEY_DELETED),
                true,
                false);
    }

    public static void toParseObject(final WishItem item, ParseObject wishObject) {
        final ParseUser user = ParseUser.getCurrentUser();
        if (user != null) {// user here should never be null
            wishObject.put(WishItem.PARSE_KEY_OWNDER_ID, user.getObjectId());
        }

        wishObject.put(ItemDBManager.KEY_ACCESS, item.getAccess());
        wishObject.put(ItemDBManager.KEY_STORENAME, item.getStoreName() == null ? JSONObject.NULL : item.getStoreName());
        wishObject.put(ItemDBManager.KEY_NAME, item.getName() == null ? JSONObject.NULL : item.getName());
        wishObject.put(ItemDBManager.KEY_DESCRIPTION, item.getDesc() == null ? JSONObject.NULL : item.getDesc());
        wishObject.put(ItemDBManager.KEY_UPDATED_TIME, item.getUpdatedTime());
        wishObject.put(ItemDBManager.KEY_IMG_META_JSON, item.getImgMetaJSON() == null ? JSONObject.NULL : item.getImgMetaJSON());
        wishObject.put(ItemDBManager.KEY_PRICE, item.getPrice() == null ? JSONObject.NULL : item.getPrice());
        wishObject.put(ItemDBManager.KEY_LATITUDE, item.getLatitude() == null ? JSONObject.NULL : item.getLatitude());
        wishObject.put(ItemDBManager.KEY_LONGITUDE, item.getLongitude() == null ? JSONObject.NULL : item.getLongitude());
        wishObject.put(ItemDBManager.KEY_ADDRESS, item.getAddress() == null ? JSONObject.NULL : item.getAddress());
        wishObject.put(ItemDBManager.KEY_COMPLETE, item.getComplete());
        wishObject.put(ItemDBManager.KEY_LINK, item.getLink() == null ? JSONObject.NULL : item.getLink());
        wishObject.put(ItemDBManager.KEY_DELETED, item.getDeleted());
        List<String> tags = TagItemDBManager.instance().tags_of_item(item.getId());
        wishObject.put(WishItem.PARSE_KEY_TAGS, tags.isEmpty() ? JSONObject.NULL : tags);
    }

    public ParseObject toParseObject() {
        final ParseObject wishObject = new ParseObject(ItemDBManager.DB_TABLE);
        // Fixme how to save id?
        // Parse Keys must start with a letter, and can contain alphanumeric characters and underscores
        //wishObject.put("id", mId);

        toParseObject(this, wishObject);
        return wishObject;
    }

    public void removeImage() {
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

    /****************** everything below here is for implementing Parcelable *********************/

    // 99.9% of the time you can just ignore this
    public int describeContents() {
        return 0;
    }

    // write object's data to the passed-in Parcel
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeString(mObjectId);
        out.writeInt(mAccess);
        out.writeString(mStoreName);
        out.writeString(mName);
        out.writeString(mComments);
        out.writeString(mDescription);
        out.writeLong(mUpdatedTime);
        out.writeString(mImgMetaJSON);
        out.writeString(mFullsizePicPath);
        out.writeInt(mPriority);
        out.writeInt(mComplete);
        out.writeString(mLink);
        out.writeDouble(mPrice);
        out.writeDouble(mLatitude);
        out.writeDouble(mLongitude);
        out.writeString(mAddress);
        out.writeByte((byte) (mDeleted ? 1 : 0));
        out.writeByte((byte) (mSyncedToServer ? 1 : 0));
        out.writeByte((byte) (mDownloadImg ? 1 : 0));
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
        mId = in.readLong();
        mObjectId = in.readString();
        mAccess = in.readInt();
        mStoreName = in.readString();
        mName = in.readString();
        mComments = in.readString();
        mDescription = in.readString();
        mUpdatedTime = in.readLong();
        mImgMetaJSON = in.readString();
        mFullsizePicPath = in.readString();
        mPriority = in.readInt();
        mComplete = in.readInt();
        mLink = in.readString();
        mPrice = in.readDouble();
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mAddress = in.readString();
        mDeleted = in.readByte() != 0;
        mSyncedToServer = in.readByte() != 0;
        mDownloadImg = in.readByte() != 0;
    }
}
