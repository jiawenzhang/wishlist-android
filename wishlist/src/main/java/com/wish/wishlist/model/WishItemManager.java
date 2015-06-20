package com.wish.wishlist.model;

import java.io.File;

import android.content.Context;

import com.wish.wishlist.db.ItemDBManager;
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

    public WishItem retrieveItemById(long itemId) {
        ItemDBManager mItemDBManager = new ItemDBManager(_ctx);

        ItemsCursor wishItemCursor = mItemDBManager.getItem(itemId);
        if (wishItemCursor.getCount() == 0) {
            return null;
        }

        double latitude = wishItemCursor.getDouble(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_LATITUDE));

        double longitude = wishItemCursor.getDouble(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_LONGITUDE));

        String itemLocation = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_ADDRESS));

        String objectId = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_OBJECT_ID));

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

        WishItem item = new WishItem(_ctx, itemId, objectId, storeName, itemName, itemDesc,
                date, picture_str, fullsize_pic_path, itemPrice, latitude, longitude,
                itemLocation, itemPriority, itemComplete, itemLink);

        return item;
    }

    public void deleteItemById(long itemId) {
        WishItem item = retrieveItemById(itemId);
        String photoPath = item.getFullsizePicPath();
        if (photoPath != null) {
            File file = new File(photoPath);
            file.delete();
        }

        ItemDBManager mItemDBManager = new ItemDBManager(_ctx);
        mItemDBManager.deleteItem(itemId);
    }
}
