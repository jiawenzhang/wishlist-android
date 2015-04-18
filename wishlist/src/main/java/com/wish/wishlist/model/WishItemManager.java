package com.wish.wishlist.model;

import java.util.List;

import android.content.Context;
import android.database.Cursor;

import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.LocationDBManager;
import com.wish.wishlist.db.StoreDBManager;
import com.wish.wishlist.db.ItemDBManager.ItemsCursor;

public class WishItemManager {
    static private Context _ctx;
    private static WishItemManager instance = null;

    public static WishItemManager getInstance(Context ctx) {
        if (instance == null){
            instance = new WishItemManager(ctx);
        }

        return instance;
    }

    private WishItemManager(Context ctx) {
        _ctx = ctx;
    }

    public List<WishItem> retrieve(long itemId) {
        return null;
    }

    public WishItem retrieveItembyId(long itemId) {

        ItemDBManager mItemDBManager;
        StoreDBManager mStoreDBManager;
        LocationDBManager mLocationDBManager;

        mItemDBManager = new ItemDBManager(_ctx);
        mItemDBManager.open();

        // Open the Store table in the database
        mStoreDBManager = new StoreDBManager(_ctx);
        mStoreDBManager.open();

        // Open the Location table in the database
        mLocationDBManager = new LocationDBManager(_ctx);
        mLocationDBManager.open();

        ItemsCursor wishItemCursor;
        Cursor mStoreCursor;

        wishItemCursor = mItemDBManager.getItem(itemId);
        long storeID = wishItemCursor.getLong(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_STORE_ID));

        mStoreCursor = mStoreDBManager.getStore(storeID);
        //String storeName = mStoreDBManager.getStoreName(storeID);

        long locationID = mStoreCursor.getLong(mStoreCursor
                .getColumnIndexOrThrow(StoreDBManager.KEY_LOCATION_ID));
//		String itemLocation = mLocationDBManager.getAddress(locationID);
        String itemLocation = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_ADDRESS));
        double latitude = mLocationDBManager.getLatitude(locationID);
        double longitude =  mLocationDBManager.getLongitude(locationID);

        String storeName = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_STORENAME));

        String picture_str = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_PHOTO_URL));

        String fullsize_pic_path = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_FULLSIZE_PHOTO_PATH));

        String itemName = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_NAME));

        String itemDesc = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_DESCRIPTION));

        String date = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_DATE_TIME));

        double itemPrice = wishItemCursor.getDouble(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_PRICE));

        int itemPriority = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_PRIORITY));

        int itemComplete = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_COMPLETE));

        String itemLink = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_LINK));

        WishItem item = new WishItem(_ctx, itemId, storeID, storeName, itemName, itemDesc,
                date, picture_str, fullsize_pic_path, itemPrice, latitude, longitude,
                itemLocation, itemPriority, itemComplete, itemLink);

        wishItemCursor.close();
        mStoreCursor.close();
        mItemDBManager.close();
        mStoreDBManager.close();
        mLocationDBManager.close();

        return item;

    }

    public List<WishItem> retrieveAll() {
        return null;
    }

    public void deleteItembyId(long itemId) {
        ItemDBManager mItemDBManager = new ItemDBManager(_ctx);
        mItemDBManager.open();
        mItemDBManager.deleteItem(itemId);
        mItemDBManager.close();
    }
}
