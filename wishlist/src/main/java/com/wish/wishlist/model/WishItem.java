package com.wish.wishlist.model;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.wish.wishlist.image.PhotoFileCreater;
import com.wish.wishlist.sync.SyncAgent;
import com.wish.wishlist.util.DoubleUtil;
import com.wish.wishlist.util.StringUtil;
import com.wish.wishlist.wish.ImgMeta;
import com.wish.wishlist.wish.ImgMetaArray;

import android.preference.PreferenceManager;

import org.json.JSONObject;

public class WishItem implements Parcelable {
    private static final String TAG = "WishItem";
    public static final int PUBLIC = 0;
    public static final int PRIVATE = 1;

    private long mId = -1;
    private String mObjectId;
    private String mOwnerId;
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

    public final static String PARSE_KEY_OWNER_ID = "ownerId";
    public final static String PARSE_KEY_LAST_CHANGED_BY = "lastChangedBy";
    public final static String PARSE_KEY_TAGS = "tags";
    public final static String PARSE_KEY_IMAGES = "images";
    public static final String PARSE_KEY_ACCESS = "access";
    public static final String PARSE_KEY_STORE_NAME = "storeName";
    public static final String PARSE_KEY_NAME = "itemName";
    public static final String PARSE_KEY_DESCRIPTION = "description";
    public static final String PARSE_KEY_UPDATED_TIME = "updatedTime"; // ms, migrated from data_time:String
    public static final String PARSE_KEY_IMG_META_JSON = "picture";
    public static final String PARSE_KEY_PRICE = "price";
    public static final String PARSE_KEY_ADDRESS = "location";
    public static final String PARSE_KEY_LATITUDE = "latitude";
    public static final String PARSE_KEY_LONGITUDE = "longitude";
    public static final String PARSE_KEY_COMPLETE = "complete";
    public static final String PARSE_KEY_LINK = "link";
    public static final String PARSE_KEY_DELETED = "deleted";

