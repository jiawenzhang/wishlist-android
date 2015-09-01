package com.wish.wishlist.model;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;

import com.wish.wishlist.db.ItemDBManager;
import com.wish.wishlist.db.ItemDBManager.ItemsCursor;
import com.wish.wishlist.db.TagItemDBManager;

public class WishItemManager {
    private static WishItemManager instance = null;

    public static WishItemManager getInstance() {
        if (instance == null) {
            instance = new WishItemManager();
        }
        return instance;
    }

    private WishItemManager() {}

    public ArrayList<WishItem> getItemsSinceLastSynced()
    {
        ItemDBManager itemDBManager = new ItemDBManager();

        ArrayList<Long> ids = itemDBManager.getItemsSinceLastSynced();
        ArrayList<WishItem> items = new ArrayList<>();
        // Fixme: optimize this by using one SQL to get all items into on cursor
        for (Long id : ids) {
            ItemsCursor wishItemCursor = itemDBManager.getItem(id);
            if (wishItemCursor.getCount() == 0) {
                continue;
            }
            items.add(itemFromCursor(wishItemCursor));
        }
        return items;
    }

    public WishItem getItemByObjectId(String object_id)
    {
        ItemDBManager itemDBManager = new ItemDBManager();

        ItemsCursor wishItemCursor = itemDBManager.getItemByObjectId(object_id);
        if (wishItemCursor.getCount() == 0) {
            return null;
        }
        return itemFromCursor(wishItemCursor);
    }

    public WishItem getItemById(long itemId)
    {
        ItemDBManager itemDBManager = new ItemDBManager();

        ItemsCursor wishItemCursor = itemDBManager.getItem(itemId);
        if (wishItemCursor.getCount() == 0) {
            return null;
        }

        return itemFromCursor(wishItemCursor);
    }

    private WishItem itemFromCursor(ItemsCursor wishItemCursor)
    {
        long itemId = wishItemCursor.getLong(wishItemCursor.getColumnIndexOrThrow(ItemDBManager.KEY_ID));

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

        long updated_time = wishItemCursor.getLong(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_UPDATED_TIME));

        double itemPrice = wishItemCursor.getDouble(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_PRICE));

        int itemPriority = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_PRIORITY));

        int itemComplete = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_COMPLETE));

        String itemLink = wishItemCursor.getString(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_LINK));

        boolean deleted = wishItemCursor.getInt(wishItemCursor
                .getColumnIndexOrThrow(ItemDBManager.KEY_DELETED)) == 1;

        WishItem item = new WishItem(itemId, objectId, storeName, itemName, itemDesc,
                updated_time, picture_str, fullsize_pic_path, itemPrice, latitude, longitude,
                itemLocation, itemPriority, itemComplete, itemLink, deleted);

        return item;
    }

    public void deleteItemById(long itemId) {
        WishItem item = getItemById(itemId);
        String photoPath = item.getFullsizePicPath();
        if (photoPath != null) {
            File file = new File(photoPath);
            file.delete();
        }

        TagItemDBManager.instance().Remove_tags_by_item(itemId);
        item.setDeleted(true);
        item.setUpdatedTime(System.currentTimeMillis());
        item.save();
    }
}