    public WishItem(
            long itemId,
            String object_id,
            String owner_id,
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
        mOwnerId = owner_id;
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
    public void setId(long id)  {
        mId = id;
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

    public String getOwnerId() {
        return mOwnerId;
    }
    public void setOwnerId(final String owner_id) {
        mOwnerId = owner_id;
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

    public String getFormatPriceString() {
        if (mPrice == null) {
            return null;
        } else {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(2);
            nf.setMinimumFractionDigits(2);
            return nf.format(mPrice);
        }
    }

    public String getPriceString() {
        if (mPrice == null) {
            return null;
        }
        return new DecimalFormat("0.00").format(mPrice);
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
        long diffMSecs = System.currentTimeMillis() - mUpdatedTime;
        long diffHours = diffMSecs / (60 * 60 * 1000);
        long diffMins =  diffMSecs / (60 * 1000);

        String updatedTimeStr;
        if (diffHours >= 24) {
            updatedTimeStr = new SimpleDateFormat("MMM dd, yyyy").format(new Date(mUpdatedTime));
        } else if (diffHours > 1) {
            updatedTimeStr = Long.toString(diffHours) + " hours ago";
        } else if (diffHours == 1) {
            updatedTimeStr = Long.toString(diffHours) + " hour ago";
        } else if (diffMins > 1) {
            updatedTimeStr = Long.toString(diffMins) + " minutes ago";
        } else {
            updatedTimeStr = "1 minute ago";
        }
        return updatedTimeStr;
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

    public void setImgMetaJSON(String imgMetaJSON) {
        mImgMetaJSON = imgMetaJSON;
    }

    public ArrayList<ImgMeta> getImgMetaArray() {
        if (mImgMetaJSON == null) {
            return null;
        }
        return ImgMetaArray.fromJSON(mImgMetaJSON);
    }

    public void setImgMetaArray(ArrayList<ImgMeta> metaArray) {
        if (metaArray == null) {
            mImgMetaJSON = null;
            return;
        }
        mImgMetaJSON = new ImgMetaArray(metaArray).toJSON();
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
        String priceStr = getFormatPriceString();
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
                    mOwnerId,
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
                mOwnerId,
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

    public static WishItem newFromParseObject(final ParseObject wishObject) {
        // wishObject.getDouble() will return 0 if the double is null on server,
        // use getNumber so we can check null
        Number priceNumber = wishObject.getNumber(WishItem.PARSE_KEY_PRICE);
        Number latNumber = wishObject.getNumber(WishItem.PARSE_KEY_LATITUDE);
        Number lngNumber = wishObject.getNumber(WishItem.PARSE_KEY_LONGITUDE);

        return new WishItem(
                -1,
                wishObject.getObjectId(),
                wishObject.getString(WishItem.PARSE_KEY_OWNER_ID),
                wishObject.getInt(WishItem.PARSE_KEY_ACCESS),
                wishObject.getString(WishItem.PARSE_KEY_STORE_NAME),
                wishObject.getString(WishItem.PARSE_KEY_NAME),
                wishObject.getString(WishItem.PARSE_KEY_DESCRIPTION),
                wishObject.getLong(WishItem.PARSE_KEY_UPDATED_TIME),
                wishObject.getString(WishItem.PARSE_KEY_IMG_META_JSON),
                null, // _fullsizePhotoPath, will be updated when we save the image
                priceNumber == null ? null : priceNumber.doubleValue(),
                latNumber == null ? null : latNumber.doubleValue(),
                lngNumber == null ? null : lngNumber.doubleValue(),
                wishObject.getString(WishItem.PARSE_KEY_ADDRESS),
                0, // priority, not used
                wishObject.getInt(WishItem.PARSE_KEY_COMPLETE),
                wishObject.getString(WishItem.PARSE_KEY_LINK),
                wishObject.getBoolean(WishItem.PARSE_KEY_DELETED),
                true,
                false);
    }

    public static void toParseObject(final WishItem item, ParseObject wishObject) {
        final ParseUser user = ParseUser.getCurrentUser();
        if (user != null) {// user here should never be null
            wishObject.put(WishItem.PARSE_KEY_OWNER_ID, user.getObjectId());
        }

        wishObject.put(WishItem.PARSE_KEY_ACCESS, item.getAccess());
        wishObject.put(WishItem.PARSE_KEY_STORE_NAME, item.getStoreName() == null ? JSONObject.NULL : item.getStoreName());
        wishObject.put(WishItem.PARSE_KEY_NAME, item.getName() == null ? JSONObject.NULL : item.getName());
        wishObject.put(WishItem.PARSE_KEY_DESCRIPTION, item.getDesc() == null ? JSONObject.NULL : item.getDesc());
        wishObject.put(WishItem.PARSE_KEY_UPDATED_TIME, item.getUpdatedTime());
        wishObject.put(WishItem.PARSE_KEY_IMG_META_JSON, item.getImgMetaJSON() == null ? JSONObject.NULL : item.getImgMetaJSON());
        wishObject.put(WishItem.PARSE_KEY_PRICE, item.getPrice() == null ? JSONObject.NULL : item.getPrice());
        wishObject.put(WishItem.PARSE_KEY_LATITUDE, item.getLatitude() == null ? JSONObject.NULL : item.getLatitude());
        wishObject.put(WishItem.PARSE_KEY_LONGITUDE, item.getLongitude() == null ? JSONObject.NULL : item.getLongitude());
        wishObject.put(WishItem.PARSE_KEY_ADDRESS, item.getAddress() == null ? JSONObject.NULL : item.getAddress());
        wishObject.put(WishItem.PARSE_KEY_COMPLETE, item.getComplete());
        wishObject.put(WishItem.PARSE_KEY_LINK, item.getLink() == null ? JSONObject.NULL : item.getLink());
        wishObject.put(WishItem.PARSE_KEY_DELETED, item.getDeleted());
        wishObject.put(WishItem.PARSE_KEY_IMAGES, JSONObject.NULL); // will be overwritten if we do have an image to upload
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

    @Override
    public boolean equals(Object item) {
        if (item == null) {
            return false;
        }

        if (item.getClass() != getClass()) {
            return false;
        }

        if (item == this) {
            return true;
        }

        WishItem other = (WishItem) item;

        return (other.getId() == getId() &&
                StringUtil.compare(other.getObjectId(), getObjectId()) &&
                StringUtil.compare(other.getOwnerId(), getOwnerId()) &&
                StringUtil.compare(other.getName(), getName()) &&
                StringUtil.compare(other.getDesc(), getDesc()) &&
                StringUtil.compare(other.getImgMetaJSON(), getImgMetaJSON()) &&
                other.getUpdatedTime() == getUpdatedTime()) &&
                StringUtil.compare(other.getStoreName(), getStoreName()) &&
                StringUtil.compare(other.getAddress(), getAddress()) &&
                other.getAccess() == getAccess() &&
                StringUtil.compare(other.getFullsizePicPath(), getFullsizePicPath()) &&
                DoubleUtil.compare(other.getPrice(), getPrice()) &&
                DoubleUtil.compare(other.getLatitude(), getLatitude()) &&
                DoubleUtil.compare(other.getLongitude(), getLongitude()) &&
                other.getDeleted() == getDeleted() &&
                other.getSyncedToServer() == getSyncedToServer() &&
                other.getDownloadImg() == getDownloadImg() &&
                other.getComplete() == getComplete() &&
                StringUtil.compare(other.getLink(), getLink());
    }

    /****************** everything below here is for implementing Parcelable *********************/

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeString(mObjectId);
        out.writeString(mOwnerId);
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
        mOwnerId = in.readString();
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
